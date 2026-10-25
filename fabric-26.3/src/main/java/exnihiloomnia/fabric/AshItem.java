package exnihiloomnia.fabric;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BonemealSource;

public final class AshItem extends Item {
    public AshItem(Properties p){super(p);}
    public InteractionResult useOn(UseOnContext c){
        var player=c.getPlayer();var level=c.getLevel();var pos=c.getClickedPos();var state=level.getBlockState(pos);
        if(player==null||player.isSpectator()||!player.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand()))return InteractionResult.PASS;
        if(!(state.getBlock() instanceof BonemealableBlock growable)||!growable.isValidBonemealTarget(level,pos,state,BonemealSource.INTERACTION))return InteractionResult.PASS;
        if(level instanceof ServerLevel server){
            if(growable.isBonemealSuccess(level,level.getRandom(),pos,state,BonemealSource.INTERACTION)){
                level.levelEvent(2005,pos,0);
                if(level.getRandom().nextInt(5)==0)growable.performBonemeal(server,level.getRandom(),pos,state,BonemealSource.INTERACTION);
            }
            c.getItemInHand().consume(1,player);
        }
        return InteractionResult.SUCCESS;
    }
}
