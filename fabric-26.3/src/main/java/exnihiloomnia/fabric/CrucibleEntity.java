package exnihiloomnia.fabric;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import static exnihiloomnia.fabric.CrucibleBlock.Content;

public final class CrucibleEntity extends BlockEntity implements WorldlyContainer {
    public static final long CAPACITY=8*FluidConstants.BUCKET;
    public static final int ITEM_WORK=50_000,MAX_WORK=200_000;
    private ItemStack material=ItemStack.EMPTY,input=ItemStack.EMPTY;
    private int work,processed,syncCooldown;
    private boolean syncPending;
    public final SingleVariantStorage<FluidVariant> tank=new SingleVariantStorage<>() {
        protected FluidVariant getBlankVariant(){return FluidVariant.blank();}
        protected long getCapacity(FluidVariant f){return CAPACITY;}
        protected boolean canInsert(FluidVariant f){return false;}
        protected void onFinalCommit(){changed();}
    };
    public CrucibleEntity(BlockPos p,BlockState s){super(ExNihiloOmnia.CRUCIBLE_ENTITY,p,s);}
    public int work(){return work;}
    public int processed(){return processed;}
    public boolean canAccept(ItemStack stack){
        var recipe=CrucibleRecipes.melt(stack);
        return recipe!=null&&work+processed+ITEM_WORK<=MAX_WORK
                &&(material.isEmpty()||material.is(stack.getItem()))
                &&(tank.amount==0||tank.variant.isOf(recipe.fluid()));
    }
    public boolean addSolid(ItemStack stack){
        if(!canAccept(stack))return false;
        if(material.isEmpty())material=stack.copyWithCount(1);
        work+=ITEM_WORK;changed();return true;
    }
    public void interact(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);
        if(BucketSupport.empty(held)&&tank.amount>=FluidConstants.BUCKET){
            var full=BucketSupport.filled(held,tank.variant.getFluid());
            if(!full.isEmpty())try(var tx=Transaction.openOuter()){
                if(tank.extract(tank.variant,FluidConstants.BUCKET,tx)==FluidConstants.BUCKET){
                    tx.commit();player.setItemInHand(hand,ItemUtils.createFilledResult(held,player,full));
                }
            }
        }else if(addSolid(held))held.consume(1,player);
        player.sendOverlayMessage(Component.literal(status()));
    }
    public String status(){
        int heat=level==null?0:CrucibleRecipes.heat(level.getBlockState(worldPosition.below()));
        return "Crucible: "+(tank.amount*1000/FluidConstants.BUCKET)+"/8000 mB"
                +", solids "+((work+processed)*100/MAX_WORK)+"% — "+(heat==0?"no heat source below.":"heat "+heat+".");
    }
    public static void tick(ServerLevel level,BlockPos pos,BlockState state,CrucibleEntity e){
        if(!e.input.isEmpty()&&e.addSolid(e.input)){e.input.shrink(1);e.setChanged();}
        var recipe=CrucibleRecipes.melt(e.material);
        if(recipe!=null&&e.tank.amount<CAPACITY){
            int heat=CrucibleRecipes.heat(level.getBlockState(pos.below()));
            int moved=Math.min(e.work,heat*2);
            if(moved>0){e.work-=moved;e.processed+=moved;e.setChanged();}
            // 200 work units = one original solid mB. Retain fractional work and
            // pause at a full tank, so neither clipping nor a final tick loses fluid.
            long perUnit=recipe.millibuckets()*FluidConstants.BUCKET/(250*1000);
            while(e.processed>=200&&CAPACITY-e.tank.amount>=perUnit){
                e.processed-=200;e.tank.variant=FluidVariant.of(recipe.fluid());e.tank.amount+=perUnit;e.setChanged();
            }
            if(e.work==0&&e.processed==0)e.material=ItemStack.EMPTY;
        }
        e.updateVisual();
        if(e.syncCooldown>0)e.syncCooldown--;
        if(e.syncCooldown==0&&e.syncPending){e.syncPending=false;e.syncNow();}
    }
    private void updateVisual(){
        if(!(level instanceof ServerLevel)||!(getBlockState().getBlock() instanceof CrucibleBlock))return;
        int solid=(work+processed+24_999)/25_000;
        int fluid=(int)((tank.amount*8+CAPACITY-1)/CAPACITY);
        Content content=Content.EMPTY;
        int fill=Math.max(solid,fluid);
        if(fluid>=solid&&fluid>0)content=tank.variant.isOf(Fluids.LAVA)?Content.LAVA:Content.WATER;
        else if(solid>0){
            var item=material.getItem();
            if(item==Items.COBBLESTONE)content=Content.COBBLESTONE;
            else if(item==Items.STONE)content=Content.STONE;
            else if(item==Items.GRANITE)content=Content.GRANITE;
            else if(item==Items.DIORITE)content=Content.DIORITE;
            else if(item==Items.ANDESITE)content=Content.ANDESITE;
            else if(item==Items.POLISHED_GRANITE)content=Content.POLISHED_GRANITE;
            else if(item==Items.POLISHED_DIORITE)content=Content.POLISHED_DIORITE;
            else if(item==Items.POLISHED_ANDESITE)content=Content.POLISHED_ANDESITE;
            else if(item==Items.GRAVEL)content=Content.GRAVEL;
            else if(item==Items.NETHERRACK)content=Content.NETHERRACK;
            else if(item==Items.MAGMA_BLOCK)content=Content.MAGMA;
            else if(item==Items.SNOW)content=Content.SNOW;
            else if(item==Items.ICE)content=Content.ICE;
            else content=Content.PACKED_ICE;
        }
        var current=getBlockState();
        if(current.getValue(CrucibleBlock.CONTENT)!=content||current.getValue(CrucibleBlock.FILL)!=fill)
            level.setBlock(worldPosition,current.setValue(CrucibleBlock.CONTENT,content).setValue(CrucibleBlock.FILL,fill),3);
    }
    public void changed(){setChanged();updateVisual();if(level instanceof ServerLevel){if(syncCooldown==0)syncNow();else syncPending=true;}}
    private void syncNow(){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);syncCooldown=8;}
    public int getContainerSize(){return 1;}
    public boolean isEmpty(){return input.isEmpty();}
    public ItemStack getItem(int slot){return slot==0?input:ItemStack.EMPTY;}
    public ItemStack removeItem(int slot,int count){var result=getItem(slot).split(count);changed();return result;}
    public ItemStack removeItemNoUpdate(int slot){var result=getItem(slot);input=ItemStack.EMPTY;return result;}
    public void setItem(int slot,ItemStack stack){if(slot==0)input=stack;changed();}
    public void clearContent(){input=ItemStack.EMPTY;changed();}
    public boolean stillValid(Player p){return net.minecraft.world.Container.stillValidBlockEntity(this,p);}
    public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&canAccept(stack);}
    public int[] getSlotsForFace(Direction side){return side==Direction.UP?new int[]{0}:new int[0];}
    public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return side==Direction.UP&&canPlaceItem(slot,stack);}
    public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level instanceof ServerLevel&&!input.isEmpty())Block.popResource(level,worldPosition,input.copy());
        input=ItemStack.EMPTY;super.preRemoveSideEffects(pos,state);
    }
    protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);SingleVariantStorage.writeValue(tank,FluidVariant.CODEC,out.child("tank"));
        out.putInt("work",work);out.putInt("processed",processed);
        if(!material.isEmpty())out.store("material",ItemStack.CODEC,material);
        if(!input.isEmpty())out.store("input",ItemStack.CODEC,input);
    }
    protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);SingleVariantStorage.readValue(tank,FluidVariant.CODEC,FluidVariant::blank,in.childOrEmpty("tank"));
        tank.amount=Math.clamp(tank.amount,0,CAPACITY);
        if(!tank.variant.getComponents().isEmpty()||(!tank.variant.isOf(Fluids.WATER)&&!tank.variant.isOf(Fluids.LAVA))){tank.amount=0;tank.variant=FluidVariant.blank();}
        material=in.read("material",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        input=in.read("input",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        work=Math.clamp(in.getIntOr("work",0),0,MAX_WORK);
        processed=Math.clamp(in.getIntOr("processed",0),0,MAX_WORK-work);
        var recipe=CrucibleRecipes.melt(material);
        if(recipe==null||(tank.amount>0&&!tank.variant.isOf(recipe.fluid()))){work=0;processed=0;material=ItemStack.EMPTY;}
    }
    public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
