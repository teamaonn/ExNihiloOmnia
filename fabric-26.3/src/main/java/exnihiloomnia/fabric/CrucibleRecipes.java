package exnihiloomnia.fabric;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Original Omnia defaults, expressed in modern block/item IDs. */
public final class CrucibleRecipes {
    public record Melt(Fluid fluid,int millibuckets) {}
    public static Melt melt(ItemStack stack) {
        if(stack.is(Items.COBBLESTONE)||stack.is(Items.STONE)||stack.is(Items.GRANITE)
                ||stack.is(Items.DIORITE)||stack.is(Items.ANDESITE)||stack.is(Items.POLISHED_GRANITE)
                ||stack.is(Items.POLISHED_DIORITE)||stack.is(Items.POLISHED_ANDESITE)||stack.is(Items.GRAVEL)) return new Melt(Fluids.LAVA,250);
        if(stack.is(Items.NETHERRACK)) return new Melt(Fluids.LAVA,1000);
        if(stack.is(Items.MAGMA_BLOCK)) return new Melt(Fluids.LAVA,2000);
        // The original SNOW block here is the thin snow layer, not SNOW_BLOCK.
        if(stack.is(Items.SNOW)) return new Melt(Fluids.WATER,250);
        if(stack.is(Items.ICE)) return new Melt(Fluids.WATER,1000);
        if(stack.is(Items.PACKED_ICE)) return new Melt(Fluids.WATER,2000);
        return null;
    }
    public static int heat(BlockState state) {
        if(state.is(Blocks.TORCH)||state.is(Blocks.WALL_TORCH)) return 8;
        if(state.is(Blocks.LAVA)) return state.getFluidState().isSource()?16:12;
        if(state.is(Blocks.FURNACE)&&state.getValue(BlockStateProperties.LIT)) return 12;
        if(state.is(Blocks.FIRE)||state.is(Blocks.MAGMA_BLOCK)) return 20;
        // Modern equivalents require their flame to be lit too.
        if((state.is(Blocks.CAMPFIRE)||state.is(Blocks.SOUL_CAMPFIRE))&&state.getValue(BlockStateProperties.LIT)) return 20;
        if(state.is(Blocks.SOUL_FIRE)) return 20;
        if(state.is(Blocks.SOUL_TORCH)||state.is(Blocks.SOUL_WALL_TORCH)) return 8;
        if((state.is(Blocks.BLAST_FURNACE)||state.is(Blocks.SMOKER))&&state.getValue(BlockStateProperties.LIT)) return 12;
        return 0;
    }
    private CrucibleRecipes() {}
}
