package exnihiloomnia.fabric;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class SilkwormItem extends Item {
    public SilkwormItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || player.isSpectator() || !player.mayUseItemAt(context.getClickedPos(),
                context.getClickedFace(), context.getItemInHand())) return InteractionResult.PASS;
        if (!InfestedLeavesBlock.canInfest(context.getLevel().getBlockState(context.getClickedPos()))) return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel server && InfestedLeavesBlock.infest(server, context.getClickedPos(), 0, true)) {
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
