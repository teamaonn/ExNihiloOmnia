package exnihiloomnia.fabric;

import java.util.List;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CrookItem extends Item {
    public CrookItem(Properties properties) { super(properties); }

    public static Properties properties(boolean bone) {
        return new Properties().durability(bone ? 196 : 88).enchantable(bone ? 5 : 15)
                .repairable(bone ? Items.BONE_MEAL : Items.STICK)
                .delayedComponent(DataComponents.TOOL, lookup -> new Tool(List.of(Tool.Rule.minesAndDrops(
                        lookup.lookupOrThrow(Registries.BLOCK).getOrThrow(BlockTags.LEAVES), bone ? 5F : 3F)), 1F, 1, true));
    }

    public static boolean crookable(BlockState state) {
        return !(state.getBlock() instanceof InfestedLeavesBlock) && (state.is(BlockTags.LEAVES)
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN));
    }

    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, BlockState state,
                             net.minecraft.core.BlockPos pos, LivingEntity miner) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && miner instanceof Player player
                && !player.isCreative() && crookable(state) && !server.getBlockState(pos).is(state.getBlock())) {
            // The successful vanilla break supplies one normal loot roll. Add a
            // second before durability is charged, including on the crook's last use.
            for (ItemStack drop : Block.getDrops(state, server, pos, null, player, stack)) Block.popResource(server, pos, drop);
            if (server.getRandom().nextInt(100) <= 1) Block.popResource(server, pos, new ItemStack(ExNihiloOmnia.ITEMS.get("silkworm")));
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    public static void registerEvents() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player.isSpectator() || !(player.getItemInHand(hand).getItem() instanceof CrookItem)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                moveEntity(player, entity, false);
                player.getItemInHand(hand).hurtAndBreak(1, player, hand == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS; // Push instead of dealing melee damage.
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player.isSpectator() || !(entity instanceof LivingEntity)
                    || !(player.getItemInHand(hand).getItem() instanceof CrookItem)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                moveEntity(player, entity, true);
                player.getItemInHand(hand).hurtAndBreak(1, player, hand == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        });
    }

    static void moveEntity(Player player, Entity target, boolean pulling) {
        double x = player.getX() - target.getX(), z = player.getZ() - target.getZ();
        double distance = Math.sqrt(x*x + z*z);
        double sign = pulling ? 1D : -1D;
        double y = (pulling ? player.getY() > target.getY() : player.getY() < target.getY()) ? .5D : 0D;
        // Avoid the old divide-by-zero bug when player and creature share X/Z.
        target.push(distance < 1E-6 ? 0D : x / distance * sign, y,
                distance < 1E-6 ? 0D : z / distance * sign);
        target.syncVelocity = true;
    }
}
