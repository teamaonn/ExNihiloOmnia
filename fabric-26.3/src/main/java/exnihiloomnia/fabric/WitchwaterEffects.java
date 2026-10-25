package exnihiloomnia.fabric;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;

public final class WitchwaterEffects {
    private static final java.util.Map<Entity,Long> ANIMAL_LIGHTNING=new java.util.WeakHashMap<>();
    public static void touch(ServerLevel level,Entity entity){
        if(!entity.isAlive()||entity.isRemoved())return;
        if(entity instanceof Player player){
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,210));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,210,2));
            player.addEffect(new MobEffectInstance(MobEffects.WITHER,210));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,210));return;
        }
        if(!(entity instanceof Mob mob))return;
        String type=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        boolean hostileAllowed=level.getDifficulty()!=Difficulty.PEACEFUL;
        switch(type){
            case "villager" -> {
                if(!hostileAllowed)return;
                var villager=(Villager)mob;
                if(villager.isBaby())mob.convertTo(EntityTypes.ZOMBIE_VILLAGER,ConversionParams.single(mob,true,true),EntitySpawnReason.CONVERSION,z->{z.setBaby(true);z.setVillagerData(villager.getVillagerData());z.setHealth(Math.min(z.getMaxHealth(),villager.getHealth()));});
                else mob.convertTo(EntityTypes.WITCH,ConversionParams.single(mob,true,true),EntitySpawnReason.CONVERSION,w->w.setHealth(Math.min(w.getMaxHealth(),mob.getHealth())));
            }
            case "skeleton" -> {if(hostileAllowed)mob.convertTo(EntityTypes.WITHER_SKELETON,ConversionParams.single(mob,true,true),EntitySpawnReason.CONVERSION,w->w.setHealth(w.getMaxHealth()));}
            case "spider" -> {if(hostileAllowed)mob.convertTo(EntityTypes.CAVE_SPIDER,ConversionParams.single(mob,true,true),EntitySpawnReason.CONVERSION,s->s.setHealth(s.getMaxHealth()));}
            case "squid" -> {if(hostileAllowed)mob.convertTo(EntityTypes.GHAST,ConversionParams.single(mob,true,true),EntitySpawnReason.CONVERSION,g->{g.setPos(mob.getX(),mob.getY()+2,mob.getZ());g.setHealth(g.getMaxHealth());});}
            case "creeper" -> {
                var creeper=(net.minecraft.world.entity.monster.Creeper)mob;
                if(!creeper.isPowered()){
                    var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
                    if(bolt!=null){creeper.thunderHit(level,bolt);creeper.setHealth(creeper.getMaxHealth());}
                }
            }
            case "pig" -> {if(hostileAllowed){var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);if(bolt!=null)mob.thunderHit(level,bolt);}}
            default -> {
                if(mob instanceof net.minecraft.world.entity.animal.Animal){
                    long now=level.getGameTime();
                    if(now-ANIMAL_LIGHTNING.getOrDefault(mob,now-20)>=20){
                        ANIMAL_LIGHTNING.put(mob,now);
                        var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
                        if(bolt!=null)mob.thunderHit(level,bolt);
                    }
                }
            }
        }
    }
    private WitchwaterEffects(){}
}
