package net.ganyusbathwater.oririmod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * True Vision: The ultimate perceptive state.
 *
 * Capabilities:
 * 1. Built-in Night Vision (automatically maintains ambient night vision lightmap).
 * 2. Built-in Mob Sense (highlights all nearby living entities within a 24m sphere with Glowing outline).
 * 3. Complete immunity and active purging of vision debuffs: Darkness, Blindness, and Nausea.
 * 4. Disables atmospheric / deep-sea fog and negates Abyssal Darkness.
 */
public class TrueVisionEffect extends MobEffect {

    public TrueVisionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // tick every tick to ensure continuous debuff purging and perception
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 1. Actively purge vision impairing debuffs: Darkness, Blindness, and Nausea (Confusion)
        if (entity.hasEffect(MobEffects.DARKNESS)) {
            entity.removeEffect(MobEffects.DARKNESS);
        }
        if (entity.hasEffect(MobEffects.BLINDNESS)) {
            entity.removeEffect(MobEffects.BLINDNESS);
        }
        if (entity.hasEffect(MobEffects.CONFUSION)) {
            entity.removeEffect(MobEffects.CONFUSION);
        }

        // 2. Built-in Night Vision: Maintain hidden ambient night vision
        MobEffectInstance nightVision = entity.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision == null || (nightVision.isAmbient() && nightVision.getDuration() <= 300)) {
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, true, false, false));
        }

        // 3. Built-in Mob Sense: Highlight all living entities in radius every 10 ticks (server-side)
        if (entity.tickCount % 10 == 0 && entity.level() instanceof ServerLevel level) {
            int radius = 24 * (Math.max(0, amplifier) + 1);
            AABB searchBox = entity.getBoundingBox().inflate(radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != entity && e.isAlive())) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
            }
        }

        return true;
    }
}

