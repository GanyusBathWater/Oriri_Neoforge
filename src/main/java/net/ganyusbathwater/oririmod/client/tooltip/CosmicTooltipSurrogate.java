package net.ganyusbathwater.oririmod.client.tooltip;

import net.ganyusbathwater.oririmod.item.component.CosmicTooltipData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Carries only the item name — no extra lines.
 * Inserted at position 0 of the tooltip element list via GatherComponents,
 * replacing the vanilla name element so our component owns the name row.
 */
public record CosmicTooltipSurrogate(
        CosmicTooltipData data,
        Component itemName
) implements TooltipComponent {}
