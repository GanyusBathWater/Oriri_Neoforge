package net.ganyusbathwater.oririmod.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ColdAuraEffect extends MobEffect {

    public ColdAuraEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath("oririmod", "effect.cold_aura_speed"),
                -0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Trigger damage exactly once a second (every 20 ticks) only for lethal boss attack stacks
        return duration > 0 && (duration % 20) == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return true;

        // "after the 10th stack it starts to damage... 1 heart per second"
        // Natural snowstorm only stacks to 2 (speed slow only, no damage).
        // Eye of the Storm boss attack stacks past 10 (lethal freeze damage).
        if (amplifier >= 10) {
            float damage = (amplifier - 9) * 2.0f;
            entity.hurt(entity.damageSources().freeze(), damage);
        }

        return true;
    }
}
