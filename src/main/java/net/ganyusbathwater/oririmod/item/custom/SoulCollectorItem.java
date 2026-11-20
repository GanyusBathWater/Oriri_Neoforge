package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.block.menu.SoulCollectorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class SoulCollectorItem extends Item {

    public SoulCollectorItem(Properties properties) {
        super(properties);
    }

    public static ItemStack getStoredShard(ItemStack collectorStack) {
        ItemContainerContents contents = collectorStack.get(DataComponents.CONTAINER);
        if (contents == null || contents.getSlots() <= 0) {
            return ItemStack.EMPTY;
        }
        return contents.getStackInSlot(0);
    }

    public static void setStoredShard(ItemStack collectorStack, ItemStack shard) {
        if (shard.isEmpty()) {
            collectorStack.remove(DataComponents.CONTAINER);
        } else {
            collectorStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(shard.copyWithCount(1))));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int slotIndex = (hand == InteractionHand.MAIN_HAND)
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInv, p) -> new SoulCollectorMenu(containerId, playerInv, slotIndex),
                            stack.getHoverName()
                    ),
                    buf -> buf.writeVarInt(slotIndex)
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        ItemStack shard = getStoredShard(stack);
        if (!shard.isEmpty() && SoulShardItem.hasBoundEntity(shard)) {
            String entityId = SoulShardItem.getBoundEntityId(shard);
            Optional<EntityType<?>> opt = entityId != null ? EntityType.byString(entityId) : Optional.empty();
            Component mobName = opt.isPresent()
                    ? opt.get().getDescription().copy().withStyle(ChatFormatting.WHITE)
                    : Component.literal(String.valueOf(entityId)).withStyle(ChatFormatting.WHITE);
            int kills = SoulShardItem.getKills(shard);
            int req = SoulShardItem.getRequiredKills(shard);
            if (SoulShardItem.isCompleted(shard)) {
                tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_collector_stored_complete", mobName)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_collector_stored_progress", mobName, kills, req)
                        .withStyle(ChatFormatting.AQUA));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_collector_empty")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_collector_open_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
