package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

public class PandorasBladeItem extends CustomSwordItem {
    public PandorasBladeItem(Tier pTier, Properties pProperties, ModRarity pRarity) {
        super(pTier, pProperties, pRarity);
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }

    @Override
    public void appendHoverText(ItemStack pStack, net.minecraft.world.item.Item.TooltipContext pContext, java.util.List<net.minecraft.network.chat.Component> pTooltipComponents, net.minecraft.world.item.TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.pandoras_blade").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
    }
}
