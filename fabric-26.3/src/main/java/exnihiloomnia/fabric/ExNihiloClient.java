package exnihiloomnia.fabric;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public final class ExNihiloClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(
                ExNihiloOmnia.WITCHWATER,ExNihiloOmnia.FLOWING_WITCHWATER,
                new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                    new net.minecraft.client.resources.model.sprite.Material(ExNihiloOmnia.id("block/witchwater_still"),true),
                    new net.minecraft.client.resources.model.sprite.Material(ExNihiloOmnia.id("block/witchwater_flowing"),true),null,null));
        for (InfestedLeavesBlock block : ExNihiloOmnia.INFESTABLE.values()) {
            BlockColorRegistry.register((state, view, pos, output) -> {
                var original = block.original().defaultBlockState();
                int color = originalTint(original, view, pos, Minecraft.getInstance().getBlockColors());
                float progress = state.getValue(InfestedLeavesBlock.INFESTATION) / 4F;
                output.add(blendTint(color, progress));
            }, block);
        }
        BlockColorRegistry.register((state,view,pos,output)->output.add(0xFF000000|
                (view!=null&&pos!=null?net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(view,pos):0x3F76E4)),ExNihiloOmnia.BLOCKS.get("crucible"));
        for(BarrelBlock block:ExNihiloOmnia.BARRELS.values()) {
            BlockColorRegistry.register((state,view,pos,output)->{
                int color=0xFFFFFF;
                if(state.getValue(BarrelBlock.CONTENTS)==BarrelBlock.Contents.WATER)
                    color=view!=null&&pos!=null?net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(view,pos):0x3F76E4;
                else if(state.getValue(BarrelBlock.CONTENTS)==BarrelBlock.Contents.GRASS)
                    color=view!=null&&pos!=null?net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(view,pos):0x79C05A;
                else if(state.getValue(BarrelBlock.CONTENTS)==BarrelBlock.Contents.COMPOST && view!=null && pos!=null
                        && view.getBlockEntity(pos) instanceof BarrelEntity barrel) color=barrel.compostColor();
                output.add(0xFF000000|color);
            },block);
        }
    }

    static int originalTint(BlockState original, BlockAndTintGetter view, BlockPos pos, BlockColors vanillaColors) {
        var factory = BlockColorRegistry.getFactory(original);
        if (factory != null) {
            var colors = new IntArrayList();
            factory.collect(original, view, pos, colors);
            if (!colors.isEmpty()) return colors.getInt(0);
        }
        // getFactory only queries Fabric's dynamic factories. Vanilla foliage
        // uses the static BlockColors registry, including biome and species tints.
        var source = vanillaColors.getTintSource(original, 0);
        return view == null || pos == null ? source.color(original) : source.colorInWorld(original, view, pos);
    }

    static int blendTint(int color, float progress) {
        return 0xFF000000 | blend((color >> 16) & 255, progress) << 16
                | blend((color >> 8) & 255, progress) << 8 | blend(color & 255, progress);
    }

    private static int blend(int color, float progress) { return Math.round(color + (255 - color) * progress); }
}
