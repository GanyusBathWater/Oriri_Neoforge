package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.item.Tier;

public class SolsEmbraceItem extends CustomSwordItem {
    public SolsEmbraceItem(Tier pTier, Properties pProperties, ModRarity pRarity) {
        super(pTier, pProperties, pRarity);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack pStack, net.minecraft.world.item.Item.TooltipContext pContext, java.util.List<net.minecraft.network.chat.Component> pTooltipComponents, net.minecraft.world.item.TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.sols_embrace").withStyle(net.minecraft.ChatFormatting.GOLD));
    }
}
