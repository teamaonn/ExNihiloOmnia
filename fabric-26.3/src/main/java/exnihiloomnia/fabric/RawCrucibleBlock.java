package exnihiloomnia.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RawCrucibleBlock extends Block {
    public RawCrucibleBlock(Properties p){super(p);}
    protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return CrucibleBlock.SHAPE;}
}
