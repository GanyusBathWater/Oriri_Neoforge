package net.ganyusbathwater.oririmod.combat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Locale;

public final class ElementTooltipHandler {

    private ElementTooltipHandler() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(ElementTooltipHandler.class);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        Element element = ItemElementRegistry.getElement(stack);
        if (element == null || element == Element.PHYSICAL) {
            return;
        }

        ChatFormatting color = switch (element) {
            case FIRE -> ChatFormatting.RED;
            case WATER -> ChatFormatting.AQUA;
            case EARTH -> ChatFormatting.DARK_GREEN;
            case NATURE -> ChatFormatting.GREEN;
            case LIGHT -> ChatFormatting.WHITE;
            case DARKNESS -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.GRAY;
        };

        String key = "tooltip.oririmod.element." + element.name().toLowerCase(Locale.ROOT);
        event.getToolTip().add(Component.translatable(key).withStyle(color));
    }
}