package exnihiloomnia.fabric;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

public final class SporesItem extends Item {
    public SporesItem(Properties properties){super(properties);}
    public InteractionResult useOn(UseOnContext c){
        var player=c.getPlayer();var level=c.getLevel();var pos=c.getClickedPos();var held=c.getItemInHand();
        if(player==null||player.isSpectator()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,c.getClickedFace(),held))return InteractionResult.PASS;
        if(level.getBlockState(pos).is(Blocks.DIRT)){
            if(level instanceof ServerLevel){level.setBlock(pos,Blocks.MYCELIUM.defaultBlockState(),3);held.consume(1,player);}return InteractionResult.SUCCESS;
        }
        var target=pos.relative(c.getClickedFace());
        var mushroom=Blocks.RED_MUSHROOM.defaultBlockState();
        if(!level.getBlockState(target).canBeReplaced()||!mushroom.canSurvive(level,target)||!player.mayUseItemAt(target,c.getClickedFace(),held))return InteractionResult.PASS;
        if(level instanceof ServerLevel){level.setBlock(target,(level.getRandom().nextBoolean()?Blocks.RED_MUSHROOM:Blocks.BROWN_MUSHROOM).defaultBlockState(),3);held.consume(1,player);}
        return InteractionResult.SUCCESS;
    }
    public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(player.isSpectator()||target.getType()!=EntityTypes.COW||!target.isAlive())return InteractionResult.PASS;
        if(player.level() instanceof ServerLevel){
            var cow=(Mob)target;
            var converted=cow.convertTo(EntityTypes.MOOSHROOM,ConversionParams.single(cow,true,true),EntitySpawnReason.CONVERSION,m->{m.setBaby(cow.isBaby());m.setHealth(Math.min(m.getMaxHealth(),cow.getHealth()));});
            if(converted!=null)stack.consume(1,player);
        }
        return InteractionResult.SUCCESS;
    }
}
