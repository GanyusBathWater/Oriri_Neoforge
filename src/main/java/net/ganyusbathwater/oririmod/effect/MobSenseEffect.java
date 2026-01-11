package net.ganyusbathwater.oririmod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class MobSenseEffect extends MobEffect {

    public MobSenseEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && (duration % 10) == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) return true;
        if (!(player.level() instanceof ServerLevel level)) return true;

        int safeAmp = Math.max(0, amplifier);
        int radiusBlocks = 12 * (safeAmp + 1);

        AABB box = player.getBoundingBox().inflate(radiusBlocks);

        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, false));
        }

        return true;
    }
}