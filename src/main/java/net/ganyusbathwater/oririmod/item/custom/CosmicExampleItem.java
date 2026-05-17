package net.ganyusbathwater.oririmod.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Test item for the cosmic tooltip pipeline.
 * CosmicTooltipData is baked into the stack via Properties.component() in ModItems.
 * OririClient.onGatherTooltipCosmic() detects the component and injects the surrogate.
 * No getTooltipImage() override needed.
 */
public class CosmicExampleItem extends Item {

    public CosmicExampleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipLines, TooltipFlag tooltipFlag) {
        tooltipLines.add(Component.translatable("item.oririmod.cosmic_example.desc1")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipLines.add(Component.translatable("item.oririmod.cosmic_example.desc2")
                .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
