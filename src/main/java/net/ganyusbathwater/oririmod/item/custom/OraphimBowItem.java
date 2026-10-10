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
        net.minecraft.world.item.component.CustomData data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        int currentLevel = data.copyTag().contains("oriri_level") ? data.copyTag().getInt("oriri_level") : 1;
        net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.oraphim_bow.ability");
        if (currentLevel > 1) {
            net.ganyusbathwater.oririmod.util.TooltipHelper.addEmptyLine(tooltipComponents);
            net.ganyusbathwater.oririmod.util.TooltipHelper.addLevelInfo(tooltipComponents, "item.oririmod.oraphim_bow", currentLevel);
        }
        net.ganyusbathwater.oririmod.util.TooltipHelper.addLore(tooltipComponents, "tooltip.oririmod.oraphim_bow.lore");
    }
}
