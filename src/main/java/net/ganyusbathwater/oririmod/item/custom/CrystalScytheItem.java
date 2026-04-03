package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

public class CrystalScytheItem extends CustomScytheItem {
    public CrystalScytheItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties, rarity);
    }

    @Override
    public void applyScytheEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Applies Anti-Heal effect for 100 ticks (5 seconds)
        target.addEffect(new MobEffectInstance(ModEffects.ANTI_HEAL_EFFECT, 100, 0), attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltipComponents,
            net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.scythe.anti_heal"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
