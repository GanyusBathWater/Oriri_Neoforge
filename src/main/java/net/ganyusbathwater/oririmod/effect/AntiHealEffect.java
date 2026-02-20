package net.ganyusbathwater.oririmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Anti-Heal Effect - A harmful effect that prevents all incoming healing.
 * 
 * Amplifier 0 (Level 1): Base duration
 * Amplifier 1 (Level 2): Extended duration
 * 
 * While this effect is active, the entity cannot be healed by any means.
 * The healing blocking is handled via LivingHealEvent in ServerEvents.
 */
public class AntiHealEffect extends MobEffect {

    public AntiHealEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // This effect doesn't need to tick - the healing prevention is handled via
        // events
        return false;
    }
}
