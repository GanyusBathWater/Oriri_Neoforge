package net.ganyusbathwater.oririmod.item.custom;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import java.util.List;

public class UpgradedSaplingItem extends BlockItem {
    public UpgradedSaplingItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents
                .add(Component.translatable("tooltip.oririmod.upgraded_sapling.lore").withStyle(ChatFormatting.GRAY));
    }
}
