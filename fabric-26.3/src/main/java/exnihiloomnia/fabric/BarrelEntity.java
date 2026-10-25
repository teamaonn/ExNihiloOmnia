package exnihiloomnia.fabric;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import static exnihiloomnia.fabric.BarrelBlock.Contents;

public final class BarrelEntity extends BlockEntity implements WorldlyContainer {
    public static final int COMPOST_TICKS=1000;
    public final SingleVariantStorage<FluidVariant> tank=new SingleVariantStorage<>() {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return FluidConstants.BUCKET; }
        @Override protected boolean canInsert(FluidVariant fluid) {
            return fluid.getComponents().isEmpty() && (fluid.isOf(Fluids.WATER)||fluid.isOf(Fluids.LAVA)||fluid.isOf(ExNihiloOmnia.WITCHWATER))
                    && compost==0 && product.isEmpty() && slimeTicks<0 && !magicBusy();
        }
        @Override protected boolean canExtract(FluidVariant fluid) { return product.isEmpty() && compost==0 && slimeTicks<0 && !magicBusy(); }
        @Override protected void onFinalCommit() { changed(); }
    };
    private ItemStack product=ItemStack.EMPTY;
    private ItemStack input=ItemStack.EMPTY;
    private ItemStack containers=ItemStack.EMPTY;
    private int compost;
    private int compostTicks;
    private int compostColor=0xFFFFFF;
    private int hotTicks;
    private int slimeTicks=-1;
    private int witchTicks=-1;
    private int summonTicks=-1;
    private boolean summonBlaze;
    private int syncCooldown;
    private boolean syncPending;

    public BarrelEntity(BlockPos pos,BlockState state) { super(ExNihiloOmnia.BARREL_ENTITY,pos,state); }
    public int compostAmount() { return compost; }
    public int compostTicks() { return compostTicks; }
    public int compostColor() { return compostColor; }
    public int hotTicks() { return hotTicks; }
    public int slimeTicks() { return slimeTicks; }
    public int witchTicks(){return witchTicks;}
    public int summonTicks(){return summonTicks;}
    private boolean magicBusy(){return witchTicks>=0||summonTicks>=0;}
    private boolean canSummon(ItemStack item){
        return fullTank()&&level!=null&&level.getDifficulty()!=Difficulty.PEACEFUL
                &&(fluid()==Fluids.LAVA&&item.is(ExNihiloOmnia.ITEMS.get("doll_blaze"))
                ||fluid()==ExNihiloOmnia.WITCHWATER&&item.is(ExNihiloOmnia.ITEMS.get("doll_ender")));
    }
    public Fluid fluid() { return tank.amount>0 ? tank.variant.getFluid() : Fluids.EMPTY; }
    public boolean fullTank() { return tank.amount==FluidConstants.BUCKET; }

    private static Fluid bucketFluid(ItemStack stack) {
        return BucketSupport.fluid(stack);
    }
    private ItemStack fluidRecipe(ItemStack item) {
        if(!fullTank()) return ItemStack.EMPTY;
        if(fluid()==Fluids.WATER && item.is(ExNihiloOmnia.ITEMS.get("dust"))) return new ItemStack(Items.CLAY);
        if(fluid()==Fluids.LAVA && item.is(Items.REDSTONE)) return new ItemStack(Items.NETHERRACK);
        if(fluid()==Fluids.LAVA && item.is(Items.GLOWSTONE_DUST)) return new ItemStack(Items.END_STONE);
        if(fluid()==ExNihiloOmnia.WITCHWATER && item.is(Items.SAND))return new ItemStack(Items.SOUL_SAND);
        if(fluid()==ExNihiloOmnia.WITCHWATER && item.is(Items.GOLDEN_APPLE))return new ItemStack(Items.CHORUS_FRUIT);
        return ItemStack.EMPTY;
    }
    boolean canAccept(ItemStack item) {
        if(item.isEmpty() || !product.isEmpty() || slimeTicks>=0 || magicBusy()) return false;
        Fluid incoming=bucketFluid(item);
        if(incoming!=Fluids.EMPTY) return compost==0 && tank.amount==0;
        if(BucketSupport.empty(item)) return fullTank() && compost==0;
        if(!fluidRecipe(item).isEmpty() || canSummon(item)) return true;
        if(item.is(ExNihiloOmnia.ITEMS.get("spores")))return fluid()==Fluids.WATER&&fullTank();
        if(BucketSupport.milk(item)) return fluid()==Fluids.WATER && fullTank() && level!=null && level.getDifficulty()!=Difficulty.PEACEFUL;
        return tank.amount==0 && compost<1000 && BarrelCompost.value(item)>0;
    }

    private ItemStack containerAfter(ItemStack stack) {
        if(bucketFluid(stack)!=Fluids.EMPTY || BucketSupport.milk(stack)) return BucketSupport.emptied(stack);
        if(BucketSupport.empty(stack)) return BucketSupport.filled(stack,fluid());
        return ItemStack.EMPTY;
    }

    /** Mutate the barrel only; the caller owns consumption/remainders. */
    private void accept(ItemStack stack) {
        Fluid incoming=bucketFluid(stack);
        ItemStack recipe=fluidRecipe(stack);
        if(incoming!=Fluids.EMPTY) {
            tank.variant=FluidVariant.of(incoming);tank.amount=FluidConstants.BUCKET;hotTicks=0;
        } else if(BucketSupport.empty(stack)) clearTank();
        else if(!recipe.isEmpty()) finish(recipe);
        else if(stack.is(ExNihiloOmnia.ITEMS.get("spores"))){witchTicks=0;}
        else if(canSummon(stack)){summonBlaze=stack.is(ExNihiloOmnia.ITEMS.get("doll_blaze"));summonTicks=0;hotTicks=0;}
        else if(BucketSupport.milk(stack)) { clearTank();slimeTicks=0; }
        else {
            int amount=Math.min(BarrelCompost.value(stack),1000-compost);
            int addedColor=BarrelCompost.color(stack);
            int total=compost+amount;
            int red=(((compostColor>>16)&255)*compost+((addedColor>>16)&255)*amount)/total;
            int green=(((compostColor>>8)&255)*compost+((addedColor>>8)&255)*amount)/total;
            int blue=((compostColor&255)*compost+(addedColor&255)*amount)/total;
            compostColor=red<<16|green<<8|blue;compost=total;
        }
        changed();
    }
    private void clearTank() { tank.variant=FluidVariant.blank();tank.amount=0;hotTicks=0; }
    private void finish(ItemStack stack) {
        product=stack;compost=0;compostTicks=0;slimeTicks=-1;witchTicks=-1;summonTicks=-1;clearTank();changed();
    }

    public void interact(Player player,InteractionHand hand) {
        if(!product.isEmpty()) { give(player,removeItem(0,product.getCount()));return; }
        if(!containers.isEmpty()) { give(player,removeItem(2,containers.getCount()));return; }
        ItemStack held=player.getItemInHand(hand);
        if(slimeTicks>=1000 && held.getItem() instanceof CrookItem && level instanceof ServerLevel server
                && server.getDifficulty()!=Difficulty.PEACEFUL && server.isEmptyBlock(worldPosition.above())) {
            var slime=EntityTypes.SLIME.create(server,EntitySpawnReason.TRIGGERED);
            if(slime!=null) {
                slime.setPos(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5);
                slime.setSize(1,true);slime.setHealth(1F);
                if(server.addFreshEntity(slime)) {
                    held.hurtAndBreak(1,player,hand==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
                    slimeTicks=-1;changed();
                }
            }
            return;
        }
        if(canAccept(held)) {
            ItemStack remainder=containerAfter(held);
            accept(held);
            if(remainder.isEmpty()) held.consume(1,player);
            else player.setItemInHand(hand,ItemUtils.createFilledResult(held,player,remainder));
        }
        player.sendOverlayMessage(Component.literal(status()));
    }
    private static void give(Player player,ItemStack stack) {
        if(!player.getInventory().add(stack)) player.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);
    }
    public String status() {
        if(!product.isEmpty()) return "Barrel: "+product.getHoverName().getString()+" ready.";
        if(witchTicks>=0)return "Witchwater transforming: "+witchTicks/10+"%";
        if(summonTicks>=0)return (summonBlaze?"Blaze":"Enderman")+" summoning: "+summonTicks/10+"%"+(summonTicks>=1000?" — needs clear space above and non-Peaceful difficulty.":"");
        if(slimeTicks>=0) return slimeTicks>=1000?"Slime ready: use a crook to release it.":"Slime growing: "+slimeTicks/10+"%";
        if(compost>0) return compost<1000?"Compost: "+compost/10+"% full.":"Composting: "+compostTicks/10+"%";
        if(tank.amount>0) return "Barrel: "+(fluid()==Fluids.WATER?"water":fluid()==Fluids.LAVA?"lava":"Witchwater")+" "+tank.amount*1000/FluidConstants.BUCKET+"/1000 mB"
                +(hotTicks>0?" — wooden barrel burning: "+hotTicks/4+"%":"");
        return "Barrel empty: add compost material or a fluid bucket.";
    }

    public static void tick(ServerLevel level,BlockPos pos,BlockState state,BarrelEntity barrel) {
        barrel.processInput();
        if(barrel.witchTicks>=0){
            if(++barrel.witchTicks>=1000){barrel.witchTicks=-1;barrel.tank.variant=FluidVariant.of(ExNihiloOmnia.WITCHWATER);barrel.changed();}
            else barrel.setChanged();
        } else if(barrel.summonTicks>=0){
            if(barrel.summonTicks<1000){barrel.summonTicks++;barrel.setChanged();}
            if(barrel.summonTicks%20==0)level.sendParticles(barrel.summonBlaze?ParticleTypes.SMOKE:ParticleTypes.PORTAL,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,3,.3,.1,.3,.02);
            if(barrel.summonTicks>=1000 && level.getDifficulty()!=Difficulty.PEACEFUL){
                net.minecraft.world.entity.Mob mob=barrel.summonBlaze?EntityTypes.BLAZE.create(level,EntitySpawnReason.TRIGGERED):EntityTypes.ENDERMAN.create(level,EntitySpawnReason.TRIGGERED);
                if(mob!=null){
                    mob.setPos(pos.getX()+.5,pos.getY()+1.25,pos.getZ()+.5);
                    if(level.isEmptyBlock(pos.above())&&level.noCollision(mob)&&level.addFreshEntity(mob)){
                        barrel.summonTicks=-1;barrel.clearTank();barrel.changed();
                    }
                }
            }
        } else if(barrel.compost==1000) {
            if(++barrel.compostTicks>=COMPOST_TICKS) barrel.finish(new ItemStack(Items.DIRT));
            else barrel.setChanged();
        } else if(barrel.slimeTicks>=0) {
            if(barrel.slimeTicks<1000){barrel.slimeTicks++;barrel.setChanged();}
        } else if(barrel.product.isEmpty()) {
            if((barrel.tank.amount==0||barrel.fluid()==Fluids.WATER) && barrel.compost==0 && level.isRainingAt(pos.above())) {
                try(var tx=Transaction.openOuter()) { barrel.tank.insert(FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET/1000,tx);tx.commit(); }
            }
            var above=level.getFluidState(pos.above());
            if(barrel.fluid()==Fluids.LAVA && barrel.fullTank() && above.is(FluidTags.WATER)) barrel.finish(new ItemStack(Items.OBSIDIAN));
            else if(barrel.fluid()==Fluids.WATER && above.is(FluidTags.LAVA)) barrel.finish(new ItemStack(above.isSource()?Items.STONE:Items.COBBLESTONE));
            else if(barrel.fluid()==Fluids.WATER && barrel.fullTank()
                    && level.getBiome(pos).value().coldEnoughToSnow(pos,level.getSeaLevel()) && level.getRandom().nextInt(1000)==0) barrel.finish(new ItemStack(Items.ICE));
            if(barrel.fluid()==Fluids.LAVA && ((BarrelBlock)state.getBlock()).burnsWithLava()) {
                barrel.hotTicks=Math.min(400,barrel.hotTicks+1);barrel.setChanged();
                if(barrel.hotTicks<400 && barrel.hotTicks%20==0) level.sendParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,3,.25,.05,.25,.01);
                if(barrel.hotTicks>=400) {
                    if(barrel.fullTank()) {
                        barrel.product=ItemStack.EMPTY;
                        level.setBlock(pos,Blocks.LAVA.defaultBlockState(),3);
                        return;
                    } else if(level.isEmptyBlock(pos.above())) level.setBlock(pos.above(),Blocks.FIRE.defaultBlockState(),3);
                }
            } else if(barrel.hotTicks!=0){barrel.hotTicks=0;barrel.setChanged();}
            barrel.spreadMoss(level,pos,state);
            if(barrel.fluid()==Fluids.WATER&&barrel.fullTank()&&((BarrelBlock)state.getBlock()).wooden()
                    &&level.getRandom().nextInt(1000)==0&&hasMycelium(level,pos)){
                barrel.witchTicks=0;barrel.changed();
            }
        }
        if(barrel.product.is(Items.DIRT) && level.getRandom().nextInt(1000)==0 && level.getMaxLocalRawBrightness(pos.above())>=9) {
            BlockPos neighbour=pos.offset(level.getRandom().nextInt(3)-1,level.getRandom().nextInt(3)-1,level.getRandom().nextInt(3)-1);
            if(level.hasChunkAt(neighbour)) {
                if(level.getBlockState(neighbour).is(Blocks.GRASS_BLOCK)) barrel.finish(new ItemStack(Items.GRASS_BLOCK));
                else if(level.getBlockState(neighbour).is(Blocks.MYCELIUM)) barrel.finish(new ItemStack(Items.MYCELIUM));
            }
        }
        barrel.updateVisual();
        if(barrel.syncCooldown>0)barrel.syncCooldown--;
        if(barrel.syncCooldown==0 && barrel.syncPending){barrel.syncPending=false;barrel.syncNow();}
    }

    private static boolean hasMycelium(ServerLevel level,BlockPos pos){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var below=pos.offset(x,-1,z);
            if(level.hasChunkAt(below)&&level.getBlockState(below).is(Blocks.MYCELIUM))return true;
        }
        return false;
    }

    private void spreadMoss(ServerLevel level,BlockPos pos,BlockState state) {
        if(fluid()!=Fluids.WATER || !((BarrelBlock)state.getBlock()).wooden() || level.getMaxLocalRawBrightness(pos)>=5) return;
        int chance=(int)Math.max(1,2_000_000L/Math.max(1,tank.amount*1000/FluidConstants.BUCKET));
        if(level.getRandom().nextInt(chance)!=0) return;
        BlockPos below=pos.offset(level.getRandom().nextInt(3)-1,-1,level.getRandom().nextInt(3)-1);
        for(int i=0;i<4 && level.hasChunkAt(below);i++,below=below.below()) {
            if(level.getBlockState(below).is(Blocks.COBBLESTONE)){level.setBlock(below,Blocks.MOSSY_COBBLESTONE.defaultBlockState(),3);return;}
            if(!level.getBlockState(below).is(Blocks.MOSSY_COBBLESTONE)) return;
        }
    }

    private void processInput() {
        if(!canAccept(input)) return;
        ItemStack remainder=containerAfter(input);
        if(!remainder.isEmpty() && !containers.isEmpty() && (!ItemStack.isSameItemSameComponents(containers,remainder)
                || containers.getCount()+remainder.getCount()>containers.getMaxStackSize())) return;
        accept(input);input.shrink(1);
        if(input.isEmpty()) input=ItemStack.EMPTY;
        if(!remainder.isEmpty()){if(containers.isEmpty())containers=remainder;else containers.grow(remainder.getCount());}
        changed();
    }

    public void changed() {
        if(fluid()!=Fluids.LAVA)hotTicks=0;
        setChanged();updateVisual();
        if(level instanceof ServerLevel){if(syncCooldown==0)syncNow();else syncPending=true;}
    }
    private void syncNow(){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);syncCooldown=8;}
    private void updateVisual() {
        if(!(level instanceof ServerLevel) || !(getBlockState().getBlock() instanceof BarrelBlock)) return;
        Contents content=Contents.EMPTY;int fill=0;
        if(!product.isEmpty()) {content=outputContent();fill=8;}
        else if(summonTicks>=0){content=summonBlaze?Contents.SUMMON_BLAZE:Contents.SUMMON_ENDERMAN;fill=8;}
        else if(witchTicks>=0){content=Contents.WITCHWATER_GROWING;fill=8;}
        else if(slimeTicks>=0){content=Contents.SLIME;fill=8;}
        else if(compost>0){content=Contents.COMPOST;fill=(compost+124)/125;}
        else if(tank.amount>0){content=fluid()==Fluids.LAVA?Contents.LAVA:fluid()==ExNihiloOmnia.WITCHWATER?Contents.WITCHWATER:Contents.WATER;fill=(int)((tank.amount*8+FluidConstants.BUCKET-1)/FluidConstants.BUCKET);}
        BlockState current=getBlockState();
        if(current.getValue(BarrelBlock.CONTENTS)!=content || current.getValue(BarrelBlock.FILL)!=fill)
            level.setBlock(worldPosition,current.setValue(BarrelBlock.CONTENTS,content).setValue(BarrelBlock.FILL,fill),3);
    }
    private Contents outputContent() {
        if(product.is(Items.SOUL_SAND))return Contents.SOUL_SAND;
        if(product.is(Items.CHORUS_FRUIT))return Contents.CHORUS;
        if(product.is(Items.CLAY))return Contents.CLAY;
        if(product.is(Items.NETHERRACK))return Contents.NETHERRACK;
        if(product.is(Items.END_STONE))return Contents.END_STONE;
        if(product.is(Items.OBSIDIAN))return Contents.OBSIDIAN;
        if(product.is(Items.STONE))return Contents.STONE;
        if(product.is(Items.COBBLESTONE))return Contents.COBBLESTONE;
        if(product.is(Items.GRASS_BLOCK))return Contents.GRASS;
        if(product.is(Items.MYCELIUM))return Contents.MYCELIUM;
        if(product.is(Items.ICE))return Contents.ICE;
        return Contents.DIRT;
    }

    @Override public int getContainerSize(){return 3;}
    @Override public boolean isEmpty(){return product.isEmpty()&&input.isEmpty()&&containers.isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?product:slot==1?input:slot==2?containers:ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){ItemStack result=getItem(slot).split(count);changed();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){ItemStack result=getItem(slot);setItem(slot,ItemStack.EMPTY);return result;}
    @Override public void setItem(int slot,ItemStack stack){if(slot==0)product=stack;else if(slot==1)input=stack;else if(slot==2)containers=stack;changed();}
    @Override public void clearContent(){product=ItemStack.EMPTY;input=ItemStack.EMPTY;containers=ItemStack.EMPTY;changed();}
    @Override public int getMaxStackSize(ItemStack stack){return stack.getMaxStackSize();}
    @Override public boolean stillValid(Player player){return net.minecraft.world.Container.stillValidBlockEntity(this,player);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==1 && canAccept(stack);}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[]{0,2}:new int[]{1};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return side!=Direction.DOWN && canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return side==Direction.DOWN && (slot==0||slot==2);}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level instanceof ServerLevel){for(int slot=0;slot<3;slot++)if(!getItem(slot).isEmpty())Block.popResource(level,worldPosition,getItem(slot).copy());}
        product=ItemStack.EMPTY;input=ItemStack.EMPTY;containers=ItemStack.EMPTY;
        super.preRemoveSideEffects(pos,state);
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);SingleVariantStorage.writeValue(tank,FluidVariant.CODEC,out.child("tank"));
        out.putInt("compost",compost);out.putInt("compost_ticks",compostTicks);out.putInt("compost_color",compostColor);
        out.putInt("hot_ticks",hotTicks);out.putInt("slime_ticks",slimeTicks);
        out.putInt("witch_ticks",witchTicks);out.putInt("summon_ticks",summonTicks);out.putBoolean("summon_blaze",summonBlaze);
        if(!product.isEmpty())out.store("product",ItemStack.CODEC,product);
        if(!input.isEmpty())out.store("input",ItemStack.CODEC,input);
        if(!containers.isEmpty())out.store("containers",ItemStack.CODEC,containers);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);SingleVariantStorage.readValue(tank,FluidVariant.CODEC,FluidVariant::blank,in.childOrEmpty("tank"));
        if(!tank.variant.getComponents().isEmpty() || fluid()!=Fluids.WATER && fluid()!=Fluids.LAVA && fluid()!=ExNihiloOmnia.WITCHWATER)clearTank();
        tank.amount=Math.clamp(tank.amount,0,FluidConstants.BUCKET);
        compost=Math.clamp(in.getIntOr("compost",0),0,1000);compostTicks=Math.clamp(in.getIntOr("compost_ticks",0),0,COMPOST_TICKS);
        compostColor=in.getIntOr("compost_color",0xFFFFFF);hotTicks=Math.clamp(in.getIntOr("hot_ticks",0),0,400);
        slimeTicks=Math.clamp(in.getIntOr("slime_ticks",-1),-1,1000);
        witchTicks=Math.clamp(in.getIntOr("witch_ticks",-1),-1,1000);
        summonTicks=Math.clamp(in.getIntOr("summon_ticks",-1),-1,1000);summonBlaze=in.getBooleanOr("summon_blaze",false);
        if(!fullTank()||fluid()!=Fluids.WATER)witchTicks=-1;
        if(!fullTank()||(summonBlaze?fluid()!=Fluids.LAVA:fluid()!=ExNihiloOmnia.WITCHWATER))summonTicks=-1;
        if(witchTicks>=0)summonTicks=-1;
        product=in.read("product",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        input=in.read("input",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        containers=in.read("containers",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if(!product.isEmpty()){compost=0;compostTicks=0;slimeTicks=-1;witchTicks=-1;summonTicks=-1;clearTank();}
        else if(compost>0||slimeTicks>=0){witchTicks=-1;summonTicks=-1;clearTank();}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
