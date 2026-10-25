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
        for (var block : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SPRUCE_LEAVES}) {
            var state = block.defaultBlockState();
            int expected = colors.getTintSource(state,0).color(state);
            int actual = ExNihiloClient.originalTint(state,null,null,colors);
            if ((actual & 0xFFFFFF) != (expected & 0xFFFFFF) || (actual & 0xFFFFFF)==0xFFFFFF)
                throw new IllegalStateException("Starting tint must retain vanilla color for " + block);
            int previous = actual;
            for (int stage=0; stage<=4; stage++) {
                int tinted = ExNihiloClient.blendTint(actual,stage/4F);
                for (int shift : new int[]{0,8,16})
                    if (((tinted>>shift)&255) < ((previous>>shift)&255))
                        throw new IllegalStateException("Tint must progressively lighten");
                if (stage<4 && (tinted & 0xFFFFFF)==0xFFFFFF)
                    throw new IllegalStateException("Leaves whitened before maturity");
                previous=tinted;
            }
            if ((previous & 0xFFFFFF)!=0xFFFFFF) throw new IllegalStateException("Mature leaves must be white");
            System.out.println("LEAF TINT PASS: " + block + " starts with its vanilla tint and whitens through growth stages");
        }
    }
}
