package net.ganyusbathwater.oririmod.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Converts a CosmicTooltipSurrogate into a CosmicTooltipComponent.
 * Only the item name is split — description lines are vanilla TextComponents.
 */
public final class CosmicClientTooltipFactory {

    private CosmicClientTooltipFactory() {}

    public static ClientTooltipComponent create(CosmicTooltipSurrogate surrogate) {
        Font font = Minecraft.getInstance().font;
        // Split the name at a generous max-width (tooltips are rarely wider than 200px).
        List<FormattedCharSequence> nameLines = font.split(surrogate.itemName(), 200);
        return new CosmicTooltipComponent(surrogate.data(), nameLines);
    }
}
