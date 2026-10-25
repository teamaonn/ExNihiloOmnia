package exnihiloomnia.fabric;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

public final class WitchwaterGameTests {
    private static final long B=FluidConstants.BUCKET;
    private static Item item(String name){return ExNihiloOmnia.ITEMS.get(name);}
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);System.out.println("WITCHWATER PASS: "+why);}
    private static BarrelEntity place(ServerLevel l,BlockPos p,String name){l.setBlock(p,Blocks.AIR.defaultBlockState(),3);l.setBlock(p,ExNihiloOmnia.BARRELS.get(name).defaultBlockState(),3);return (BarrelEntity)l.getBlockEntity(p);}
    private static void step(ServerLevel l,BlockPos p,int ticks){for(int i=0;i<ticks;i++)BarrelEntity.tick(l,p,l.getBlockState(p),(BarrelEntity)l.getBlockEntity(p));}
    private static void use(BarrelEntity b,Player p,Item i,int count){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(i,count));b.interact(p,InteractionHand.MAIN_HAND);}
    private static void apply(Player p,InteractionResult r){if(r instanceof InteractionResult.Success s&&s.heldItemTransformedTo()!=null)p.setItemInHand(InteractionHand.MAIN_HAND,s.heldItemTransformedTo());}
    private static void fill(BarrelEntity b,long amount){try(var tx=Transaction.openOuter()){check(b.tank.insert(FluidVariant.of(ExNihiloOmnia.WITCHWATER),amount,tx)==amount,"Witchwater fluid transfer accepts compatible tank");tx.commit();}}

    @GameTest public void sporesNaturalFermentationAndReload(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,1,1));var player=h.makeMockPlayer(GameType.SURVIVAL);
        var b=place(l,p,"barrel_stone");use(b,player,Items.WATER_BUCKET,1);use(b,player,item("spores"),2);
        check(b.witchTicks()==0&&player.getMainHandItem().getCount()==1,"one spore starts Witchwater fermentation in a full water barrel");
        try(var tx=Transaction.openOuter()){
            check(b.tank.extract(FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),B,tx)==0,"fermenting tanks reject fluid extraction");
            check(b.tank.insert(FluidVariant.of(ExNihiloOmnia.WITCHWATER),B,tx)==0,"fermenting tanks reject fluid insertion");
        }
        step(l,p,500);var tag=b.saveWithFullMetadata(l.registryAccess());
        var restored=(BarrelEntity)BlockEntity.loadStatic(p,l.getBlockState(p),tag,l.registryAccess());
        check(restored!=null&&restored.witchTicks()==500&&restored.fullTank(),"fermentation progress and fluid survive save/reload");
        l.setBlockEntity(restored);step(l,p,499);check(restored.fluid()==net.minecraft.world.level.material.Fluids.WATER,"Witchwater is not ready early");step(l,p,1);
        check(restored.fluid()==ExNihiloOmnia.WITCHWATER&&restored.witchTicks()==-1,"Witchwater completes after exactly 1000 loaded ticks");
        use(restored,player,item("bucket_porcelain_empty"),1);
        check(player.getMainHandItem().is(item("bucket_porcelain_witchwater"))&&restored.tank.amount==0,"porcelain bucket collects Witchwater");
        b=place(l,p,"barrel_wood");l.setBlock(p.below(),Blocks.MYCELIUM.defaultBlockState(),3);use(b,player,Items.WATER_BUCKET,1);
        l.getRandom().setSeed(42);for(int i=0;i<30000&&b.witchTicks()<0;i++)step(l,p,1);
        check(b.witchTicks()>=0,"full wooden water barrel above mycelium naturally starts fermentation");
        b=place(l,p,"barrel_stone");use(b,player,Items.WATER_BUCKET,1);step(l,p,3000);
        check(b.witchTicks()==-1,"stone barrel does not naturally ferment from nearby mycelium");
        h.succeed();
    }
    @GameTest public void witchwaterRecipesAndTransactionalContainers(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,1,1));var player=h.makeMockPlayer(GameType.SURVIVAL);
        var b=place(l,p,"barrel_stone");fill(b,B/2);use(b,player,Items.SAND,1);
        check(b.getItem(0).isEmpty()&&player.getMainHandItem().is(Items.SAND),"partial Witchwater cannot craft soul sand");fill(b,B/2);
        use(b,player,Items.SAND,2);check(b.getItem(0).is(Items.SOUL_SAND)&&player.getMainHandItem().getCount()==1&&b.tank.amount==0,"one sand plus one Witchwater bucket yields one soul sand");
        use(b,player,Items.AIR,0);use(b,player,item("bucket_witchwater"),1);use(b,player,Items.GOLDEN_APPLE,2);
        check(b.getItem(0).is(Items.CHORUS_FRUIT)&&b.tank.amount==0&&player.getMainHandItem().getCount()==1,"Witchwater plus one golden apple yields one chorus fruit");
        var context=ContainerItemContext.ofPlayerHand(player,InteractionHand.MAIN_HAND);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item("bucket_porcelain_empty")));
        var storage=context.find(FluidStorage.ITEM);
        try(var tx=Transaction.openOuter()){check(storage.insert(FluidVariant.of(ExNihiloOmnia.WITCHWATER),B,tx)==B,"porcelain item storage can fill Witchwater");}
        check(player.getMainHandItem().is(item("bucket_porcelain_empty")),"aborted Witchwater bucket fill rolls back");
        try(var tx=Transaction.openOuter()){storage.insert(FluidVariant.of(ExNihiloOmnia.WITCHWATER),B,tx);tx.commit();}
        check(player.getMainHandItem().is(item("bucket_porcelain_witchwater")),"committed Witchwater fill preserves porcelain container");
        storage=context.find(FluidStorage.ITEM);try(var tx=Transaction.openOuter()){storage.extract(FluidVariant.of(ExNihiloOmnia.WITCHWATER),B,tx);tx.commit();}
        check(player.getMainHandItem().is(item("bucket_porcelain_empty")),"draining Witchwater returns porcelain empty bucket");
        h.succeed();
    }
    @GameTest public void dollRecipesTimedSummonsAndBlockedSpace(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,1,1));var player=h.makeMockPlayer(GameType.SURVIVAL);var difficulty=l.getDifficulty();l.getServer().setDifficulty(Difficulty.NORMAL,true);
        try{
            var recipes=l.getServer().getRecipeManager();
            for(String gem:List.of("diamond","emerald")){
                var x=new ItemStack(item("porcelain"));var g=new ItemStack(gem.equals("diamond")?Items.DIAMOND:Items.EMERALD);
                var input=CraftingInput.of(3,3,List.of(x,g,x,ItemStack.EMPTY,x,ItemStack.EMPTY,x,ItemStack.EMPTY,x));
                var r=(CraftingRecipe)recipes.byKey(ResourceKey.create(Registries.RECIPE,ExNihiloOmnia.id("doll_"+gem))).orElseThrow().value();
                check(r.matches(input,l)&&r.assemble(input).is(item("doll")),"porcelain doll recipe accepts "+gem);
            }
            for(boolean blaze:new boolean[]{true,false}){
                String name=blaze?"doll_blaze":"doll_ender";
                Item powder=blaze?Items.BLAZE_POWDER:Items.INK_SAC,side=blaze?Items.GLOWSTONE_DUST:Items.LAPIS_LAZULI;
                var a=new ItemStack(powder);var input=CraftingInput.of(3,3,List.of(a,new ItemStack(Items.NETHER_WART),a,new ItemStack(side),new ItemStack(item("doll")),new ItemStack(side),a,new ItemStack(Items.REDSTONE),a));
                var recipe=(CraftingRecipe)recipes.byKey(ResourceKey.create(Registries.RECIPE,ExNihiloOmnia.id(name))).orElseThrow().value();
                check(recipe.matches(input,l)&&recipe.assemble(input).is(item(name)),"original crafting recipe produces "+name);
                var b=place(l,p,"barrel_stone");use(b,player,blaze?Items.LAVA_BUCKET:item("bucket_witchwater"),1);use(b,player,item(name),2);
                check(b.summonTicks()==0&&player.getMainHandItem().getCount()==1,"summoning consumes exactly one doll");
                try(var tx=Transaction.openOuter()){check(b.tank.extract(b.tank.variant,B,tx)==0,"summoning locks its fluid until completion");}
                step(l,p,500);var restored=(BarrelEntity)BlockEntity.loadStatic(p,l.getBlockState(p),b.saveWithFullMetadata(l.registryAccess()),l.registryAccess());
                check(restored!=null&&restored.summonTicks()==500,"summon progress survives save/reload");l.setBlockEntity(restored);
                l.setBlock(p.above(),Blocks.STONE.defaultBlockState(),3);step(l,p,500);
                check(restored.summonTicks()==1000,"blocked space retains a ready summon");
                l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),3);
                l.getServer().setDifficulty(Difficulty.PEACEFUL,true);step(l,p,10);check(restored.summonTicks()==1000,"Peaceful difficulty waits without losing a ready summon");l.getServer().setDifficulty(Difficulty.NORMAL,true);
                step(l,p,1);check(restored.summonTicks()==-1&&restored.tank.amount==0,"clear space releases a completed summon and empties fluid");
                var type=blaze?EntityTypes.BLAZE:EntityTypes.ENDERMAN;
                var mobs=l.getEntitiesOfClass(Mob.class,new AABB(p).inflate(4),e->e.getType()==type);
                check(mobs.size()==1,"exactly one summoned mob appears");step(l,p,20);
                check(l.getEntitiesOfClass(Mob.class,new AABB(p).inflate(4),e->e.getType()==type).size()==1,"repeated ticks cannot duplicate summons");mobs.forEach(Entity::discard);
            }
        }finally{l.getServer().setDifficulty(difficulty,true);}h.succeed();
    }
    @GameTest public void witchwaterWorldFlowAndBuckets(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,2,1));var player=h.makeMockPlayer(GameType.SURVIVAL);
        l.setBlock(p,ExNihiloOmnia.WITCHWATER_BLOCK.defaultBlockState(),3);
        check(l.getFluidState(p).isSource()&&l.getFluidState(p).getType()==ExNihiloOmnia.WITCHWATER,"placed Witchwater exposes its source fluid");
        ExNihiloOmnia.WITCHWATER.tick(l,p,l.getBlockState(p),l.getFluidState(p));
        check(ExNihiloOmnia.WITCHWATER.isSame(l.getFluidState(p.below()).getType()),"Witchwater flows down as its own fluid");
        var empty=(PorcelainBucketItem)item("bucket_porcelain_empty");player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(empty));
        apply(player,empty.useAt(l,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));
        check(player.getMainHandItem().is(item("bucket_porcelain_witchwater"))&&l.getFluidState(p).isEmpty(),"world Witchwater pickup preserves porcelain bucket");
        l.setBlock(p,Blocks.STONE.defaultBlockState(),3);var full=(PorcelainBucketItem)player.getMainHandItem().getItem();
        apply(player,full.useAt(l,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));
        check(player.getMainHandItem().is(empty)&&l.getFluidState(p.above()).getType()==ExNihiloOmnia.WITCHWATER,"porcelain Witchwater bucket places its fluid and returns empty");
        h.succeed();
    }
    @GameTest public void sporesDirtCowsAndSieveProgression(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,1,1));var player=h.makeMockPlayer(GameType.SURVIVAL);var spores=item("spores");
        l.setBlock(p,Blocks.DIRT.defaultBlockState(),3);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(spores,2));
        spores.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));
        check(l.getBlockState(p).is(Blocks.MYCELIUM)&&player.getMainHandItem().getCount()==1,"one spore converts dirt to mycelium");
        spores.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));
        check((l.getBlockState(p.above()).is(Blocks.RED_MUSHROOM)||l.getBlockState(p.above()).is(Blocks.BROWN_MUSHROOM))&&player.getMainHandItem().isEmpty(),"spores plant mushrooms on suitable ground");
        var cow=EntityTypes.COW.create(l,EntitySpawnReason.TRIGGERED);cow.setPos(p.getX()+.5,p.getY()+1,p.getZ()+.5);cow.setCustomName(net.minecraft.network.chat.Component.literal("Test cow"));cow.setBaby(true);l.addFreshEntity(cow);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(spores,2));spores.interactLivingEntity(player.getMainHandItem(),player,cow,InteractionHand.MAIN_HAND);
        var moos=l.getEntities(EntityTypes.MOOSHROOM,new AABB(p).inflate(4),e->true);
        check(cow.isRemoved()&&moos.size()==1&&moos.getFirst().isBaby()&&moos.getFirst().hasCustomName()&&player.getMainHandItem().getCount()==1,"spores convert cow once and preserve name and baby age");moos.forEach(Entity::discard);
        int[] count={0};SieveDrops.roll(3,1,10000,net.minecraft.util.RandomSource.create(42),stack->{if(stack.is(spores))count[0]+=stack.getCount();});
        check(count[0]>50&&count[0]<150,"sand sieving provides spores at the original one percent chance");h.succeed();
    }
    @GameTest public void mobTransformationsAndPlayerEffects(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(1,1,1));var difficulty=l.getDifficulty();l.getServer().setDifficulty(Difficulty.NORMAL,true);
        try{
            EntityType<?>[] from={EntityTypes.SKELETON,EntityTypes.SPIDER,EntityTypes.SQUID,EntityTypes.VILLAGER,EntityTypes.PIG};
            EntityType<?>[] to={EntityTypes.WITHER_SKELETON,EntityTypes.CAVE_SPIDER,EntityTypes.GHAST,EntityTypes.WITCH,EntityTypes.ZOMBIFIED_PIGLIN};
            for(int i=0;i<from.length;i++){
                var mob=(Mob)from[i].create(l,EntitySpawnReason.TRIGGERED);mob.setPos(p.getX()+.5,p.getY()+1,p.getZ()+.5);l.addFreshEntity(mob);WitchwaterEffects.touch(l,mob);
                var target=to[i];var converted=l.getEntitiesOfClass(Mob.class,new AABB(p).inflate(6),e->e.getType()==target);
                check(mob.isRemoved()&&converted.size()==1,"Witchwater transforms "+from[i]+" into "+target);converted.forEach(Entity::discard);
            }
            var creeper=EntityTypes.CREEPER.create(l,EntitySpawnReason.TRIGGERED);WitchwaterEffects.touch(l,creeper);check(creeper.isPowered(),"Witchwater charges a creeper");
            var baby=EntityTypes.VILLAGER.create(l,EntitySpawnReason.TRIGGERED);baby.setBaby(true);baby.setPos(p.getX()+.5,p.getY()+1,p.getZ()+.5);l.addFreshEntity(baby);WitchwaterEffects.touch(l,baby);
            var zombies=l.getEntities(EntityTypes.ZOMBIE_VILLAGER,new AABB(p).inflate(4),e->true);
            check(zombies.size()==1&&zombies.getFirst().isBaby(),"baby villagers become baby zombie villagers");zombies.forEach(Entity::discard);
            var player=h.makeMockPlayer(GameType.SURVIVAL);ExNihiloOmnia.WITCHWATER.defaultFluidState().entityInside(l,p,player,net.minecraft.world.entity.InsideBlockEffectApplier.NOOP);
            check(player.hasEffect(MobEffects.BLINDNESS)&&player.hasEffect(MobEffects.WEAKNESS)&&player.getEffect(MobEffects.WEAKNESS).getAmplifier()==2&&player.hasEffect(MobEffects.WITHER)&&player.hasEffect(MobEffects.SLOWNESS),"actual fluid contact applies original four Witchwater effects");
            l.getServer().setDifficulty(Difficulty.PEACEFUL,true);var villager=EntityTypes.VILLAGER.create(l,EntitySpawnReason.TRIGGERED);WitchwaterEffects.touch(l,villager);
            check(!villager.isRemoved(),"Peaceful prevents hostile villager conversion");
        }finally{l.getServer().setDifficulty(difficulty,true);}h.succeed();
    }
}
