package exnihiloomnia.fabric;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluids;

public final class PorcelainDispensers extends DefaultDispenseItemBehavior {
    public static void register(){
        var behavior=new PorcelainDispensers();
        DispenserBlock.registerBehavior(ExNihiloOmnia.ITEMS.get("bucket_witchwater"),behavior);
        for(String name:new String[]{"empty","water","lava","witchwater"})DispenserBlock.registerBehavior(ExNihiloOmnia.ITEMS.get("bucket_porcelain_"+name),behavior);
    }
    @Override protected ItemStack execute(BlockSource source,ItemStack stack){
        var pos=source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        var level=source.level();
        var bucket=(net.minecraft.world.item.BucketItem)stack.getItem();
        if(BucketSupport.empty(stack)){
            var state=level.getBlockState(pos);var fluid=state.getFluidState();
            if(fluid.isSource()&&(fluid.getType()==Fluids.WATER||fluid.getType()==Fluids.LAVA||fluid.getType()==ExNihiloOmnia.WITCHWATER)&&state.getBlock() instanceof BucketPickup pickup){
                if(!pickup.pickupBlock(null,level,pos,state).isEmpty())return consumeWithRemainder(source,stack,BucketSupport.filled(stack,fluid.getType()));
            }
        }else if(bucket.emptyContents(null,level,pos,null))return consumeWithRemainder(source,stack,BucketSupport.emptied(stack));
        return super.execute(source,stack);
    }
}
