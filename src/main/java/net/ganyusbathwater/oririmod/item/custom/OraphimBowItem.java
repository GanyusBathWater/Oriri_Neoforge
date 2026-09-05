package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import java.util.List;

public class OraphimBowItem extends CustomBowItemClass {
    public OraphimBowItem(Properties properties, ModRarity rarity) {
        super(properties, rarity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.oririmod.oraphim_bow.ability").withStyle(ChatFormatting.DARK_PURPLE));
        tooltipComponents.add(Component.translatable("tooltip.oririmod.oraphim_bow.lore").withStyle(ChatFormatting.GRAY));
    }
}
