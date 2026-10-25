package exnihiloomnia.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class InfestedLeavesBlock extends LeavesBlock implements EntityBlock {
    public static final IntegerProperty INFESTATION = IntegerProperty.create("infestation", 0, 4);
    private final Block original;

    public InfestedLeavesBlock(Block original, Properties properties) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
        this.original = original;
        registerDefaultState(defaultBlockState().setValue(INFESTATION, 0));
    }

    public Block original() { return original; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(INFESTATION);
    }

    public static boolean canInfest(BlockState state) {
        return state.is(BlockTags.LEAVES) && !state.hasBlockEntity() && !(state.getBlock() instanceof InfestedLeavesBlock);
    }

    public static boolean infest(ServerLevel level, BlockPos pos, int initialTicks, boolean sound) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState source = level.getBlockState(pos);
        if (!canInfest(source)) return false;
        InfestedLeavesBlock block = ExNihiloOmnia.INFESTABLE.getOrDefault(source.getBlock(),
                ExNihiloOmnia.INFESTABLE.get(net.minecraft.world.level.block.Blocks.OAK_LEAVES));
        BlockState target = block.defaultBlockState();
        if (source.hasProperty(DISTANCE)) target = target.setValue(DISTANCE, source.getValue(DISTANCE));
        if (source.hasProperty(PERSISTENT)) target = target.setValue(PERSISTENT, source.getValue(PERSISTENT));
        if (source.hasProperty(WATERLOGGED)) target = target.setValue(WATERLOGGED, source.getValue(WATERLOGGED));
        if (!level.setBlock(pos, target, 3)) return false;
        if (level.getBlockEntity(pos) instanceof InfestedLeavesEntity leaves) leaves.start(initialTicks);
        if (sound) level.playSound(null, pos, SoundEvents.GRASS_HIT, SoundSource.BLOCKS, .5F, 1F);
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InfestedLeavesEntity(pos, state); }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == ExNihiloOmnia.INFESTED_LEAVES_ENTITY
                ? (l, p, s, entity) -> InfestedLeavesEntity.tick((ServerLevel) l, p, s, (InfestedLeavesEntity) entity) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof InfestedLeavesEntity leaves) {
            int percent = leaves.growthTicks() * 100 / InfestedLeavesEntity.MATURE_TICKS;
            player.sendOverlayMessage(Component.literal(percent == 100 ? "Infested leaves: fully grown. Harvest for string."
                    : "Leaf infestation: " + percent + "%"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!decaying(state)) return;
        // Natural decay keeps the original small string chance, not the player
        // harvest table. Player-placed infested leaves are persistent like vanilla.
        float progress = level.getBlockEntity(pos) instanceof InfestedLeavesEntity leaves
                ? leaves.growthTicks() / (float) InfestedLeavesEntity.MATURE_TICKS : 0F;
        if (random.nextFloat() < progress * .06F) Block.popResource(level, pos, new ItemStack(Items.STRING));
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
    }
}
