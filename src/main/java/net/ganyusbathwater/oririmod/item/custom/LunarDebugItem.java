package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.client.skybox.LunarSkyboxState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Debug item for toggling the Lunar Skybox during Overworld testing.
 * Right-click toggles the skybox on/off.
 * This is purely a developer tool — not intended for players.
 *
 * Once the custom dimension is implemented, this will be removed or
 * repurposed as a creative-mode shortcut.
 */
public class LunarDebugItem extends Item {

    public LunarDebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            boolean nowEnabled = LunarSkyboxState.toggle();
            player.displayClientMessage(
                Component.translatable(nowEnabled
                    ? "item.oririmod.lunar_debug_item.message.on"
                    : "item.oririmod.lunar_debug_item.message.off")
                    .withStyle(nowEnabled ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                true // action bar message
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.oririmod.lunar_debug_item.tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));

        // Show current state in tooltip
        boolean active = LunarSkyboxState.isEnabled();
        tooltipComponents.add(Component.literal("State: ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(active ? "ACTIVE" : "INACTIVE")
                        .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY)));
    }
}
