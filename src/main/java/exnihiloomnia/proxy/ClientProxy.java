package exnihiloomnia.proxy;

import exnihiloomnia.ENO;
import exnihiloomnia.ENOConfig;
import exnihiloomnia.blocks.ENOBlocks;
import exnihiloomnia.blocks.barrels.renderer.BarrelRenderer;
import exnihiloomnia.blocks.barrels.tileentity.TileEntityBarrel;
import exnihiloomnia.blocks.crucibles.renderer.CrucibleRenderer;
import exnihiloomnia.blocks.crucibles.tileentity.TileEntityCrucible;
import exnihiloomnia.blocks.leaves.InfestedLeavesRenderer;
import exnihiloomnia.blocks.leaves.TileEntityInfestedLeaves;
import exnihiloomnia.blocks.sieves.renderer.SieveRenderer;
import exnihiloomnia.blocks.sieves.tileentity.TileEntitySieve;
import exnihiloomnia.client.models.ENOModels;
import exnihiloomnia.compatibility.ENOCompatibility;
import exnihiloomnia.compatibility.forestry.ForestryCompatibility;
import exnihiloomnia.compatibility.industrialcraft.IC2;
import exnihiloomnia.entities.thrown.stone.EntityStone;
import exnihiloomnia.entities.thrown.stone.EntityStoneRenderer;
import exnihiloomnia.items.ENOItems;
import exnihiloomnia.items.ores.ItemOre;
import exnihiloomnia.registries.ore.OreRegistry;
import exnihiloomnia.util.enums.EnumOreBlockType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;

//Commands that only execute on the client.
public class ClientProxy extends Proxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        ENOModels.register();

        registerItemModels();
        registerBlockModels();

        if (IC2.SEED_RUBBER != null)
            IC2.loadTexture();

        if (ENOCompatibility.BEE_TRAP != null)
            ForestryCompatibility.loadTexture();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        registerBlockRenderers();
        registerEntityRenderers();

        OreRegistry.regColors();

        if (!ENOConfig.pretty_leaves) {
            Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) -> {

                if (worldIn != null && pos != null) {
                    TileEntity tileEntity = worldIn.getTileEntity(pos);

                    if (tileEntity != null && tileEntity instanceof TileEntityInfestedLeaves) {
                        TileEntityInfestedLeaves leaves = ((TileEntityInfestedLeaves) tileEntity);

                        return leaves.getColorForTint(0).toInt();
                    }
                }
                return 0;
            }, ENOBlocks.INFESTED_LEAVES);
        }
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void setCustomStateMapper(Block block, final String location) {
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return new ModelResourceLocation(location);
            }
        });
    }

    public static void registerItemModels() {
        for (Item item : ENOItems.getItems()) {
            registerRenderer(item);
        }
    }

    public static void registerBlockModels() {
        for (Block block : ENOBlocks.getBlocks()) {
            registerRenderer(block);
        }

        for (EnumOreBlockType type : EnumOreBlockType.values()) {
            ModelResourceLocation loc = type.getLocation();

            for (Block ore : OreRegistry.blocks.values()) {
                Item item = Item.getItemFromBlock(ore);

                if (item != null) {
                    ModelLoader.setCustomModelResourceLocation(item, type.ordinal(), loc);
                }
            }
        }
    }

    private void registerBlockRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBarrel.class, new BarrelRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySieve.class, new SieveRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCrucible.class, new CrucibleRenderer());

        if (ENOConfig.pretty_leaves)
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityInfestedLeaves.class, new InfestedLeavesRenderer());
    }

    private void registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityStone.class, EntityStoneRenderer.INSTANCE);
    }

    private static void registerRenderer(Block block) {
        Item item = Item.getItemFromBlock(block);

        if (item != null) {
            if (item.getHasSubtypes()) {
                ArrayList<ItemStack> list = new ArrayList<>();

                block.getSubBlocks(item, null, list);

                for (ItemStack i : list) {
                    ModelBakery.registerItemVariants(i.getItem(), i.getItem().getRegistryName());
                    ModelLoader.setCustomModelResourceLocation(item, i.getItemDamage(), new ModelResourceLocation(ENO.MODID + ":" + i.getUnlocalizedName().substring(5)));
                }
            }
            else {
                ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(ENO.MODID + ":" + item.getUnlocalizedName().substring(5)));
            }
        }
    }

    private static void registerRenderer(Item item) {
        if (item.getHasSubtypes()) {
            ArrayList<ItemStack> list = new ArrayList<>();

            item.getSubItems(item, null, list);

            for (ItemStack i : list) {
                ModelBakery.registerItemVariants(i.getItem(), i.getItem().getRegistryName());
                if (i.getItem() instanceof ItemOre)
                    ModelLoader.setCustomModelResourceLocation(item, i.getItemDamage(), new ModelResourceLocation(ENO.MODID + ":ore_" + ((ItemOre) i.getItem()).getType().getName()));
                else
                    ModelLoader.setCustomModelResourceLocation(item, i.getItemDamage(), new ModelResourceLocation(ENO.MODID + ":" + i.getUnlocalizedName().substring(5)));
            }
        }
        else {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(ENO.MODID + ":" + item.getUnlocalizedName().substring(5)));
        }
    }
}
