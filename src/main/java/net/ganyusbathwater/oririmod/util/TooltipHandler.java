package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.ItemElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TooltipHandler {

    private TooltipHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(TooltipHandler.class);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        List<Component> tooltip = event.getToolTip();

        List<Component> extraLines = new ArrayList<>();

        // 1) Rarity / Vestige von ModRarityCarrier einsammeln
        if (item instanceof ModRarityCarrier carrier) {
            extraLines.addAll(
                    carrier.buildModTooltip(stack, event.getContext(), event.getFlags()));
        }

        // 2) Element-Zeile hinzufügen
        Element element = ItemElementRegistry.getElement(stack);
        if (element != null && element != Element.PHYSICAL) {
            ChatFormatting color = switch (element) {
                case FIRE -> ChatFormatting.RED;
                case WATER -> ChatFormatting.AQUA;
                case EARTH -> ChatFormatting.DARK_GREEN;
                case NATURE -> ChatFormatting.GREEN;
                case LIGHT -> ChatFormatting.WHITE;
                case DARKNESS -> ChatFormatting.DARK_PURPLE;
                case TRUE_DAMAGE -> ChatFormatting.WHITE;
                default -> ChatFormatting.GRAY;
            };
            String key = "tooltip.oririmod.element." + element.name().toLowerCase(Locale.ROOT);
            net.minecraft.network.chat.MutableComponent text = Component.translatable(key);

            if (element == Element.TRUE_DAMAGE) {
                // Determine a pulsing white to gray color using system time
                long time = System.currentTimeMillis() % 2000L;
                float ratio = (float) Math.abs((time / 1000.0) - 1.0); // 0.0 to 1.0 back and forth
                int grayValue = 150 + (int) (105 * ratio); // 150 to 255
                int colorInt = (grayValue << 16) | (grayValue << 8) | grayValue;
                text = text.withStyle(net.minecraft.network.chat.Style.EMPTY
                        .withColor(net.minecraft.network.chat.TextColor.fromRgb(colorInt)));
            } else {
                text = text.withStyle(color);
            }
            extraLines.add(text);
        }

        if (extraLines.isEmpty()) {
            return;
        }

        // \=\=\= HIER bestimmst du die Position \=\=\=
        // Beispiel: direkt NACH der ersten Zeile (Itemname-Zeile wird von MC selbst
        // gerendert)
        int insertIndex = 1;
        if (insertIndex > tooltip.size()) {
            insertIndex = tooltip.size();
        }

        tooltip.addAll(insertIndex, extraLines);
    }
}
