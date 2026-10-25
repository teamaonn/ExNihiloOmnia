package exnihiloomnia.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class InfestedLeavesEntity extends BlockEntity {
    public static final int MATURE_TICKS = 2000;
    private int growthTicks;
    private int spreadTicks;

    public InfestedLeavesEntity(BlockPos pos, BlockState state) {
        super(ExNihiloOmnia.INFESTED_LEAVES_ENTITY, pos, state);
        growthTicks = state.getValue(InfestedLeavesBlock.INFESTATION) * 500;
    }

    public int growthTicks() { return growthTicks; }

    public void start(int ticks) {
        growthTicks = Math.clamp(ticks, 0, MATURE_TICKS);
        spreadTicks = 0;
        setChanged();
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, InfestedLeavesEntity leaves) {
        if (leaves.growthTicks < MATURE_TICKS) leaves.growthTicks++;
        int stage = leaves.growthTicks / 500;
        if (state.getValue(InfestedLeavesBlock.INFESTATION) != stage) {
            level.setBlock(pos, state.setValue(InfestedLeavesBlock.INFESTATION, stage), 3);
        }
        // Match Omnia's >60% threshold and one random neighbour attempt every
        // roughly five seconds. Never generate or load an unknown chunk.
        if (leaves.growthTicks > 1200 && ++leaves.spreadTicks >= 100) {
            var random = level.getRandom();
            BlockPos next = pos.offset(random.nextInt(3)-1, random.nextInt(3)-1, random.nextInt(3)-1);
            InfestedLeavesBlock.infest(level, next, random.nextInt(15) * 20, false);
            leaves.spreadTicks = random.nextInt(10);
        }
        leaves.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("growth_ticks", growthTicks);
        output.putInt("spread_ticks", spreadTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        growthTicks = Math.clamp(input.getIntOr("growth_ticks", 0), 0, MATURE_TICKS);
        spreadTicks = Math.clamp(input.getIntOr("spread_ticks", 0), 0, 99);
    }
}
