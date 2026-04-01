package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

public class HarbingerScytheItem extends CustomScytheItem {
    public HarbingerScytheItem(Tier tier, Properties properties, net.ganyusbathwater.oririmod.util.ModRarity rarity) {
        super(tier, properties, rarity);
    }

    @Override
    public void applyScytheEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Ancient Scythe effect
        target.addEffect(new MobEffectInstance(ModEffects.STUNNED_EFFECT, 100, 0), attacker);
        // Crystal Scythe effect
        target.addEffect(new MobEffectInstance(ModEffects.ANTI_HEAL_EFFECT, 100, 0), attacker);
        // Gilded Netherite Scythe effect
        target.addEffect(new MobEffectInstance(ModEffects.BROKEN_EFFECT, 100, 0), attacker);
        // Prismarine Scythe effect
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), attacker);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), attacker);
        // Black Ice Scythe effect
        int minFreezeTicks = 140;
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), minFreezeTicks + 100));
        // Molten Scythe effect
        target.igniteForSeconds(5.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltipComponents,
            net.minecraft.world.item.TooltipFlag tooltipFlag) {
        long now = System.currentTimeMillis();
        float phase = (now % 4000L) / 4000.0f;
        int rgb = hsvToRgb(phase, 1.0f, 1.0f);

        net.minecraft.network.chat.Component rainbowEffects = net.minecraft.network.chat.Component.translatable("tooltip.oririmod.scythe.harbinger.effects")
                .withStyle(style -> style.withColor(rgb));

        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.scythe.harbinger", rainbowEffects));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private static int hsvToRgb(float h, float s, float v) {
        float r, g, b;

        int i = (int) (h * 6.0f);
        float f = (h * 6.0f) - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);

        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            case 5 -> { r = v; g = p; b = q; }
            default -> { r = v; g = t; b = p; }
        }

        int ri = (int)(r * 255.0f) & 0xFF;
        int gi = (int)(g * 255.0f) & 0xFF;
        int bi = (int)(b * 255.0f) & 0xFF;

        return (ri << 16) | (gi << 8) | bi;
    }
}

