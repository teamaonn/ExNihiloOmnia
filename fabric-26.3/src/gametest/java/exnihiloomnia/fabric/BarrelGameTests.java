package exnihiloomnia.fabric;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

public final class BarrelGameTests {
    private static void check(boolean pass,String reason){if(!pass)throw new IllegalStateException(reason);System.out.println("EX NIHILO BARREL PASS: "+reason);}
    private static BarrelEntity place(ServerLevel level,BlockPos pos,String name){
        // Replacing a barrel with the same block/state intentionally retains its
        // entity in Minecraft. Fixtures need an actual removal for a fresh barrel.
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        level.setBlock(pos,ExNihiloOmnia.BARRELS.get(name).defaultBlockState(),3);
        return (BarrelEntity)level.getBlockEntity(pos);
    }
    private static void use(BarrelEntity barrel,Player player,Item item,int count){player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,count));barrel.interact(player,InteractionHand.MAIN_HAND);}
    private static void step(ServerLevel level,BlockPos pos,int count){for(int i=0;i<count;i++)BarrelEntity.tick(level,pos,level.getBlockState(pos),(BarrelEntity)level.getBlockEntity(pos));}
    private static int dropped(ServerLevel level,BlockPos pos,Item item){return level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
    private static int inventory(Player player,Item item){return player.getInventory().countItem(item);}
    private static void fill(BarrelEntity barrel,Fluid fluid,long amount){try(var tx=Transaction.openOuter()){check(barrel.tank.insert(FluidVariant.of(fluid),amount,tx)==amount,"transactional fluid fill accepts the requested amount");tx.commit();}}

    @GameTest
    public void variantsRecipesAndCompostLifecycle(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));var player=helper.makeMockPlayer(GameType.SURVIVAL);
        check(ExNihiloOmnia.BARRELS.size()==31,"all 13 wood, stone, glass and 16 stained-glass barrel variants are registered");
        var recipes=level.getServer().getRecipeManager();
        for(var entry:ExNihiloOmnia.BARRELS.entrySet()){
            String name=entry.getKey(),material,slab;
            if(name.equals("barrel_stone")){material="stone";slab="stone_slab";}
            else if(name.equals("barrel_glass")){material="glass";slab="glass_pane";}
            else if(name.startsWith("barrel_glass_")){String color=name.substring("barrel_glass_".length());material=color+"_stained_glass";slab=color+"_stained_glass_pane";}
            else {String wood=name.equals("barrel_wood")?"oak":name.substring("barrel_".length());material=wood+"_planks";slab=wood+"_slab";}
            Item m=BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(material));
            Item s=BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(slab));
            var input=CraftingInput.of(3,3,List.of(new ItemStack(m),ItemStack.EMPTY,new ItemStack(m),new ItemStack(m),ItemStack.EMPTY,new ItemStack(m),new ItemStack(m),new ItemStack(s),new ItemStack(m)));
            var recipe=(CraftingRecipe)recipes.byKey(ResourceKey.create(Registries.RECIPE,ExNihiloOmnia.id(name))).orElseThrow().value();
            check(recipe.matches(input,level)&&recipe.assemble(input).is(entry.getValue().asItem()),"original shell recipe crafts "+name);
            var barrel=place(level,pos,name);
            check(barrel.getType().isValid(entry.getValue().defaultBlockState()) && entry.getValue().asItem().getDescriptionId().equals("block.exnihiloomnia."+name),"barrel variant has the correct entity type and translated item-name key");
        }
        var barrel=place(level,pos,"barrel_wood");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_LEAVES,9));
        for(int i=0;i<8;i++)barrel.interact(player,InteractionHand.MAIN_HAND);
        check(barrel.compostAmount()==1000&&player.getMainHandItem().getCount()==1,"eight leaves fill compost and consume exactly eight items");
        barrel.interact(player,InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().getCount()==1,"full compost rejects additional ingredients without consumption");
        step(level,pos,500);
        var tag=barrel.saveWithFullMetadata(level.registryAccess());
        var restored=(BarrelEntity)BlockEntity.loadStatic(pos,level.getBlockState(pos),tag,level.registryAccess());
        check(restored!=null&&restored.compostAmount()==1000&&restored.compostTicks()==500,"save/reload preserves full compost and exact processing progress");
        level.setBlockEntity(restored);step(level,pos,499);
        check(restored.getItem(0).isEmpty(),"compost does not finish before 1000 loaded ticks");
        step(level,pos,1);
        check(restored.getItem(0).is(Items.DIRT)&&restored.getItem(0).getCount()==1,"compost completes after 1000 ticks as one dirt block");
        int before=inventory(player,Items.DIRT);use(restored,player,Items.AIR,0);
        check(inventory(player,Items.DIRT)==before+1&&restored.getItem(0).isEmpty(),"manual collection returns one output and resets the barrel");
        check(level.getBlockState(pos).getValue(BarrelBlock.CONTENTS)==BarrelBlock.Contents.EMPTY,"collecting output clears the displayed barrel contents");
        check(BarrelCompost.value(new ItemStack(Items.ROTTEN_FLESH))==85 && BarrelCompost.value(new ItemStack(Items.DANDELION))==100,"original flesh and flower compost quantities are retained");
        helper.succeed();
    }

    @GameTest
    public void bucketsAlchemyAndCreative(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var barrel=place(level,pos,"barrel_stone");
        use(barrel,player,Items.WATER_BUCKET,1);
        check(barrel.fluid()==Fluids.WATER&&barrel.fullTank()&&player.getMainHandItem().is(Items.BUCKET),"one water bucket fills the tank and returns an empty bucket");
        use(barrel,player,Items.LAVA_BUCKET,1);
        check(barrel.fluid()==Fluids.WATER&&player.getMainHandItem().is(Items.LAVA_BUCKET),"incompatible fluid cannot mix, spill or consume a bucket");
        use(barrel,player,Items.BUCKET,2);
        check(barrel.tank.amount==0&&player.getMainHandItem().is(Items.BUCKET)&&player.getMainHandItem().getCount()==1&&inventory(player,Items.WATER_BUCKET)==1,"stacked empty buckets collect exactly one fluid bucket");
        use(barrel,player,Items.WATER_BUCKET,1);
        use(barrel,player,ExNihiloOmnia.ITEMS.get("dust"),2);
        check(barrel.getItem(0).is(Items.CLAY)&&barrel.tank.amount==0&&player.getMainHandItem().getCount()==1,"water plus one dust consumes the fluid and produces one clay block");
        use(barrel,player,Items.AIR,0);
        use(barrel,player,Items.LAVA_BUCKET,1);use(barrel,player,Items.REDSTONE,2);
        check(barrel.getItem(0).is(Items.NETHERRACK)&&player.getMainHandItem().getCount()==1,"lava plus redstone produces netherrack using one ingredient");
        use(barrel,player,Items.AIR,0);use(barrel,player,Items.LAVA_BUCKET,1);use(barrel,player,Items.GLOWSTONE_DUST,1);
        check(barrel.getItem(0).is(Items.END_STONE),"lava plus glowstone produces End stone");
        use(barrel,player,Items.AIR,0);fill(barrel,Fluids.WATER,FluidConstants.BUCKET/2);
        use(barrel,player,ExNihiloOmnia.ITEMS.get("dust"),1);
        check(barrel.getItem(0).isEmpty()&&player.getMainHandItem().getCount()==1,"partial tanks cannot craft full-bucket recipes");
        use(barrel,player,Items.BUCKET,1);
        check(player.getMainHandItem().is(Items.BUCKET)&&barrel.tank.amount==FluidConstants.BUCKET/2,"partial tanks cannot create a full fluid bucket");
        var creative=helper.makeMockPlayer(GameType.CREATIVE);GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
        barrel=place(level,pos,"barrel_glass");use(barrel,creative,Items.WATER_BUCKET,1);
        check(creative.getMainHandItem().is(Items.WATER_BUCKET)&&barrel.fullTank(),"Creative filling retains the held fluid bucket");
        helper.succeed();
    }

    @GameTest
    public void transfersHoppersAndBreakRecovery(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,2,1));
        var barrel=place(level,pos,"barrel_stone");
        var fluid=FluidStorage.SIDED.find(level,pos,Direction.NORTH);
        check(fluid==barrel.tank,"Fabric fluid pipes resolve the barrel tank");
        try(var tx=Transaction.openOuter()){check(fluid.insert(FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET,tx)==FluidConstants.BUCKET,"simulated fluid insertion reports capacity");}
        check(barrel.tank.amount==0&&level.getBlockState(pos).getValue(BarrelBlock.CONTENTS)==BarrelBlock.Contents.EMPTY,"aborted insertion restores both fluid and displayed state");
        fill(barrel,Fluids.WATER,FluidConstants.BUCKET/2);
        try(var tx=Transaction.openOuter()){
            check(fluid.insert(FluidVariant.of(Fluids.LAVA),FluidConstants.BUCKET,tx)==0,"pipes reject incompatible fluids");
            check(fluid.insert(FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET*2,tx)==FluidConstants.BUCKET/2,"pipe insertion is capped at the remaining tank capacity");tx.commit();
        }
        try(var tx=Transaction.openOuter()){fluid.extract(FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET,tx);}
        check(barrel.fullTank(),"aborted fluid extraction cannot lose tank contents");
        try(var tx=Transaction.openOuter()){fluid.extract(FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET,tx);tx.commit();}
        check(barrel.tank.amount==0,"committed extraction empties the tank");
        check(HopperBlockEntity.getContainerAt(level,pos)==barrel,"vanilla hoppers discover the barrel container");
        var remainder=HopperBlockEntity.addItem(null,barrel,new ItemStack(Items.OAK_LEAVES,8),Direction.UP);
        check(remainder.isEmpty(),"hopper input accepts ordinary compost materials");
        step(level,pos,1007);
        check(barrel.getItem(0).is(Items.DIRT)&&barrel.getItem(1).isEmpty(),"hopper-fed compost completes without losing buffered inputs");
        level.setBlock(pos.below(),Blocks.HOPPER.defaultBlockState(),3);
        var hopper=(HopperBlockEntity)level.getBlockEntity(pos.below());
        check(HopperBlockEntity.suckInItems(level,hopper)&&hopper.countItem(Items.DIRT)==1&&barrel.getItem(0).isEmpty(),"a real bottom hopper extracts exactly one completed output");
        check(!HopperBlockEntity.suckInItems(level,hopper)&&hopper.countItem(Items.DIRT)==1,"repeated hopper extraction cannot duplicate the result");
        var itemPipe=ItemStorage.SIDED.find(level,pos,Direction.UP);
        try(var tx=Transaction.openOuter()){itemPipe.insert(ItemVariant.of(Items.WATER_BUCKET),1,tx);}
        check(barrel.getItem(1).isEmpty()&&barrel.tank.amount==0,"aborted item-pipe insertion leaves no pending bucket");
        try(var tx=Transaction.openOuter()){check(itemPipe.insert(ItemVariant.of(Items.WATER_BUCKET),1,tx)==1,"item pipes can insert a water bucket");tx.commit();}
        step(level,pos,1);
        check(barrel.fullTank()&&barrel.getItem(2).is(Items.BUCKET),"automated bucket filling retains its empty container for extraction");
        check(HopperBlockEntity.suckInItems(level,hopper)&&hopper.countItem(Items.BUCKET)==1,"bottom hoppers recover the empty bucket once");
        barrel.removeItem(0,64); // no-op on the empty output slot
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        use(barrel,player,ExNihiloOmnia.ITEMS.get("dust"),1);
        check(barrel.getItem(0).is(Items.CLAY),"automated fluid remains usable for barrel crafting");
        int before=dropped(level,pos,Items.CLAY);
        level.destroyBlock(pos,true);
        check(dropped(level,pos,Items.CLAY)==before+1&&dropped(level,pos,ExNihiloOmnia.ITEMS.get("barrel_stone"))==1,"breaking a ready barrel drops its shell and finished output exactly once");
        level.destroyBlock(pos,true);
        check(dropped(level,pos,Items.CLAY)==before+1,"repeated removal does not redrop contents");
        helper.succeed();
    }

    @GameTest(skyAccess=true)
    public void rainAndColdBiomes(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));
        var chunk=level.getChunkAt(pos);var original=level.getBiome(pos);
        var plains=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        for(var section:chunk.getSections())section.fillBiomesFromNoise((x,y,z)->plains,0,0,0);
        level.setRainLevel(1F);level.getWeatherData().setRaining(true);
        try{
            var barrel=place(level,pos,"barrel_glass");
            level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
            step(level,pos,100);
            check(barrel.tank.amount==0,"a roof prevents rain collection");
            level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
            check(level.isRainingAt(pos.above()),"test barrel has exposed rainy sky");
            step(level,pos,1000);
            check(barrel.fullTank()&&barrel.fluid()==Fluids.WATER,"exposed rain fills one bucket at the original 1 mB per tick");
            step(level,pos,100);
            check(barrel.tank.amount==FluidConstants.BUCKET,"rain cannot overfill the tank");
            barrel=place(level,pos,"barrel_stone");fill(barrel,Fluids.LAVA,FluidConstants.BUCKET);step(level,pos,100);
            check(barrel.fluid()==Fluids.LAVA&&barrel.fullTank(),"rain does not replace or dilute lava");
            level.setRainLevel(0F);level.getWeatherData().setRaining(false);
            var snowy=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.SNOWY_PLAINS);
            for(var section:chunk.getSections())section.fillBiomesFromNoise((x,y,z)->snowy,0,0,0);
            // Centre the freezing check in its chunk to avoid biome blending into
            // a neighbour chunk at the structure's edge.
            var coldPos=new BlockPos((pos.getX()>>4)*16+8,pos.getY(),(pos.getZ()>>4)*16+8);
            var coldBarrel=place(level,coldPos,"barrel_stone");fill(coldBarrel,Fluids.WATER,FluidConstants.BUCKET);
            check(level.getBiome(coldPos).value().coldEnoughToSnow(coldPos,level.getSeaLevel()),"test barrel is in a freezing biome");
            level.getRandom().setSeed(42);
            for(int i=0;i<20000&&coldBarrel.getItem(0).isEmpty();i++)step(level,coldPos,1);
            check(coldBarrel.getItem(0).is(Items.ICE)&&coldBarrel.tank.amount==0,"a full water barrel can freeze into one ice block in cold biomes");
        }finally{
            level.setRainLevel(0F);level.getWeatherData().setRaining(false);
            for(var section:chunk.getSections())section.fillBiomesFromNoise((x,y,z)->original,0,0,0);
        }
        helper.succeed();
    }

    @GameTest
    public void lavaReactionsHeatAndSave(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var barrel=place(level,pos,"barrel_stone");use(barrel,player,Items.LAVA_BUCKET,1);
        level.setBlock(pos.above(),Blocks.WATER.defaultBlockState(),3);step(level,pos,1);
        check(barrel.getItem(0).is(Items.OBSIDIAN)&&level.getBlockState(pos.above()).is(Blocks.WATER),"water above a full lava barrel makes obsidian without consuming the overhead source");
        level.setBlock(pos.above(),Blocks.LAVA.defaultBlockState(),3);barrel=place(level,pos,"barrel_stone");use(barrel,player,Items.WATER_BUCKET,1);step(level,pos,1);
        check(barrel.getItem(0).is(Items.STONE),"source lava above a water barrel produces stone");
        level.setBlock(pos.above(),Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,4),3);
        barrel=place(level,pos,"barrel_stone");use(barrel,player,Items.WATER_BUCKET,1);step(level,pos,1);
        check(barrel.getItem(0).is(Items.COBBLESTONE),"flowing lava above a water barrel produces cobblestone");
        level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
        barrel=place(level,pos,"barrel_wood");use(barrel,player,Items.LAVA_BUCKET,1);step(level,pos,200);
        var tag=barrel.saveWithFullMetadata(level.registryAccess());
        var restored=(BarrelEntity)BlockEntity.loadStatic(pos,level.getBlockState(pos),tag,level.registryAccess());
        check(restored!=null&&restored.fullTank()&&restored.fluid()==Fluids.LAVA&&restored.hotTicks()==200,"lava and the partially elapsed wood-burning timer survive save/reload");
        level.setBlockEntity(restored);step(level,pos,199);
        check(level.getBlockEntity(pos)==restored,"a wooden barrel survives until the 400-tick hot-fluid limit");
        step(level,pos,1);
        check(level.getBlockState(pos).is(Blocks.LAVA),"a full wooden lava barrel burns away after 400 ticks");
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        barrel=place(level,pos,"barrel_wood");fill(barrel,Fluids.LAVA,FluidConstants.BUCKET/2);step(level,pos,400);
        check(level.getBlockEntity(pos)==barrel&&barrel.tank.amount==FluidConstants.BUCKET/2&&level.getBlockState(pos.above()).is(Blocks.FIRE),"a partial lava tank can ignite fire but never manufactures a full lava source");
        level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
        for(String material:new String[]{"barrel_stone","barrel_glass","barrel_glass_blue","barrel_crimson","barrel_warped"}){
            barrel=place(level,pos,material);use(barrel,player,Items.LAVA_BUCKET,1);step(level,pos,500);
            check(level.getBlockEntity(pos)==barrel&&barrel.fullTank(),material+" safely holds lava");
        }
        helper.succeed();
    }

    @GameTest
    public void milkSlimeAndCrookRelease(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var difficulty=level.getDifficulty();level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        try{
            var barrel=place(level,pos,"barrel_stone");use(barrel,player,Items.WATER_BUCKET,1);use(barrel,player,Items.MILK_BUCKET,1);
            check(barrel.slimeTicks()==0&&barrel.tank.amount==0&&player.getMainHandItem().is(Items.BUCKET),"milk added to a full water barrel begins slime growth and returns its bucket");
            step(level,pos,1000);
            check(barrel.slimeTicks()==1000,"slime growth completes after the original 1000 ticks");
            level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
            use(barrel,player,ExNihiloOmnia.ITEMS.get("crook_wood"),1);
            check(barrel.slimeTicks()==1000&&player.getMainHandItem().getDamageValue()==0,"a blocked barrel cannot release a slime or damage the crook");
            level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
            barrel.interact(player,InteractionHand.MAIN_HAND);
            var slimes=level.getEntities(net.minecraft.world.entity.EntityTypes.SLIME,new AABB(pos).inflate(2),e->true);
            check(slimes.size()==1&&slimes.getFirst().getHealth()==1F&&barrel.slimeTicks()==-1&&player.getMainHandItem().getDamageValue()==1,"a crook releases one low-health slime and resets the barrel once");
            barrel.interact(player,InteractionHand.MAIN_HAND);
            check(level.getEntities(net.minecraft.world.entity.EntityTypes.SLIME,new AABB(pos).inflate(2),e->true).size()==1,"repeated crook clicks cannot duplicate the slime");
            slimes.forEach(net.minecraft.world.entity.Entity::discard);
        }finally{level.getServer().setDifficulty(difficulty,true);}
        helper.succeed();
    }
}
