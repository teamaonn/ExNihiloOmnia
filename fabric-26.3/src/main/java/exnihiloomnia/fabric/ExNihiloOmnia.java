package exnihiloomnia.fabric;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.food.FoodProperties;

public final class ExNihiloOmnia implements ModInitializer {
    public static final String MOD_ID = "exnihiloomnia";
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<Block, InfestedLeavesBlock> INFESTABLE = new LinkedHashMap<>();
    public static BlockEntityType<InfestedLeavesEntity> INFESTED_LEAVES_ENTITY;
    public static final Map<String,BarrelBlock> BARRELS = new LinkedHashMap<>();
    public static BlockEntityType<BarrelEntity> BARREL_ENTITY;
    public static BlockEntityType<CrucibleEntity> CRUCIBLE_ENTITY;
    public static net.minecraft.world.level.material.FlowingFluid WITCHWATER,FLOWING_WITCHWATER;
    public static net.minecraft.world.level.block.LiquidBlock WITCHWATER_BLOCK;
    public static final String[] WOOD_TYPES={"oak","spruce","birch","jungle","acacia","dark_oak","pale_oak","crimson","warped","mangrove","bamboo","cherry","poplar"};
    public static final String[] COLORS={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
    public static final String[] LEAF_TYPES = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "pale_oak", "azalea", "flowering_azalea", "red_poplar", "orange_poplar", "yellow_poplar"};
    public static final String[] MATERIALS = {"dirt", "gravel", "sand", "dust", "soul_sand", "gravel_nether", "gravel_ender"};
    public static final String[] MESHES = {"mesh_string", "mesh_flint", "mesh_iron", "mesh_diamond"};

    public static Identifier id(String name) { return Identifier.fromNamespaceAndPath(MOD_ID, name); }

    @Override
    public void onInitialize() {
        block("dust", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SAND));
        block("gravel_nether", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL));
        block("gravel_ender", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL));
        block("sieve_wood", p -> new SieveBlock(false, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());
        block("heavy_sieve", p -> new SieveBlock(true, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());
        for (String name : MATERIALS) {
            block("compressed_" + name, Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).strength(1.5F));
        }
        block("compressed_cobblestone", Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).strength(3F));
        for (String name : MESHES) item(name, Item::new, new Item.Properties());
        item("stone_pebble", Item::new, new Item.Properties());
        // Retain old IDs for existing saves; conversion recipes preserve their
        // value. New sieving rewards use the vanilla nuggets instead.
        for (String ore : new String[]{"iron", "copper", "gold"}) item(ore + "_piece", Item::new, new Item.Properties());
        hammer("wood", ToolMaterial.WOOD);
        hammer("stone", ToolMaterial.STONE);
        hammer("iron", ToolMaterial.IRON);
        hammer("gold", ToolMaterial.GOLD);
        hammer("diamond", ToolMaterial.DIAMOND);
        hammer("netherite", ToolMaterial.NETHERITE);
        for (String species : LEAF_TYPES) {
            Block source = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(species + "_leaves"));
            String name = species.equals("oak") ? "infested_leaves" : "infested_" + species + "_leaves";
            block(name, p -> new InfestedLeavesBlock(source, p), BlockBehaviour.Properties.ofFullCopy(source));
            INFESTABLE.put(source, (InfestedLeavesBlock) BLOCKS.get(name));
            net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(BLOCKS.get(name), 30, 60);
        }
        INFESTED_LEAVES_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("infested_leaves"),
                new BlockEntityType<>(InfestedLeavesEntity::new, new java.util.HashSet<>(INFESTABLE.values())));
        item("silkworm", SilkwormItem::new, new Item.Properties());
        item("cooked_silkworm", Item::new, new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(.2F).build()));
        item("crook_wood", CrookItem::new, CrookItem.properties(false));
        item("crook_bone", CrookItem::new, CrookItem.properties(true));
        CrookItem.registerEvents();
        for(String wood:WOOD_TYPES) {
            String name=wood.equals("oak")?"barrel_wood":"barrel_"+wood;
            Block planks=BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(wood+"_planks"));
            boolean burns=!wood.equals("crimson")&&!wood.equals("warped");
            barrel(name,true,burns,BlockBehaviour.Properties.ofFullCopy(planks));
            if(burns)net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(BLOCKS.get(name),5,20);
        }
        barrel("barrel_stone",false,false,BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.STONE).sound(net.minecraft.world.level.block.SoundType.STONE));
        barrel("barrel_glass",false,false,BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
        for(String color:COLORS)barrel("barrel_glass_"+color,false,false,BlockBehaviour.Properties.ofFullCopy(
                BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color+"_stained_glass"))));
        BARREL_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("barrel"),new BlockEntityType<>(BarrelEntity::new,new java.util.HashSet<>(BARRELS.values())));
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((barrel,side)->barrel.tank,BARREL_ENTITY);
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (barrel,side)->net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(barrel,side),BARREL_ENTITY);

        item("ash",AshItem::new,new Item.Properties());
        item("porcelain",Item::new,new Item.Properties());
        block("crucible_raw",RawCrucibleBlock::new,BlockBehaviour.Properties.ofFullCopy(Blocks.CLAY).strength(1F).noOcclusion());
        block("crucible",CrucibleBlock::new,BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).strength(1F).noOcclusion()
                .lightLevel(s->s.getValue(CrucibleBlock.CONTENT)==CrucibleBlock.Content.LAVA?15:s.getValue(CrucibleBlock.CONTENT)==CrucibleBlock.Content.MAGMA?3:0));
        CRUCIBLE_ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("crucible"),new BlockEntityType<>(CrucibleEntity::new,java.util.Set.of(BLOCKS.get("crucible"))));
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((e,side)->side==net.minecraft.core.Direction.UP||side==null?e.tank:null,CRUCIBLE_ENTITY);
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((e,side)->side==net.minecraft.core.Direction.UP||side==null?net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(e,net.minecraft.core.Direction.UP):null,CRUCIBLE_ENTITY);
        item("bucket_porcelain_raw",Item::new,new Item.Properties());
        item("bucket_porcelain_empty",p->new PorcelainBucketItem(net.minecraft.world.level.material.Fluids.EMPTY,p),new Item.Properties().stacksTo(16));
        Item emptyPorcelain=ITEMS.get("bucket_porcelain_empty");
        item("bucket_porcelain_water",p->new PorcelainBucketItem(net.minecraft.world.level.material.Fluids.WATER,p),new Item.Properties().stacksTo(1).craftRemainder(emptyPorcelain));
        item("bucket_porcelain_lava",p->new PorcelainBucketItem(net.minecraft.world.level.material.Fluids.LAVA,p),new Item.Properties().stacksTo(1).craftRemainder(emptyPorcelain)
                .cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_LAVA_BUCKET));
        item("bucket_porcelain_milk",Item::new,new Item.Properties().stacksTo(1).craftRemainder(emptyPorcelain).usingConvertsTo(emptyPorcelain)
                .component(net.minecraft.core.component.DataComponents.CONSUMABLE,net.minecraft.world.item.component.Consumables.MILK_BUCKET));
        WITCHWATER=Registry.register(BuiltInRegistries.FLUID,id("witchwater"),new WitchwaterFluid.Source());
        FLOWING_WITCHWATER=Registry.register(BuiltInRegistries.FLUID,id("flowing_witchwater"),new WitchwaterFluid.Flowing());
        var witchKey=ResourceKey.create(Registries.BLOCK,id("witchwater"));
        WITCHWATER_BLOCK=Registry.register(BuiltInRegistries.BLOCK,witchKey,new net.minecraft.world.level.block.LiquidBlock(WITCHWATER,BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).setId(witchKey)));
        item("bucket_witchwater",p->new net.minecraft.world.item.BucketItem(WITCHWATER,p),new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET));
        item("bucket_porcelain_witchwater",p->new PorcelainBucketItem(WITCHWATER,p),new Item.Properties().stacksTo(1).craftRemainder(emptyPorcelain));
        item("spores",SporesItem::new,new Item.Properties());
        for(String doll:new String[]{"doll","doll_blaze","doll_ender"})item(doll,Item::new,new Item.Properties());
        PorcelainTransfers.register();
        PorcelainDispensers.register();

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            ItemStack tool = player.getMainHandItem();
            if (!(tool.getItem() instanceof HammerItem) || player.isCreative()) return true;
            Block output = hammerOutput(state.getBlock());
            if (output == null) return true;
            // The event fires after the normal mining time, on the server. Replacing
            // the vanilla break prevents its loot table from producing a second drop.
            if (level.removeBlock(pos, false)) {
                Block.popResource(level, pos, new ItemStack(output));
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            return false;
        });
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.exnihiloomnia"))
                .icon(() -> new ItemStack(ITEMS.get("sieve_wood")))
                .displayItems((parameters, output) -> ITEMS.forEach((name, item) -> {
                    if (!name.endsWith("_piece")) output.accept(item);
                })).build());
        SieveDrops.initialize();
    }

    private static void hammer(String material, ToolMaterial tier) {
        Item.Properties properties = new Item.Properties().pickaxe(tier, 1F, -2.8F);
        if (material.equals("netherite")) properties.fireResistant();
        item("hammer_" + material, HammerItem::new, properties);
    }

    private static void barrel(String name,boolean wooden,boolean burns,BlockBehaviour.Properties properties) {
        block(name,p->new BarrelBlock(wooden,burns,p),properties.strength(1.2F).noOcclusion()
                .lightLevel(state->(state.getValue(BarrelBlock.CONTENTS)==BarrelBlock.Contents.LAVA||state.getValue(BarrelBlock.CONTENTS)==BarrelBlock.Contents.SUMMON_BLAZE)?15:0));
        BARRELS.put(name,(BarrelBlock)BLOCKS.get(name));
    }

    private static void item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key))));
    }

    private static void block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
        BLOCKS.put(name, block);
        item(name, p -> new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
    }

    public static Item materialItem(int content, boolean compressed) {
        if (content < 1 || content > MATERIALS.length) throw new IllegalArgumentException("Invalid sieve content " + content);
        String name = MATERIALS[content - 1];
        if (compressed) return ITEMS.get("compressed_" + name);
        return switch (name) {
            case "dirt" -> Blocks.DIRT.asItem();
            case "gravel" -> Blocks.GRAVEL.asItem();
            case "sand" -> Blocks.SAND.asItem();
            case "soul_sand" -> Blocks.SOUL_SAND.asItem();
            default -> ITEMS.get(name);
        };
    }

    public static Block hammerOutput(Block input) {
        if (input == Blocks.COBBLESTONE || input == Blocks.COBBLED_DEEPSLATE) return Blocks.GRAVEL;
        if (input == Blocks.GRAVEL) return Blocks.SAND;
        if (input == Blocks.SAND || input == Blocks.RED_SAND) return BLOCKS.get("dust");
        if (input == Blocks.NETHERRACK) return BLOCKS.get("gravel_nether");
        if (input == Blocks.END_STONE) return BLOCKS.get("gravel_ender");
        if (input == BLOCKS.get("compressed_cobblestone")) return BLOCKS.get("compressed_gravel");
        if (input == BLOCKS.get("compressed_gravel")) return BLOCKS.get("compressed_sand");
        if (input == BLOCKS.get("compressed_sand")) return BLOCKS.get("compressed_dust");
        return null;
    }

    public static final class HammerItem extends Item {
        public HammerItem(Properties properties) { super(properties); }
    }
}
