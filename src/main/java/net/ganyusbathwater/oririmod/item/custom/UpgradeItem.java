package net.ganyusbathwater.oririmod.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class UpgradeItem extends Item {
    private final String upgradeType;
    private final String tooltipKey;

    public UpgradeItem(Properties properties, String upgradeType, String tooltipKey) {
        super(properties);
        this.upgradeType = upgradeType;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        int level = 1;
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            net.minecraft.nbt.CompoundTag tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("oriri_level")) {
                level = tag.getInt("oriri_level");
            }
        }
        return Component.translatable(this.getDescriptionId(stack)).append(" Level " + level);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int level = 1;
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            net.minecraft.nbt.CompoundTag tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("oriri_level")) {
                level = tag.getInt("oriri_level");
            }
        }
        tooltipComponents.add(Component.translatable(tooltipKey, level).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
