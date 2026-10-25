package exnihiloomnia.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CrucibleBlock extends Block implements EntityBlock {
    public enum Content implements StringRepresentable {
        EMPTY,COBBLESTONE,STONE,GRANITE,DIORITE,ANDESITE,POLISHED_GRANITE,POLISHED_DIORITE,POLISHED_ANDESITE,GRAVEL,NETHERRACK,MAGMA,SNOW,ICE,PACKED_ICE,WATER,LAVA;
        public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final EnumProperty<Content> CONTENT=EnumProperty.create("content",Content.class);
    public static final IntegerProperty FILL=IntegerProperty.create("fill",0,8);
    static final VoxelShape SHAPE=Shapes.or(Block.box(0,2,0,16,3,16),Block.box(0,3,0,1,16,16),
            Block.box(15,3,0,16,16,16),Block.box(1,3,0,15,16,1),Block.box(1,3,15,15,16,16),
            Block.box(0,0,0,1,2,1),Block.box(15,0,0,16,2,1),Block.box(0,0,15,1,2,16),Block.box(15,0,15,16,2,16));
    public CrucibleBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(CONTENT,Content.EMPTY).setValue(FILL,0));}
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CONTENT,FILL);}
    protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CrucibleEntity(p,s);}
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
        return !l.isClientSide()&&t==ExNihiloOmnia.CRUCIBLE_ENTITY?(w,p,b,e)->CrucibleEntity.tick((net.minecraft.server.level.ServerLevel)w,p,b,(CrucibleEntity)e):null;
    }
    protected InteractionResult useItemOn(ItemStack i,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return interact(l,p,player,hand);}
    protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return interact(l,p,player,InteractionHand.MAIN_HAND);}
    private InteractionResult interact(Level l,BlockPos p,Player player,InteractionHand hand){
        if(player.isSpectator())return InteractionResult.PASS;
        if(!l.isClientSide()&&l.getBlockEntity(p) instanceof CrucibleEntity e)e.interact(player,hand);
        return InteractionResult.SUCCESS;
    }
}
