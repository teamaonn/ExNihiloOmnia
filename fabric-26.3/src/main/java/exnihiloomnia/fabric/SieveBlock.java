package exnihiloomnia.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** All machine state lives in the block state, so saves, chunk unloads and
 * server restarts preserve both the installed mesh and the consumed input. */
public final class SieveBlock extends Block {
    public static final IntegerProperty MESH = IntegerProperty.create("mesh", 0, 4);
    public static final IntegerProperty CONTENT = IntegerProperty.create("content", 0, 7);
    public static final IntegerProperty PROGRESS = IntegerProperty.create("progress", 0, 7);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 11, 0, 16, 14, 16), Block.box(0, 0, 0, 2, 11, 2),
            Block.box(14, 0, 0, 16, 11, 2), Block.box(0, 0, 14, 2, 11, 16),
            Block.box(14, 0, 14, 16, 11, 16));
    private final boolean heavy;

    public SieveBlock(boolean heavy, Properties properties) {
        super(properties);
        this.heavy = heavy;
        registerDefaultState(stateDefinition.any().setValue(MESH, 0).setValue(CONTENT, 0).setValue(PROGRESS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MESH, CONTENT, PROGRESS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(stack, state, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(ItemStack.EMPTY, state, level, pos, player);
    }

    private InteractionResult interact(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (player.isSpectator()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int mesh = state.getValue(MESH), content = state.getValue(CONTENT);
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (content != 0) {
                give(player, new ItemStack(ExNihiloOmnia.materialItem(content, heavy)));
                level.setBlock(pos, state.setValue(CONTENT, 0).setValue(PROGRESS, 0), 3);
                status(player, "Input returned.");
            } else if (mesh != 0) {
                give(player, new ItemStack(ExNihiloOmnia.ITEMS.get(ExNihiloOmnia.MESHES[mesh - 1])));
                level.setBlock(pos, state.setValue(MESH, 0), 3);
                status(player, "Mesh removed.");
            }
            return InteractionResult.SUCCESS;
        }
        if (mesh == 0) {
            for (int i = 0; i < ExNihiloOmnia.MESHES.length; i++) {
                if (stack.is(ExNihiloOmnia.ITEMS.get(ExNihiloOmnia.MESHES[i]))) {
                    level.setBlock(pos, state.setValue(MESH, i + 1), 3);
                    stack.consume(1, player);
                    status(player, "Mesh installed. Add " + (heavy ? "compressed material." : "material."));
                    return InteractionResult.SUCCESS;
                }
            }
            status(player, "Install a mesh first.");
            return InteractionResult.SUCCESS;
        }
        if (content == 0) {
            for (int i = 1; i <= ExNihiloOmnia.MATERIALS.length; i++) {
                if (stack.is(ExNihiloOmnia.materialItem(i, heavy))) {
                    level.setBlock(pos, state.setValue(CONTENT, i).setValue(PROGRESS, 0), 3);
                    stack.consume(1, player);
                    status(player, "Right-click with an empty hand to sieve.");
                    return InteractionResult.SUCCESS;
                }
            }
            status(player, heavy ? "Add a compressed sieve material." : "Add dirt, gravel, sand, dust, soul sand, Nether gravel or End gravel.");
            return InteractionResult.SUCCESS;
        }
        if (!stack.isEmpty()) {
            status(player, "Use an empty hand to sieve; sneak-click to return the input.");
            return InteractionResult.SUCCESS;
        }
        int next = state.getValue(PROGRESS) + 1;
        if (level instanceof ServerLevel serverLevel) {
            BlockState particleState = ((net.minecraft.world.item.BlockItem)
                    ExNihiloOmnia.materialItem(content, false)).getBlock().defaultBlockState();
            // Spawn beneath the mesh so the falling fragments are visible through
            // the open frame, including to other players watching the sieve.
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, particleState),
                    pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5,
                    heavy ? 24 : 12, 0.22, 0.04, 0.22, 0.025);
        }
        if (next >= 8) {
            SieveDrops.roll(content, mesh, heavy ? 9 : 1, level.getRandom(),
                    reward -> Block.popResource(level, pos.above(), reward));
            level.setBlock(pos, state.setValue(CONTENT, 0).setValue(PROGRESS, 0), 3);
            status(player, heavy ? "Sieving complete: nine rolls." : "Sieving complete.");
        } else {
            level.setBlock(pos, state.setValue(PROGRESS, next), 3);
            status(player, "Sieving: " + next + "/8");
        }
        return InteractionResult.SUCCESS;
    }

    private static void status(Player player, String text) { player.sendOverlayMessage(Component.literal(text)); }
    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }
}
