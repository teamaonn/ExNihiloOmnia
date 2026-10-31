package exnihiloomnia.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SieveDrops {
    public record Drop(int input, int minimumMesh, Item item, int count, float bonusChance, float probability) {}
    private static final List<Drop> DROPS = new ArrayList<>();
    private SieveDrops() {}

    public static void initialize() {
        DROPS.clear();
        // Dirt: retain the original independent pebble and seed rolls, while
        // using modern vanilla crops/saplings instead of obsolete seed items.
        Item pebble = ExNihiloOmnia.ITEMS.get("stone_pebble");
        add(1, 1, pebble, 1F); add(1, 1, pebble, 1F);
        add(1, 1, pebble, .75F); add(1, 1, pebble, .50F);
        for (Item seed : new Item[]{Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO}) add(1, 1, seed, .07F);
        for (Item seed : new Item[]{Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.SUGAR_CANE}) add(1, 1, seed, .03F);
        add(1, 1, Items.OAK_SAPLING, .05F);
        for (Item sapling : new Item[]{Items.BIRCH_SAPLING, Items.SPRUCE_SAPLING, Items.JUNGLE_SAPLING,
                Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING, Items.CHERRY_SAPLING, Items.PALE_OAK_SAPLING,
                Items.MANGROVE_PROPAGULE, Items.AZALEA}) add(1, 1, sapling, .02F);
        add(1, 1, Items.BAMBOO, .03F);
        add(1, 2, Items.TORCHFLOWER_SEEDS, .01F); add(1, 2, Items.PITCHER_POD, .01F);
        add(2, 1, Items.FLINT, .25F); add(2, 1, Items.COAL, .13F);
        add(2, 2, Items.LAPIS_LAZULI, .05F); add(2, 2, Items.LAPIS_LAZULI, .07F);
        add(2, 3, Items.EMERALD, .01F); add(2, 3, Items.EMERALD, .01F);
        add(2, 4, Items.DIAMOND, .01F); add(2, 4, Items.DIAMOND, .01F);
        for (int input : new int[]{2, 3, 4}) {
            addNuggets(input, Items.IRON_NUGGET, .20F);
            addNuggets(input, Items.COPPER_NUGGET, .24F);
            addNuggets(input, Items.GOLD_NUGGET, .08F);
        }
        add(3, 1, Items.COCOA_BEANS, .03F); add(3, 1, Items.CACTUS, .03F);
        add(3, 1, ExNihiloOmnia.ITEMS.get("spores"), .01F);
        add(3, 1, Items.BROWN_MUSHROOM, .01F); add(3, 1, Items.RED_MUSHROOM, .01F);
        add(4, 1, Items.BONE_MEAL, .20F); add(4, 2, Items.GUNPOWDER, .07F);
        add(4, 3, Items.BLAZE_POWDER, .05F); add(4, 2, Items.REDSTONE, .13F); add(4, 2, Items.GLOWSTONE_DUST, .06F);
        add(5, 1, Items.QUARTZ, 1F); add(5, 1, Items.NETHER_WART, .06F); add(5, 3, Items.GHAST_TEAR, .01F);
        add(6, 3, Items.GHAST_TEAR, .07F); add(6, 2, Items.GLOWSTONE_DUST, .03F);
        addNuggets(6, Items.GOLD_NUGGET, .12F);
        add(7, 1, Items.CHORUS_FLOWER, .10F); add(7, 3, Items.ENDER_PEARL, .03F);
    }

    /** Immutable view of the live drop table, including independent duplicate rolls. */
    public static List<Drop> entries() { return List.copyOf(DROPS); }

    private static void add(int input, int mesh, Item item, float probability) {
        DROPS.add(new Drop(input, mesh, item, 1, 0F, probability));
    }

    private static void addNuggets(int input, Item item, float probability) {
        // Four old pieces yielded one ingot. A successful metal roll now yields
        // two nuggets, with a 25% chance of a third: the same 2.25-nugget average.
        DROPS.add(new Drop(input, 2, item, 2, .25F, probability));
    }

    /** Nine independent passes through the same table, never nine copies of a
     * single lucky result. Higher meshes retain all lower-tier rewards. */
    public static void roll(int input, int mesh, int repetitions, RandomSource random, Consumer<ItemStack> output) {
        for (int i = 0; i < repetitions; i++) {
            for (Drop drop : DROPS) {
                if (drop.input == input && mesh >= drop.minimumMesh && random.nextFloat() < drop.probability) {
                    int count = drop.count;
                    if (drop.bonusChance > 0F && random.nextFloat() < drop.bonusChance) count++;
                    output.accept(new ItemStack(drop.item, count));
                }
            }
        }
    }
}
