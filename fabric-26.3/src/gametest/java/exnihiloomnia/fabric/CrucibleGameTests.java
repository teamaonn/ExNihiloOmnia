package exnihiloomnia.fabric;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CrucibleGameTests {
    private static final long B=FluidConstants.BUCKET;
    private static Item item(String name){return ExNihiloOmnia.ITEMS.get(name);}
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);System.out.println("EX NIHILO CRUCIBLE PASS: "+why);}
    private static CrucibleEntity place(ServerLevel l,BlockPos p){l.setBlock(p,Blocks.AIR.defaultBlockState(),3);l.setBlock(p,ExNihiloOmnia.BLOCKS.get("crucible").defaultBlockState(),3);return (CrucibleEntity)l.getBlockEntity(p);}
    private static void step(ServerLevel l,BlockPos p,int ticks){for(int i=0;i<ticks;i++)CrucibleEntity.tick(l,p,l.getBlockState(p),(CrucibleEntity)l.getBlockEntity(p));}
    private static void use(CrucibleEntity e,Player p,Item i,int count){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(i,count));e.interact(p,InteractionHand.MAIN_HAND);}
    private static void apply(Player p,InteractionResult r){if(r instanceof InteractionResult.Success s&&s.heldItemTransformedTo()!=null)p.setItemInHand(InteractionHand.MAIN_HAND,s.heldItemTransformedTo());}

    @GameTest public void originalRecipesAndHeat(GameTestHelper h){
        var l=h.getLevel();var manager=l.getServer().getRecipeManager();
        var porcelain=(CraftingRecipe)manager.byKey(ResourceKey.create(Registries.RECIPE,ExNihiloOmnia.id("porcelain"))).orElseThrow().value();
        var ingredients=CraftingInput.of(3,1,List.of(new ItemStack(Items.CLAY_BALL),new ItemStack(item("ash")),new ItemStack(Items.BONE_MEAL)));
        check(porcelain.matches(ingredients,l)&&porcelain.assemble(ingredients).is(item("porcelain")),"porcelain needs clay, ash and bone meal");
        for(String name:List.of("crucible_raw","bucket_porcelain_raw")){
            var recipe=(CraftingRecipe)manager.byKey(ResourceKey.create(Registries.RECIPE,ExNihiloOmnia.id(name))).orElseThrow().value();
            var x=new ItemStack(item("porcelain"));
            var input=name.equals("crucible_raw")?CraftingInput.of(3,3,List.of(x,ItemStack.EMPTY,x,x,ItemStack.EMPTY,x,x,x,x)):CraftingInput.of(3,2,List.of(x,ItemStack.EMPTY,x,ItemStack.EMPTY,x,ItemStack.EMPTY));
            check(recipe.matches(input,l)&&recipe.assemble(input).is(item(name)),"original shaped recipe crafts "+name);
        }
        for(String name:List.of("ash","crucible","bucket_porcelain")){
            var r=(AbstractCookingRecipe)manager.byKey(ResourceKey.create(Registries.RECIPE,ExNihiloOmnia.id(name))).orElseThrow().value();
            var in=new SingleRecipeInput(new ItemStack(name.equals("ash")?Items.STICK:item(name.equals("crucible")?"crucible_raw":"bucket_porcelain_raw")));
            check(r.matches(in,l)&&r.cookingTime()==200&&r.assemble(in).is(item(name.equals("bucket_porcelain")?"bucket_porcelain_empty":name)),"furnace recipe produces "+name);
        }
        check(CrucibleRecipes.heat(Blocks.TORCH.defaultBlockState())==8&&CrucibleRecipes.heat(Blocks.LAVA.defaultBlockState())==16
                &&CrucibleRecipes.heat(Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,4))==12
                &&CrucibleRecipes.heat(Blocks.FIRE.defaultBlockState())==20&&CrucibleRecipes.heat(Blocks.MAGMA_BLOCK.defaultBlockState())==20,"original heat source speeds retained");
        check(CrucibleRecipes.heat(Blocks.FURNACE.defaultBlockState())==0&&CrucibleRecipes.heat(Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT,true))==12
                &&CrucibleRecipes.heat(Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,false))==0,"extinguished heat sources provide no heat");
        h.succeed();
    }
    @GameTest public void meltingYieldsTimingAndReload(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));var player=h.makeMockPlayer(GameType.SURVIVAL);
        Item[] inputs={Items.COBBLESTONE,Items.STONE,Items.GRANITE,Items.DIORITE,Items.ANDESITE,Items.POLISHED_GRANITE,Items.POLISHED_DIORITE,Items.POLISHED_ANDESITE,Items.GRAVEL,Items.NETHERRACK,Items.MAGMA_BLOCK,Items.SNOW,Items.ICE,Items.PACKED_ICE};
        for(Item input:inputs){
            var e=place(l,p);l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),3);use(e,player,input,2);step(l,p,100);
            check(e.work()==50_000&&e.tank.amount==0&&player.getMainHandItem().getCount()==1,"accepted material waits without heat: "+input);
            l.setBlock(p.below(),Blocks.TORCH.defaultBlockState(),3);step(l,p,1500);
            var tag=e.saveWithFullMetadata(l.registryAccess());var restored=(CrucibleEntity)BlockEntity.loadStatic(p,l.getBlockState(p),tag,l.registryAccess());
            check(restored!=null&&restored.work()==e.work()&&restored.processed()==e.processed()&&restored.tank.amount==e.tank.amount,"save/reload preserves exact partial work and fluid");
            l.setBlockEntity(restored);step(l,p,1624);check(restored.work()>0,"torch melting is incomplete before 3125 ticks");step(l,p,1);
            var recipe=CrucibleRecipes.melt(new ItemStack(input));
            check(restored.work()==0&&restored.processed()==0&&restored.tank.amount==recipe.millibuckets()*B/1000&&restored.tank.variant.isOf(recipe.fluid()),"exact original output without rounding loss: "+input);
        }
        var e=place(l,p);l.setBlock(p.below(),Blocks.MAGMA_BLOCK.defaultBlockState(),3);
        for(int i=0;i<4;i++)check(e.addSolid(new ItemStack(Items.COBBLESTONE)),"four cobblestone fit the solid reservoir");
        check(!e.addSolid(new ItemStack(Items.COBBLESTONE))&&!e.addSolid(new ItemStack(Items.ICE)),"full reservoir rejects extra or incompatible material");
        step(l,p,5000);check(e.tank.amount==B&&e.work()==0,"four cobblestone make exactly one bucket");
        use(e,player,Items.BUCKET,1);check(player.getMainHandItem().is(Items.LAVA_BUCKET)&&e.tank.amount==0,"vanilla bucket extracts one bucket");
        h.succeed();
    }
    @GameTest public void capacityAndTransactionalAutomation(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));var e=place(l,p);l.setBlock(p.below(),Blocks.MAGMA_BLOCK.defaultBlockState(),3);
        var fluid=FluidStorage.SIDED.find(l,p,Direction.UP);var items=ItemStorage.SIDED.find(l,p,Direction.UP);
        check(fluid==e.tank&&FluidStorage.SIDED.find(l,p,Direction.NORTH)==null,"fluid automation access is top-only");
        var bottom=ItemStorage.SIDED.find(l,p,Direction.DOWN);
        try(var tx=Transaction.openOuter()){check(bottom==null||bottom.insert(ItemVariant.of(Items.MAGMA_BLOCK),1,tx)==0,"bottom item transfer cannot insert solids");}
        try(var tx=Transaction.openOuter()){check(fluid.insert(FluidVariant.of(Fluids.LAVA),B,tx)==0,"crucible refuses piped fluid input");}
        try(var tx=Transaction.openOuter()){check(items.insert(ItemVariant.of(Items.MAGMA_BLOCK),4,tx)==4,"item-pipe simulation can insert meltables");}
        check(e.getItem(0).isEmpty()&&e.work()==0,"canceled item transfer cannot add solids");
        var remainder=HopperBlockEntity.addItem(null,e,new ItemStack(Items.MAGMA_BLOCK,4),Direction.UP);
        check(remainder.isEmpty(),"vanilla hopper feeds the crucible from above");step(l,p,5004);
        check(e.tank.amount==8*B&&e.work()==0&&e.getItem(0).isEmpty(),"four magma blocks fill the eight-bucket tank exactly");
        check(e.addSolid(new ItemStack(Items.MAGMA_BLOCK)),"full fluid tank retains a new solid load");step(l,p,100);
        check(e.tank.amount==8*B&&e.work()==50_000,"full fluid tank pauses melting");
        try(var tx=Transaction.openOuter()){check(fluid.extract(FluidVariant.of(Fluids.LAVA),B,tx)==B,"extraction simulation reports one bucket");}
        check(e.tank.amount==8*B,"canceled fluid extraction restores tank");
        try(var tx=Transaction.openOuter()){fluid.extract(FluidVariant.of(Fluids.LAVA),B,tx);tx.commit();}step(l,p,1300);
        check(e.tank.amount==8*B&&e.work()+e.processed()==25_000,"melting stops at tank capacity and preserves remaining material");
        try(var tx=Transaction.openOuter()){fluid.extract(FluidVariant.of(Fluids.LAVA),B,tx);tx.commit();}step(l,p,1300);
        check(e.tank.amount==8*B&&e.work()==0&&e.processed()==0,"melting resumes after draining without loss or overflow");
        check(!e.addSolid(new ItemStack(Items.ICE)),"water materials cannot enter a lava tank");h.succeed();
    }
    @GameTest public void porcelainWorldBucketsMilkAndBarrels(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,1,1));var player=h.makeMockPlayer(GameType.SURVIVAL);
        var empty=(PorcelainBucketItem)item("bucket_porcelain_empty");
        for(var fluid:List.of(Fluids.WATER,Fluids.LAVA)){
            l.setBlock(p,fluid.defaultFluidState().createLegacyBlock(),3);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(empty));
            var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);
            apply(player,empty.useAt(l,player,InteractionHand.MAIN_HAND,hit));
            check(BucketSupport.fluid(player.getMainHandItem())==fluid&&l.getFluidState(p).isEmpty(),"world source pickup keeps porcelain container");
            l.setBlock(p,Blocks.STONE.defaultBlockState(),3);var full=(PorcelainBucketItem)player.getMainHandItem().getItem();
            apply(player,full.useAt(l,player,InteractionHand.MAIN_HAND,hit));
            check(player.getMainHandItem().is(empty)&&l.getFluidState(p.above()).getType()==fluid,"world placement returns porcelain empty bucket");
            l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),3);
        }
        l.setBlock(p,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,4),3);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(empty));
        check(empty.useAt(l,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false))==InteractionResult.FAIL,"flowing lava cannot be picked up as a full bucket");
        var cow=EntityTypes.COW.create(l,EntitySpawnReason.TRIGGERED);
        empty.interactLivingEntity(player.getMainHandItem(),player,cow,InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().is(item("bucket_porcelain_milk")),"porcelain bucket milks an adult cow");
        player.addEffect(new MobEffectInstance(MobEffects.POISON,100));
        var milk=player.getMainHandItem();var result=milk.finishUsingItem(l,player);
        check(result.is(empty)&&!player.hasEffect(MobEffects.POISON),"drinking porcelain milk cures effects and returns porcelain bucket");
        l.setBlock(p,ExNihiloOmnia.BARRELS.get("barrel_stone").defaultBlockState(),3);var barrel=(BarrelEntity)l.getBlockEntity(p);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("bucket_porcelain_water")));barrel.interact(player,InteractionHand.MAIN_HAND);
        check(barrel.fullTank()&&player.getMainHandItem().is(empty),"barrel filling returns porcelain empty container");
        barrel.interact(player,InteractionHand.MAIN_HAND);
        check(barrel.tank.amount==0&&player.getMainHandItem().is(item("bucket_porcelain_water")),"barrel extraction returns porcelain filled container");
        h.succeed();
    }
    @GameTest public void porcelainItemTransfersAndPartialBuckets(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));var player=h.makeMockPlayer(GameType.SURVIVAL);
        var e=place(l,p);l.setBlock(p.below(),Blocks.MAGMA_BLOCK.defaultBlockState(),3);e.addSolid(new ItemStack(Items.COBBLESTONE));step(l,p,1250);
        use(e,player,item("bucket_porcelain_empty"),1);check(e.tank.amount==B/4&&player.getMainHandItem().is(item("bucket_porcelain_empty")),"partial tank cannot create a full porcelain bucket");
        for(int i=0;i<3;i++)e.addSolid(new ItemStack(Items.COBBLESTONE));step(l,p,3750);
        use(e,player,item("bucket_porcelain_empty"),2);
        check(e.tank.amount==0&&player.getMainHandItem().getCount()==1&&player.getInventory().countItem(item("bucket_porcelain_lava"))==1,"stacked porcelain empties extract exactly one bucket");
        var context=ContainerItemContext.ofPlayerHand(player,InteractionHand.MAIN_HAND);var fluid=context.find(FluidStorage.ITEM);
        try(var tx=Transaction.openOuter()){check(fluid.insert(FluidVariant.of(Fluids.WATER),B,tx)==B,"porcelain item storage supports water filling");}
        check(player.getMainHandItem().is(item("bucket_porcelain_empty")),"aborted porcelain fill keeps the empty bucket");
        try(var tx=Transaction.openOuter()){fluid.insert(FluidVariant.of(Fluids.WATER),B,tx);tx.commit();}
        check(player.getMainHandItem().is(item("bucket_porcelain_water")),"committed porcelain fill changes container");
        fluid=context.find(FluidStorage.ITEM);
        try(var tx=Transaction.openOuter()){check(fluid.extract(FluidVariant.of(Fluids.WATER),B,tx)==B,"porcelain item storage supports draining");}
        check(player.getMainHandItem().is(item("bucket_porcelain_water")),"aborted porcelain drain keeps water bucket");
        try(var tx=Transaction.openOuter()){fluid.extract(FluidVariant.of(Fluids.WATER),B,tx);tx.commit();}
        check(player.getMainHandItem().is(item("bucket_porcelain_empty")),"committed porcelain drain returns porcelain empty");
        h.succeed();
    }
    @GameTest public void dispenserContainersAndAshGrowth(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));
        l.setBlock(p,Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING,Direction.EAST),3);
        var entity=(net.minecraft.world.level.block.entity.DispenserBlockEntity)l.getBlockEntity(p);
        var source=new net.minecraft.core.dispenser.BlockSource(l,p,l.getBlockState(p),entity);
        var target=p.east();
        for(var fluid:List.of(Fluids.WATER,Fluids.LAVA)){
            l.setBlock(target,fluid.defaultFluidState().createLegacyBlock(),3);
            var empty=item("bucket_porcelain_empty");
            var result=net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(empty).dispense(source,new ItemStack(empty));
            check(BucketSupport.fluid(result)==fluid&&l.getFluidState(target).isEmpty(),"dispenser collects source into porcelain bucket");
            result=net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(result.getItem()).dispense(source,result);
            check(result.is(empty)&&l.getFluidState(target).getType()==fluid,"dispenser pours fluid and keeps porcelain container");
        }
        var furnacePos=p.north();l.setBlock(furnacePos,Blocks.FURNACE.defaultBlockState(),3);
        var furnace=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)l.getBlockEntity(furnacePos);
        furnace.setItem(0,new ItemStack(Items.STICK));furnace.setItem(1,new ItemStack(item("bucket_porcelain_lava")));
        for(int i=0;i<201;i++)net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(l,furnacePos,l.getBlockState(furnacePos),furnace);
        check(furnace.getItem(2).is(item("ash"))&&furnace.getItem(1).is(item("bucket_porcelain_empty")),"porcelain lava fuels a real furnace and returns porcelain bucket");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var crop=p.west();
        l.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState(),3);
        var ash=item("ash");
        int grown=0;
        l.getRandom().setSeed(42);
        for(int i=0;i<100;i++){
            l.setBlock(crop,Blocks.WHEAT.defaultBlockState(),3);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ash,2));
            ash.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(crop),Direction.UP,crop,false)));
            if(l.getBlockState(crop).getValue(net.minecraft.world.level.block.CropBlock.AGE)>0)grown++;
            check(player.getMainHandItem().getCount()==1,"ash use consumes exactly one item on a growable crop");
        }
        check(grown>=10&&grown<=35,"ash grows crops with the original one-in-five chance");
        h.succeed();
    }

}
