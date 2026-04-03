package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

public class PrismarineScytheItem extends CustomScytheItem {
    public PrismarineScytheItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties, rarity);
    }

    @Override
    public void applyScytheEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Applies Slowness and Weakness for 100 ticks (5 seconds)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), attacker);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltipComponents,
            net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents
                .add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.scythe.slowness_weakness"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
