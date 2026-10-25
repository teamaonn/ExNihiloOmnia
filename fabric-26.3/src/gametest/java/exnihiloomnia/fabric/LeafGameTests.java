package exnihiloomnia.fabric;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class LeafGameTests {
    private static void check(boolean result, String message) {
        if (!result) throw new IllegalStateException(message);
        System.out.println("EX NIHILO LEAVES PASS: " + message);
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false); }
    private static void step(ServerLevel level, BlockPos pos, int ticks) {
        for (int i=0;i<ticks;i++) InfestedLeavesEntity.tick(level,pos,level.getBlockState(pos),(InfestedLeavesEntity)level.getBlockEntity(pos));
    }
    private static int items(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream()
                .filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }

    @GameTest
    public void infestationSpeciesConsumptionAndCreative(GameTestHelper helper) {
        var level=helper.getLevel();
        var pos=helper.absolutePos(new BlockPos(1,1,1));
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var worm=new ItemStack(ExNihiloOmnia.ITEMS.get("silkworm"),16);
        player.setItemInHand(InteractionHand.MAIN_HAND,worm);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit(pos));
        for(var entry:ExNihiloOmnia.INFESTABLE.entrySet()) {
            var source=entry.getKey().defaultBlockState().setValue(LeavesBlock.PERSISTENT,true)
                    .setValue(LeavesBlock.DISTANCE,2).setValue(LeavesBlock.WATERLOGGED,true);
            level.setBlock(pos,source,3);
            int before=worm.getCount();
            worm.getItem().useOn(context);
            var result=level.getBlockState(pos);
            check(result.is(entry.getValue()) && worm.getCount()==before-1,"one worm infests "+entry.getKey()+" and preserves its species");
            check(result.getValue(LeavesBlock.WATERLOGGED) && result.getValue(LeavesBlock.PERSISTENT)
                    && result.getValue(LeavesBlock.DISTANCE)==2,"infestation preserves waterlogging, permanence and leaf distance");
            worm.getItem().useOn(context);
            check(worm.getCount()==before-1,"already-infested leaves do not consume another worm");
        }
        level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        int before=worm.getCount();worm.getItem().useOn(context);
        check(worm.getCount()==before && level.getBlockState(pos).is(Blocks.STONE),"stone rejects infestation without consuming a worm");
        var creative=helper.makeMockPlayer(GameType.CREATIVE);
        GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
        var creativeWorm=new ItemStack(ExNihiloOmnia.ITEMS.get("silkworm"));
        creative.setItemInHand(InteractionHand.MAIN_HAND,creativeWorm);
        level.setBlock(pos,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3);
        creativeWorm.getItem().useOn(new UseOnContext(creative,InteractionHand.MAIN_HAND,hit(pos)));
        check(creativeWorm.getCount()==1 && level.getBlockEntity(pos) instanceof InfestedLeavesEntity,"Creative infestation preserves the held worm");
        helper.succeed();
    }

    @GameTest
    public void growthSpreadAndReload(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));
        level.setBlock(pos,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3);
        InfestedLeavesBlock.infest(level,pos,0,false);
        step(level,pos,1200);
        check(((InfestedLeavesEntity)level.getBlockEntity(pos)).growthTicks()==1200
                && level.getBlockState(pos).getValue(InfestedLeavesBlock.INFESTATION)==2,"growth reaches 60 percent after exactly 1200 loaded ticks");
        step(level,pos,50);
        var leaves=(InfestedLeavesEntity)level.getBlockEntity(pos);
        var saved=leaves.saveWithoutMetadata(level.registryAccess());
        var restored=new InfestedLeavesEntity(pos,level.getBlockState(pos));
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),saved));
        check(restored.growthTicks()==1250 && restored.saveWithoutMetadata(level.registryAccess()).equals(saved),"save/reload preserves exact growth and the partially elapsed spread timer");
        level.setBlockEntity(restored);
        step(level,pos,750);
        check(restored.growthTicks()==2000 && level.getBlockState(pos).getValue(InfestedLeavesBlock.INFESTATION)==4,"leaves mature at 2000 loaded ticks and retain their block entity through color changes");
        // All possible neighbours are eligible, so a random attempt can spread
        // without depending on terrain or a particular direction.
        for(BlockPos neighbour:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))) {
            if(!neighbour.equals(pos))level.setBlock(neighbour,Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3);
        }
        level.getRandom().setSeed(12345);
        step(level,pos,1000);
        int spread=0;
        for(BlockPos neighbour:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))) {
            if(!neighbour.equals(pos) && level.getBlockState(neighbour).getBlock() instanceof InfestedLeavesBlock){
                spread++;check(level.getBlockState(neighbour).is(ExNihiloOmnia.INFESTABLE.get(Blocks.BIRCH_LEAVES)),"spreading retains the neighbouring leaf's species");
            }
        }
        check(spread>0,"mature leaves spread to surrounding ordinary leaves");
        BlockPos unknown=pos.offset(100000,0,100000);
        check(!level.hasChunkAt(unknown) && !InfestedLeavesBlock.infest(level,unknown,0,false)
                && !level.hasChunkAt(unknown),"infestation rejects unknown chunks without loading them");
        helper.succeed();
    }

    @GameTest
    public void harvestingShearsAndDecay(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var block=ExNihiloOmnia.INFESTABLE.get(Blocks.BIRCH_LEAVES);
        var state=block.defaultBlockState().setValue(InfestedLeavesBlock.INFESTATION,4).setValue(LeavesBlock.PERSISTENT,true);
        level.setBlock(pos,state,3);
        var sheared=Block.getDrops(state,level,pos,level.getBlockEntity(pos),player,new ItemStack(Items.SHEARS));
        check(sheared.size()==1 && sheared.getFirst().is(block.asItem()) && sheared.getFirst().getCount()==1,"shears recover the same infested leaf species without string duplication");
        var silk=new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        var silkDrops=Block.getDrops(state,level,pos,level.getBlockEntity(pos),player,silk);
        check(silkDrops.size()==1 && silkDrops.getFirst().is(block.asItem()),"Silk Touch preserves the infested leaf block");
        int strings=0;
        level.getRandom().setSeed(89);
        for(int i=0;i<2000;i++)for(var drop:Block.getDrops(state,level,pos,level.getBlockEntity(pos),player,ItemStack.EMPTY)) {
            if (!drop.is(Items.STRING)) throw new IllegalStateException("Infested leaves dropped a non-string item");
            strings+=drop.getCount();
        }
        check(strings>2200 && strings<2800,"mature string rewards retain the original average yield");
        check(state.is(BlockTags.LEAVES),"infested variants participate in vanilla leaf tags");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_PLANKS));
        check(block.useWithoutItem(state,level,pos,player,hit(pos))==net.minecraft.world.InteractionResult.PASS,
                "growth reminders do not block using held items against leaves");
        var fire=net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().get(block);
        check(fire!=null && fire.getIgniteOdds()==30 && fire.getBurnOdds()==60,
                "infested leaves retain vanilla leaf flammability");
        var natural=state.setValue(LeavesBlock.PERSISTENT,false).setValue(LeavesBlock.DISTANCE,7).setValue(LeavesBlock.WATERLOGGED,true);
        level.setBlock(pos,natural,3);
        block.randomTick(natural,level,pos,level.getRandom());
        check(level.getBlockState(pos).is(Blocks.WATER),"unsupported natural infested leaves decay and restore their contained water");
        level.setBlock(pos,state,3);block.randomTick(state,level,pos,level.getRandom());
        check(level.getBlockState(pos).is(block),"persistent player-placed infested leaves do not decay");
        helper.succeed();
    }

    @GameTest
    public void crookHarvestAndEntityInteraction(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(1,1,1));
        // The legacy helper makeMockServerPlayerInLevel hard-codes Creative.
        // Use a Survival mock with an embedded connection for real game-mode breaks.
        var player=(net.minecraft.server.level.ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false);
        level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(pos.west(2)));
        var crook=new ItemStack(ExNihiloOmnia.ITEMS.get("crook_wood"));
        crook.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        player.setItemInHand(InteractionHand.MAIN_HAND,crook);
        var leaf=Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
        level.setBlock(pos,leaf,3);
        int before=items(level,pos,Items.OAK_LEAVES);
        check(player.gameMode.destroyBlock(pos),"crook uses a successful vanilla Survival block break");
        check(items(level,pos,Items.OAK_LEAVES)-before==2 && crook.getDamageValue()==1,"crook adds a second normal leaf loot roll while charging one durability");
        crook.setDamageValue(crook.getMaxDamage()-1);level.setBlock(pos,leaf,3);before=items(level,pos,Items.OAK_LEAVES);
        player.gameMode.destroyBlock(pos);
        check(crook.isEmpty() && items(level,pos,Items.OAK_LEAVES)-before==2,"the crook's last durability still supplies its enhanced leaf drops");
        var bone=new ItemStack(ExNihiloOmnia.ITEMS.get("crook_bone"));
        check(bone.getMaxDamage()==196 && bone.getDestroySpeed(leaf)==5F,"bone crook retains its original durability and faster leaf mining");
        player.setItemInHand(InteractionHand.MAIN_HAND,bone);
        var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);cow.setNoAi(true);
        cow.setPos(player.getX()+2,player.getY(),player.getZ());level.addFreshEntity(cow);
        float health=cow.getHealth();
        AttackEntityCallback.EVENT.invoker().interact(player,level,InteractionHand.MAIN_HAND,cow,null);
        check(cow.getDeltaMovement().x>0 && cow.getHealth()==health,"left-clicking with a crook pushes a creature without damage");
        cow.setDeltaMovement(Vec3.ZERO);
        UseEntityCallback.EVENT.invoker().interact(player,level,InteractionHand.MAIN_HAND,cow,null);
        check(cow.getDeltaMovement().x<0 && bone.getDamageValue()==2,"right-click pulls a creature and each interaction costs one durability");
        cow.setPos(player.position());CrookItem.moveEntity(player,cow,true);
        check(Double.isFinite(cow.getDeltaMovement().x) && Double.isFinite(cow.getDeltaMovement().z),"overlapping positions cannot cause invalid crook velocity");
        cow.discard();
        // Seeded real breaks exercise the rare worm reward through the full item
        // mining path. Reset durability only to avoid crafting hundreds of crooks.
        var harvestCrook=new ItemStack(ExNihiloOmnia.ITEMS.get("crook_bone"));
        player.setItemInHand(InteractionHand.MAIN_HAND,harvestCrook);level.getRandom().setSeed(23);
        for(int i=0;i<512;i++){harvestCrook.setDamageValue(0);level.setBlock(pos,leaf,3);player.gameMode.destroyBlock(pos);}
        check(items(level,pos,ExNihiloOmnia.ITEMS.get("silkworm"))>0,"ordinary leaves harvested with a crook can supply silkworms");
        var foodInput=new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(ExNihiloOmnia.ITEMS.get("silkworm")));
        var cooked=level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,foodInput,level).orElseThrow().value().assemble(foodInput);
        check(cooked.is(ExNihiloOmnia.ITEMS.get("cooked_silkworm"))
                && cooked.get(net.minecraft.core.component.DataComponents.FOOD).nutrition()==3,
                "silkworms smelt into the original three-nutrition cooked food");
        player.discard();helper.succeed();
    }
}
