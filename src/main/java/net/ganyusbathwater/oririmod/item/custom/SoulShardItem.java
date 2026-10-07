package net.ganyusbathwater.oririmod.item.custom;

import net.minecraft.world.item.Item;

public class SoulShardItem extends Item {
    public SoulShardItem(Properties properties) {
        super(properties);
    }
    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        if (!entity.isAlive() || entity instanceof net.minecraft.world.entity.player.Player) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.contains("captured_entity")) {
            return net.minecraft.world.InteractionResult.PASS; // Already captured
        }

        if (!player.level().isClientSide) {
            tag.putString("captured_entity", net.minecraft.world.entity.EntityType.getKey(entity.getType()).toString());
            
            net.minecraft.world.item.ItemStack shardToModify = stack.copy();
            shardToModify.setCount(1);
            shardToModify.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            
            if (stack.isEmpty()) {
                player.setItemInHand(hand, shardToModify);
            } else if (!player.getInventory().add(shardToModify)) {
                player.drop(shardToModify, false);
            }

            entity.discard();
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Captured " + entity.getDisplayName().getString()), true);
        }

        return net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext tooltipContext, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.contains("captured_entity")) {
            String entityId = tag.getString("captured_entity");
            tooltipComponents.add(net.minecraft.network.chat.Component.literal("Entity: " + entityId).withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(net.minecraft.network.chat.Component.literal("Empty (Right-click a mob to capture)").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, tooltipContext, tooltipComponents, tooltipFlag);
    }
}
