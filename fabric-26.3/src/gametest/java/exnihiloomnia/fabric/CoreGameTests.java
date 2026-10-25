package exnihiloomnia.fabric;

import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CoreGameTests {
    private static void check(boolean result, String reason) {
        if (!result) throw new IllegalStateException(reason);
        System.out.println("EX NIHILO PASS: " + reason);
    }
    private static Map<Item, Integer> counts(List<ItemStack> stacks) {
        Map<Item, Integer> result = new HashMap<>();
        for (ItemStack stack : stacks) result.merge(stack.getItem(), stack.getCount(), Integer::sum);
        return result;
    }
    private static int total(List<ItemStack> stacks) { return stacks.stream().mapToInt(ItemStack::getCount).sum(); }
    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    @GameTest
    public void independentHeavyRollsAndMeshGates(GameTestHelper helper) {
        List<ItemStack> heavy = new ArrayList<>(), normal = new ArrayList<>();
        SieveDrops.roll(2, 4, 9, RandomSource.create(123456), heavy::add);
        RandomSource random = RandomSource.create(123456);
        for (int i=0; i<9; i++) SieveDrops.roll(2, 4, 1, random, normal::add);
        check(counts(heavy).equals(counts(normal)), "heavy sieve equals nine consecutive independent normal rolls");
        check(counts(heavy).values().stream().anyMatch(n -> n % 9 != 0), "heavy sieve does not multiply one lucky roll by nine");
        List<ItemStack> low = new ArrayList<>(), high = new ArrayList<>();
        SieveDrops.roll(2, 1, 2000, RandomSource.create(9), low::add);
        SieveDrops.roll(2, 4, 2000, RandomSource.create(9), high::add);
        check(!counts(low).containsKey(Items.DIAMOND) && !counts(low).containsKey(Items.IRON_NUGGET), "string mesh cannot bypass the ore/gem mesh tiers");
        check(counts(high).containsKey(Items.DIAMOND) && counts(high).containsKey(Items.FLINT), "diamond mesh retains lower-tier drops and unlocks diamonds");
        for (Item nugget : new Item[]{Items.IRON_NUGGET, Items.COPPER_NUGGET, Items.GOLD_NUGGET}) {
            check(counts(high).containsKey(nugget), "metal rolls produce vanilla " + nugget);
            check(high.stream().filter(s -> s.is(nugget)).allMatch(s -> s.getCount()==2 || s.getCount()==3),
                    "successful metal rolls give two or three nuggets");
        }
        check(high.stream().noneMatch(s -> ExNihiloOmnia.ITEMS.entrySet().stream()
                .anyMatch(e -> e.getKey().endsWith("_piece") && s.is(e.getValue()))), "new sieving never drops legacy metal pieces");
        helper.succeed();
    }

    @GameTest
    public void sieveLifecycleAndBreakRecovery(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        SieveBlock sieve = (SieveBlock)ExNihiloOmnia.BLOCKS.get("sieve_wood");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        level.setBlock(pos, sieve.defaultBlockState(), 3);
        ItemStack mesh = new ItemStack(ExNihiloOmnia.ITEMS.get("mesh_string"), 2);
        sieve.useItemOn(mesh, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(mesh.getCount()==1 && level.getBlockState(pos).getValue(SieveBlock.MESH)==1, "mesh installation consumes exactly one mesh");
        ItemStack invalid = new ItemStack(ExNihiloOmnia.ITEMS.get("compressed_dirt"));
        sieve.useItemOn(invalid, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(invalid.getCount()==1 && level.getBlockState(pos).getValue(SieveBlock.CONTENT)==0, "normal sieve rejects compressed input without consuming it");
        ItemStack dirt = new ItemStack(Items.DIRT, 2);
        sieve.useItemOn(dirt, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(dirt.getCount()==1 && level.getBlockState(pos).getValue(SieveBlock.CONTENT)==1, "loading consumes exactly one input");
        for (int i=0; i<3; i++) sieve.useWithoutItem(level.getBlockState(pos), level, pos, player, hit(pos));
        BlockState saved = level.getBlockState(pos);
        var encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, saved).getOrThrow();
        BlockState restored = BlockState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        check(restored.getValue(SieveBlock.MESH)==1 && restored.getValue(SieveBlock.CONTENT)==1 && restored.getValue(SieveBlock.PROGRESS)==3, "serialized block state preserves mesh, input and partial progress");
        List<ItemStack> drops = Block.getDrops(saved, level, pos, null, player, ItemStack.EMPTY);
        check(total(drops)==3 && counts(drops).get(Items.DIRT)==1 && counts(drops).get(mesh.getItem())==1, "breaking a loaded sieve returns frame, mesh and one unfinished input");
        for (int i=3; i<8; i++) sieve.useWithoutItem(level.getBlockState(pos), level, pos, player, hit(pos));
        check(level.getBlockState(pos).getValue(SieveBlock.CONTENT)==0 && level.getBlockState(pos).getValue(SieveBlock.PROGRESS)==0, "eight processing clicks finish and clear the consumed material");
        drops = Block.getDrops(level.getBlockState(pos), level, pos, null, player, ItemStack.EMPTY);
        check(total(drops)==2 && !counts(drops).containsKey(Items.DIRT), "completed input cannot be recovered or duplicated by breaking the sieve");
        // A second batch can be cancelled before processing, without losing its mesh.
        sieve.useItemOn(dirt, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        player.setShiftKeyDown(true);
        sieve.useWithoutItem(level.getBlockState(pos), level, pos, player, hit(pos));
        check(level.getBlockState(pos).getValue(SieveBlock.CONTENT)==0 && level.getBlockState(pos).getValue(SieveBlock.MESH)==1, "sneak empty-hand interaction returns unfinished material before removing the mesh");
        sieve.useWithoutItem(level.getBlockState(pos), level, pos, player, hit(pos));
        check(level.getBlockState(pos).getValue(SieveBlock.MESH)==0, "a second sneak interaction removes the installed mesh");
        helper.succeed();
    }

    @GameTest
    public void heavyInputAndHammerConversion(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SieveBlock sieve = (SieveBlock)ExNihiloOmnia.BLOCKS.get("heavy_sieve");
        level.setBlock(pos, sieve.defaultBlockState().setValue(SieveBlock.MESH, 2), 3);
        ItemStack ordinary = new ItemStack(Items.SAND);
        sieve.useItemOn(ordinary, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(ordinary.getCount()==1 && level.getBlockState(pos).getValue(SieveBlock.CONTENT)==0, "heavy sieve rejects ordinary input");
        ItemStack compressed = new ItemStack(ExNihiloOmnia.ITEMS.get("compressed_sand"), 2);
        sieve.useItemOn(compressed, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit(pos));
        check(compressed.getCount()==1 && level.getBlockState(pos).getValue(SieveBlock.CONTENT)==3, "heavy sieve consumes exactly one compressed block");
        List<ItemStack> recovered = Block.getDrops(level.getBlockState(pos), level, pos, null, player, ItemStack.EMPTY);
        check(total(recovered)==3 && counts(recovered).get(compressed.getItem())==1, "breaking the heavy sieve recovers one compressed block");
        check(ExNihiloOmnia.hammerOutput(Blocks.COBBLESTONE)==Blocks.GRAVEL && ExNihiloOmnia.hammerOutput(Blocks.GRAVEL)==Blocks.SAND && ExNihiloOmnia.hammerOutput(Blocks.SAND)==ExNihiloOmnia.BLOCKS.get("dust"), "hammer chain is cobblestone to gravel to sand to dust");
        check(ExNihiloOmnia.hammerOutput(ExNihiloOmnia.BLOCKS.get("compressed_sand"))==ExNihiloOmnia.BLOCKS.get("compressed_dust"), "hammering compressed blocks preserves the nine-block quantity");
        ItemStack hammer = new ItemStack(ExNihiloOmnia.ITEMS.get("hammer_wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        boolean vanillaBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, level.getBlockState(pos), null);
        check(!vanillaBreak && level.getBlockState(pos).isAir() && hammer.getDamageValue()==1, "hammer conversion suppresses vanilla duplicate loot and costs one durability");
        level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
        check(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, level.getBlockState(pos), null), "unrelated blocks retain vanilla breaking behavior");
        helper.succeed();
    }
    @GameTest
    public void compressionCraftingRoundTrip(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getServer().getRecipeManager();
        for (String material : new String[]{"dirt","gravel","sand","dust","soul_sand","gravel_nether","gravel_ender","cobblestone"}) {
            Item base = material.equals("cobblestone") ? Items.COBBLESTONE
                    : ExNihiloOmnia.materialItem(java.util.Arrays.asList(ExNihiloOmnia.MATERIALS).indexOf(material)+1, false);
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, ExNihiloOmnia.id("compress_"+material));
            var recipe = (net.minecraft.world.item.crafting.CraftingRecipe)manager.byKey(key).orElseThrow().value();
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.Collections.nCopies(9,new ItemStack(base)));
            ItemStack compressed = recipe.assemble(input);
            check(recipe.matches(input,level) && compressed.is(ExNihiloOmnia.ITEMS.get("compressed_"+material)) && compressed.getCount()==1,
                    "nine "+material+" craft into exactly one compressed block");
            var reverseKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, ExNihiloOmnia.id("decompress_"+material));
            var reverse = (net.minecraft.world.item.crafting.CraftingRecipe)manager.byKey(reverseKey).orElseThrow().value();
            var reverseInput = net.minecraft.world.item.crafting.CraftingInput.of(1,1,List.of(compressed));
            ItemStack restored = reverse.assemble(reverseInput);
            check(reverse.matches(reverseInput,level) && restored.is(base) && restored.getCount()==9,
                    "one compressed "+material+" crafts back into exactly nine originals");
        }
        helper.succeed();
    }

    @GameTest
    public void nuggetMigrationAndBlockNames(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getServer().getRecipeManager();
        for (String metal : new String[]{"iron","copper","gold"}) {
            Item piece = ExNihiloOmnia.ITEMS.get(metal+"_piece");
            Item nugget = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(metal+"_nugget"));
            Item ingot = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(metal+"_ingot"));
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, ExNihiloOmnia.id("nuggets_from_"+metal+"_pieces"));
            var recipe = (net.minecraft.world.item.crafting.CraftingRecipe)manager.byKey(key).orElseThrow().value();
            var input = net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.Collections.nCopies(4,new ItemStack(piece)));
            ItemStack result = recipe.assemble(input);
            check(recipe.matches(input,level) && result.is(nugget) && result.getCount()==9,
                    "four legacy "+metal+" pieces convert into nine vanilla nuggets");
            var vanillaInput = net.minecraft.world.item.crafting.CraftingInput.of(3,3,java.util.Collections.nCopies(9,new ItemStack(nugget)));
            var vanillaRecipe = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,vanillaInput,level).orElseThrow();
            ItemStack vanillaResult = vanillaRecipe.value().assemble(vanillaInput);
            check(vanillaResult.is(ingot) && vanillaResult.getCount()==1,
                    "nine "+metal+" nuggets craft into one ingot through vanilla recipes");
        }
        for (String material : new String[]{"dirt","gravel","sand","dust","soul_sand","gravel_nether","gravel_ender","cobblestone"}) {
            String id = "compressed_"+material;
            check(ExNihiloOmnia.ITEMS.get(id).getDescriptionId().equals("block.exnihiloomnia."+id),
                    id+" uses its translated block name in inventories");
        }
        helper.succeed();
    }

}
