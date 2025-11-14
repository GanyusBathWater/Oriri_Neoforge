package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public interface ModRarityCarrier {
    ModRarity getModRarity();

    // Zentralisierte Tooltip-Erzeugung für alle Items, inkl. Vestige-spezifischer Inhalte
    default void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Rarity (einheitlich für alle Items)
        tooltip.add(getModRarity().coloredDisplayName());

        // Vestige-spezifischer Tooltip
        if (this instanceof VestigeItem v) {
            int max = v.getMaxLevel();
            int unlocked = v.getUnlockedLevel(stack);

            String base = v.getTranslationKeyBase() != null ? v.getTranslationKeyBase() : "tooltip.oririmod.vestige";

            // Level-Übersicht
            tooltip.add(Component.translatable(base + ".level", unlocked, max).withStyle(ChatFormatting.AQUA));

            // Disabled-Maske aus NBT lesen
            int mask = getInt(stack, VestigeItem.NBT_DISABLED_MASK, 0);

            for (int lvl = 1; lvl <= unlocked; lvl++) {
                boolean enabled = (mask & (1 << (lvl - 1))) == 0;

                // Item-spezifische Level-Beschreibung
                String descKey = String.format("%s.level.%d.description", base, lvl);
                tooltip.add(Component.translatable(descKey).withStyle(enabled ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
            }

            // Lore
            tooltip.add(Component.translatable(base + ".lore").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // Kleiner Helfer für NBT-Int
    private static int getInt(ItemStack stack, String key, int def) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return def;
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getInt(key) : def;
    }
}
