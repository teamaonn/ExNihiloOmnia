package exnihiloomnia.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.server.level.ServerLevel;

/** Water physics, original Witchwater artwork/effects, finite source fluid. */
public abstract class WitchwaterFluid extends WaterFluid {
    public Fluid getFlowing(){return ExNihiloOmnia.FLOWING_WITCHWATER;}
    public Fluid getSource(){return ExNihiloOmnia.WITCHWATER;}
    public Item getBucket(){return ExNihiloOmnia.ITEMS.get("bucket_witchwater");}
    protected boolean canConvertToSource(ServerLevel level){return false;}
    public boolean isSame(Fluid other){return other==getSource()||other==getFlowing();}
    public BlockState createLegacyBlock(FluidState state){return ExNihiloOmnia.WITCHWATER_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL,getLegacyLevel(state));}
    protected void entityInside(Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier effects){
        super.entityInside(level,pos,entity,effects);
        if(level instanceof ServerLevel server)WitchwaterEffects.touch(server,entity);
    }
    public static final class Source extends WitchwaterFluid {
        public int getAmount(FluidState state){return 8;}
        public boolean isSource(FluidState state){return true;}
    }
    public static final class Flowing extends WitchwaterFluid {
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState> builder){super.createFluidStateDefinition(builder);builder.add(LEVEL);}
        public int getAmount(FluidState state){return state.getValue(LEVEL);}
        public boolean isSource(FluidState state){return false;}
    }
}
