package net.ganyusbathwater.oririmod.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public interface ModRarityCarrier {
    ModRarity getModRarity();

    default List<Component> buildModTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag) {
        List<Component> list = new ArrayList<>();
        

        return list;
    }

    // Legacy: falls du irgendwo noch direkt aufrufst
    default void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(buildModTooltip(stack, context, flag));
    }

    private static int getInt(ItemStack stack, String key, int def) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return def;
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getInt(key) : def;
    }
}