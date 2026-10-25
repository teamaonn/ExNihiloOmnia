package exnihiloomnia.fabric;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.world.level.material.Fluids;

public final class PorcelainTransfers {
    public static void register(){
        var empty=ExNihiloOmnia.ITEMS.get("bucket_porcelain_empty");
        var water=ExNihiloOmnia.ITEMS.get("bucket_porcelain_water");
        var lava=ExNihiloOmnia.ITEMS.get("bucket_porcelain_lava");
        var witch=ExNihiloOmnia.ITEMS.get("bucket_porcelain_witchwater");
        FluidStorage.combinedItemApiProvider(empty).register(context->new CombinedStorage<>(List.of(
                new EmptyItemFluidStorage(context,water,Fluids.WATER,FluidConstants.BUCKET),
                new EmptyItemFluidStorage(context,witch,ExNihiloOmnia.WITCHWATER,FluidConstants.BUCKET),
                new EmptyItemFluidStorage(context,lava,Fluids.LAVA,FluidConstants.BUCKET))));
        FluidStorage.combinedItemApiProvider(witch).register(context->new FullItemFluidStorage(context,empty,FluidVariant.of(ExNihiloOmnia.WITCHWATER),FluidConstants.BUCKET));
        FluidStorage.combinedItemApiProvider(ExNihiloOmnia.ITEMS.get("bucket_witchwater")).register(context->new FullItemFluidStorage(context,net.minecraft.world.item.Items.BUCKET,FluidVariant.of(ExNihiloOmnia.WITCHWATER),FluidConstants.BUCKET));
        FluidStorage.combinedItemApiProvider(net.minecraft.world.item.Items.BUCKET).register(context->new EmptyItemFluidStorage(context,ExNihiloOmnia.ITEMS.get("bucket_witchwater"),ExNihiloOmnia.WITCHWATER,FluidConstants.BUCKET));
        FluidStorage.combinedItemApiProvider(water).register(context->new FullItemFluidStorage(context,empty,FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET));
        FluidStorage.combinedItemApiProvider(lava).register(context->new FullItemFluidStorage(context,empty,FluidVariant.of(Fluids.LAVA),FluidConstants.BUCKET));
    }
    private PorcelainTransfers(){}
}
