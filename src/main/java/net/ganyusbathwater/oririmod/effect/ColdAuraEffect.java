package net.ganyusbathwater.oririmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ColdAuraEffect extends MobEffect {

    public ColdAuraEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Trigger damage exactly once a second (every 20 ticks)
        return duration > 0 && (duration % 20) == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return true;

        // "after the 10th stack it starts to damage... 1 heart per second"
        // 10 stacks normally means amplifier = 9. 
        // "after 10th" means 11th stack -> amplifier 10 = 1 heart (2.0f damage)
        // Stack 12 -> amplifier 11 = 2 hearts (4.0f damage) etc.
        if (amplifier >= 10) {
            float damage = (amplifier - 9) * 2.0f;
            entity.hurt(entity.damageSources().freeze(), damage);
        }

        return true;
    }
}
