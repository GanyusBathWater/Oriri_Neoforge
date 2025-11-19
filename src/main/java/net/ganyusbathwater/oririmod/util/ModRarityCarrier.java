package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.ItemElementRegistry;
import net.ganyusbathwater.oririmod.item.custom.CustomSwordItem;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface ModRarityCarrier {
    ModRarity getModRarity();

    // Zusatz-Tooltip für Rarity, Vestige und Attribute
    default void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Rarity
        tooltip.add(getModRarity().coloredDisplayName());



        // Vestige-Infos
        if (this instanceof VestigeItem v) {
            int max = v.getMaxLevel();
            int unlocked = v.getUnlockedLevel(stack);

            String base = v.getTranslationKeyBase() != null
                    ? v.getTranslationKeyBase()
                    : "tooltip.oririmod.vestige";

            tooltip.add(Component.translatable(base + ".level", unlocked, max)
                    .withStyle(ChatFormatting.AQUA));

            int mask = getInt(stack, VestigeItem.NBT_DISABLED_MASK, 0);

            for (int lvl = 1; lvl <= unlocked; lvl++) {
                boolean enabled = (mask & (1 << (lvl - 1))) == 0;
                String descKey = String.format("%s.level.%d.description", base, lvl);
                tooltip.add(Component.translatable(descKey)
                        .withStyle(enabled ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
            }

            tooltip.add(Component.translatable(base + ".lore")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // NBT-Helfer
    private static int getInt(ItemStack stack, String key, int def) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return def;
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getInt(key) : def;
    }
}