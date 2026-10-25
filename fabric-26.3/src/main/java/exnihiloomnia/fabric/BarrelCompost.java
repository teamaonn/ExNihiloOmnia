package exnihiloomnia.fabric;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Original Omnia quantities, expressed as thousandths of a dirt block. */
public final class BarrelCompost {
    private BarrelCompost() {}
    private static final Map<String,Integer> VALUES=Map.ofEntries(
            Map.entry("ghast_tear",5),Map.entry("rotten_flesh",85),Map.entry("spider_eye",85),Map.entry("fermented_spider_eye",85),
            Map.entry("wheat",85),Map.entry("bread",125),Map.entry("pumpkin_pie",175),
            Map.entry("egg",80),Map.entry("brown_egg",80),Map.entry("blue_egg",80),
            Map.entry("porkchop",175),Map.entry("cooked_porkchop",175),Map.entry("beef",175),Map.entry("cooked_beef",175),
            Map.entry("mutton",175),Map.entry("cooked_mutton",175),Map.entry("chicken",175),Map.entry("cooked_chicken",175),
            Map.entry("rabbit",175),Map.entry("cooked_rabbit",175),Map.entry("cod",120),Map.entry("cooked_cod",120),
            Map.entry("salmon",120),Map.entry("cooked_salmon",120),Map.entry("tropical_fish",120),Map.entry("pufferfish",120),
            Map.entry("apple",100),Map.entry("golden_apple",200),Map.entry("enchanted_golden_apple",200),
            Map.entry("melon_slice",40),Map.entry("melon",150),Map.entry("pumpkin",150),Map.entry("carved_pumpkin",150),Map.entry("jack_o_lantern",150),
            Map.entry("cactus",100),Map.entry("carrot",80),Map.entry("potato",80),Map.entry("baked_potato",80),Map.entry("poisonous_potato",80),
            Map.entry("lily_pad",80),Map.entry("vine",80),Map.entry("short_grass",80),Map.entry("fern",80),Map.entry("tall_grass",100),Map.entry("large_fern",100),
            Map.entry("nether_wart",80),Map.entry("sugar_cane",80),Map.entry("string",40),
            Map.entry("brown_mushroom",100),Map.entry("red_mushroom",100),
            Map.entry("beetroot",80),Map.entry("sweet_berries",80),Map.entry("glow_berries",80),
            Map.entry("wheat_seeds",40),Map.entry("beetroot_seeds",40),Map.entry("melon_seeds",40),Map.entry("pumpkin_seeds",40));

    public static int value(ItemStack stack) {
        if(stack.isEmpty()) return 0;
        if(stack.is(ItemTags.SAPLINGS) || stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(BlockTags.LEAVES)) return 125;
        if(stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,Identifier.withDefaultNamespace("flowers")))) return 100;
        Identifier id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("minecraft") ? VALUES.getOrDefault(id.getPath(),0) : 0;
    }

    public static int color(ItemStack stack) {
        String name=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if(name.contains("flesh") || name.contains("eye") || name.contains("beef")) return 0xC45631;
        if(name.contains("string") || name.contains("egg")) return 0xEEE9DC;
        if(name.contains("wheat") || name.contains("bread") || name.contains("potato")) return 0xC3AB62;
        if(name.contains("carrot") || name.contains("pumpkin")) return 0xE29B39;
        return 0x4E9A39;
    }
}
