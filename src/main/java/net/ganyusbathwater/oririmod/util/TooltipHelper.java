package net.ganyusbathwater.oririmod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

public class TooltipHelper {

    /**
     * Adds narrative lore to the tooltip.
     * Style: DARK_GRAY, ITALIC
     */
    public static void addLore(List<Component> tooltip, String translationKey) {
        tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /**
     * Adds mana cost information.
     * Style: DARK_AQUA
     */
    public static void addManaCost(List<Component> tooltip, int cost) {
        tooltip.add(Component.translatable("tooltip.oririmod.mana_cost", cost).withStyle(ChatFormatting.DARK_AQUA));
    }

    /**
     * Adds level information typically used for evolving/upgrading items.
     * Constructs keys based on the baseKey (e.g. "item.oririmod.arbiter_crossbow")
     * Level Style: YELLOW
     * Description Style: GOLD
     */
    public static void addLevelInfo(List<Component> tooltip, String baseKey, int level) {
        tooltip.add(Component.translatable(baseKey + ".level", 
            Component.literal(String.valueOf(level)).withStyle(ChatFormatting.YELLOW)
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(baseKey + ".level." + level + ".description").withStyle(ChatFormatting.GOLD));
    }

    /**
     * Adds a special weapon effect, ability, or scythe mechanic.
     * Style: LIGHT_PURPLE
     */
    public static void addAbility(List<Component> tooltip, String translationKey) {
        tooltip.add(Component.translatable(translationKey).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /**
     * Adds the consumable status (whether the item effect is active or not).
     * Style: RED if eaten, GRAY if uneaten.
     */
    public static void addConsumableStatus(List<Component> tooltip, boolean isEaten, String uneatenTranslationKey) {
        if (isEaten) {
            tooltip.add(Component.translatable("tooltip.oririmod.consumable.eaten").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable(uneatenTranslationKey).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Adds general stat information (e.g. Damage).
     * Style: GRAY
     */
    public static void addStat(List<Component> tooltip, String translationKey, Object... args) {
        tooltip.add(Component.translatable(translationKey, args).withStyle(ChatFormatting.GRAY));
    }
    
    /**
     * Adds an empty line to the tooltip for spacing.
     */
    public static void addEmptyLine(List<Component> tooltip) {
        tooltip.add(Component.empty());
    }
}
