package exnihiloomnia.fabric;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Shared container mapping keeps porcelain containers porcelain through machines. */
public final class BucketSupport {
    public static boolean porcelain(ItemStack stack){return stack.getItem() instanceof PorcelainBucketItem||stack.is(ExNihiloOmnia.ITEMS.get("bucket_porcelain_milk"));}
    public static boolean empty(ItemStack stack){return stack.is(Items.BUCKET)||stack.is(ExNihiloOmnia.ITEMS.get("bucket_porcelain_empty"));}
    public static boolean milk(ItemStack stack){return stack.is(Items.MILK_BUCKET)||stack.is(ExNihiloOmnia.ITEMS.get("bucket_porcelain_milk"));}
    public static Fluid fluid(ItemStack stack){
        if(stack.is(Items.WATER_BUCKET)||stack.is(ExNihiloOmnia.ITEMS.get("bucket_porcelain_water")))return Fluids.WATER;
        if(stack.is(Items.LAVA_BUCKET)||stack.is(ExNihiloOmnia.ITEMS.get("bucket_porcelain_lava")))return Fluids.LAVA;
        if(stack.is(ExNihiloOmnia.ITEMS.get("bucket_witchwater"))||stack.is(ExNihiloOmnia.ITEMS.get("bucket_porcelain_witchwater")))return ExNihiloOmnia.WITCHWATER;
        return Fluids.EMPTY;
    }
    public static ItemStack emptied(ItemStack stack){return new ItemStack(porcelain(stack)?ExNihiloOmnia.ITEMS.get("bucket_porcelain_empty"):Items.BUCKET);}
    public static ItemStack filled(ItemStack empty,Fluid fluid){
        if(fluid==ExNihiloOmnia.WITCHWATER)return new ItemStack(ExNihiloOmnia.ITEMS.get(porcelain(empty)?"bucket_porcelain_witchwater":"bucket_witchwater"));
        if(fluid!=Fluids.WATER&&fluid!=Fluids.LAVA)return ItemStack.EMPTY;
        if(porcelain(empty))return new ItemStack(ExNihiloOmnia.ITEMS.get(fluid==Fluids.WATER?"bucket_porcelain_water":"bucket_porcelain_lava"));
        return new ItemStack(fluid==Fluids.WATER?Items.WATER_BUCKET:Items.LAVA_BUCKET);
    }
    private BucketSupport(){}
}
