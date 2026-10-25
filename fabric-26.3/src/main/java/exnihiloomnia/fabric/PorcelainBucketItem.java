package exnihiloomnia.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Vanilla placement/evaporation/waterlogging with porcelain container results. */
public final class PorcelainBucketItem extends BucketItem {
    public PorcelainBucketItem(Fluid fluid,Properties properties){super(fluid,properties);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        return useAt(level,player,hand,getPlayerPOVHitResult(level,player,getFluidContext()));
    }
    InteractionResult useAt(Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isSpectator()||hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;
        ItemStack held=player.getItemInHand(hand);
        BlockPos pos=hit.getBlockPos(),adjacent=pos.relative(hit.getDirection());
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(adjacent,hit.getDirection(),held))return InteractionResult.FAIL;
        var state=level.getBlockState(pos);
        if(content==Fluids.EMPTY){
            var fluid=state.getFluidState();
            if(!fluid.isSource()||(fluid.getType()!=Fluids.WATER&&fluid.getType()!=Fluids.LAVA&&fluid.getType()!=ExNihiloOmnia.WITCHWATER)
                    ||!(state.getBlock() instanceof BucketPickup pickup))return InteractionResult.FAIL;
            if(!level.isClientSide()){
                var picked=pickup.pickupBlock(player,level,pos,state);
                if(picked.isEmpty())return InteractionResult.FAIL;
                pickup.getPickupSound().ifPresent(sound->player.playSound(sound,1,1));
                level.gameEvent(player,GameEvent.FLUID_PICKUP,pos);
                player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(held,player,BucketSupport.filled(held,fluid.getType())));
            }
            return InteractionResult.SUCCESS;
        }
        if(level.isClientSide())return InteractionResult.SUCCESS;
        BlockPos target=content==Fluids.WATER&&state.getBlock() instanceof LiquidBlockContainer?pos:adjacent;
        if(!emptyContents(player,level,target,hit))return InteractionResult.FAIL;
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS.heldItemTransformedTo(player.hasInfiniteMaterials()?held:ItemUtils.createFilledResult(held,player,BucketSupport.emptied(held)));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
        String type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if(content!=Fluids.EMPTY||entity.isBaby()||(!type.equals("minecraft:cow")&&!type.equals("minecraft:mooshroom")))return InteractionResult.PASS;
        if(!player.level().isClientSide()){
            player.level().playSound(null,entity.blockPosition(),SoundEvents.COW_MILK,SoundSource.PLAYERS,1,1);
            player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(ExNihiloOmnia.ITEMS.get("bucket_porcelain_milk"))));
        }
        return InteractionResult.SUCCESS;
    }
}
