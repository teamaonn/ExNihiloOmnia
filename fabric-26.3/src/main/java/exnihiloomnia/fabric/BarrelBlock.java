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

public final class BarrelBlock extends Block implements EntityBlock {
    public enum Contents implements StringRepresentable {
        EMPTY, COMPOST, WATER, LAVA, DIRT, CLAY, NETHERRACK, END_STONE,
        OBSIDIAN, STONE, COBBLESTONE, GRASS, MYCELIUM, ICE, SLIME, WITCHWATER, WITCHWATER_GROWING, SUMMON_BLAZE, SUMMON_ENDERMAN, SOUL_SAND, CHORUS;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents", Contents.class);
    public static final IntegerProperty FILL = IntegerProperty.create("fill",0,8);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1,0,1,15,1,15),
            Block.box(1,1,1,15,16,2), Block.box(1,1,14,15,16,15),
            Block.box(1,1,2,2,16,14), Block.box(14,1,2,15,16,14));
    private final boolean wooden;
    private final boolean burnsWithLava;

    public BarrelBlock(boolean wooden, boolean burnsWithLava, Properties properties) {
        super(properties);
        this.wooden=wooden;
        this.burnsWithLava=burnsWithLava;
        registerDefaultState(stateDefinition.any().setValue(CONTENTS,Contents.EMPTY).setValue(FILL,0));
    }
    public boolean wooden() { return wooden; }
    public boolean burnsWithLava() { return burnsWithLava; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(CONTENTS,FILL); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new BarrelEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return !level.isClientSide() && type==ExNihiloOmnia.BARREL_ENTITY
                ? (l,p,s,entity)->BarrelEntity.tick((net.minecraft.server.level.ServerLevel)l,p,s,(BarrelEntity)entity) : null;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        return interact(level,pos,player,hand);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        return interact(level,pos,player,InteractionHand.MAIN_HAND);
    }
    private InteractionResult interact(Level level,BlockPos pos,Player player,InteractionHand hand) {
        if(player.isSpectator()) return InteractionResult.PASS;
        if(!level.isClientSide() && level.getBlockEntity(pos) instanceof BarrelEntity barrel) barrel.interact(player,hand);
        // Consume unsuccessful bucket interactions too, preventing accidental
        // fluid placement in or beside a full/composting barrel.
        return InteractionResult.SUCCESS;
    }
}
