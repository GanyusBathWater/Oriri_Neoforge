package net.ganyusbathwater.oririmod.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VisualDebugItem extends Item {

    public VisualDebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Visual effects are pure client-side logic, so we only open the menu on the client.
        if (level.isClientSide) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new net.ganyusbathwater.oririmod.client.screen.VisualDebugSelectionScreen());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Right Click: Open Visual Debug Menu").withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
