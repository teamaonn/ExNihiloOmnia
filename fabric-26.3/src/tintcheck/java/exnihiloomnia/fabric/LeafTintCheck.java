package exnihiloomnia.fabric;

import net.minecraft.SharedConstants;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;

/** Exercises the real vanilla client color registry without opening a window. */
public final class LeafTintCheck {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BlockColors colors = BlockColors.createDefault();
        for (String species : new String[]{"oak", "birch", "spruce", "jungle", "acacia", "dark_oak",
                "mangrove", "cherry", "pale_oak", "azalea", "flowering_azalea", "red_poplar", "orange_poplar", "yellow_poplar"}) {
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                    net.minecraft.resources.Identifier.withDefaultNamespace(species + "_leaves"));
            if (block == Blocks.AIR) throw new IllegalStateException("Missing leaf species: " + species);
            var state = block.defaultBlockState();
            var source = colors.getTintSource(state,0);
            int expected = source == null ? 0xFFFFFFFF : source.color(state);
            int actual = ExNihiloClient.originalTint(state,null,null,colors);
            if ((actual & 0xFFFFFF) != (expected & 0xFFFFFF))
                throw new IllegalStateException("Starting tint must retain vanilla color for " + block);
            if (source == null) {
                // Exercise the exact in-world branch that crashed on cherry.
                // Untinted leaves must not need a biome or dereference a source.
                var view = (net.minecraft.client.renderer.block.BlockAndTintGetter) java.lang.reflect.Proxy.newProxyInstance(
                        LeafTintCheck.class.getClassLoader(),
                        new Class<?>[]{net.minecraft.client.renderer.block.BlockAndTintGetter.class},
                        (proxy, method, arguments) -> { throw new AssertionError("Untinted leaves queried the world: " + method); });
                int world = ExNihiloClient.originalTint(state,view,net.minecraft.core.BlockPos.ZERO,colors);
                if (world != 0xFFFFFFFF) throw new IllegalStateException("Texture-colored leaves need a neutral multiplier: " + block);
            }
            int previous = actual;
            for (int stage=0; stage<=4; stage++) {
                int tinted = ExNihiloClient.blendTint(actual,stage/4F);
                for (int shift : new int[]{0,8,16})
                    if (((tinted>>shift)&255) < ((previous>>shift)&255))
                        throw new IllegalStateException("Tint must progressively lighten");
                if (source != null && stage<4 && (tinted & 0xFFFFFF)==0xFFFFFF)
                    throw new IllegalStateException("Leaves whitened before maturity");
                previous=tinted;
            }
            if ((previous & 0xFFFFFF)!=0xFFFFFF) throw new IllegalStateException("Mature leaves must be white");
            System.out.println("LEAF TINT PASS: " + block + (source == null
                    ? " safely retains its texture color in-world and without a world"
                    : " starts with its vanilla tint and whitens through growth stages"));
        }
    }
}
