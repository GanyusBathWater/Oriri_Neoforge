package net.ganyusbathwater.oririmod.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Threat Detection — applied exclusively to players by the Eye of Desolation's beam.
 *
 * Every 10 ticks this effect scans a 24-block radius and forces every monster-category
 * mob to switch its attack target to the afflicted player.
 * Duration stacks additively: each beam hit adds 15 s (300 ticks) on top of whatever
 * time is already remaining.
 */
public class ThreatDetectionEffect extends MobEffect {

    public ThreatDetectionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Pulse every 10 ticks (~2× per second)
        return duration > 0 && (duration % 10) == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) return true;
        if (player.isCreative() || player.isSpectator()) return true;
        if (!(player.level() instanceof ServerLevel level))  return true;

        double radius = 24.0;
        AABB searchBox = player.getBoundingBox().inflate(radius);

        for (Mob mob : level.getEntitiesOfClass(Mob.class, searchBox, m -> m.isAlive())) {
            // Only redirect mobs in the MONSTER category (hostile by nature)
            if (mob.getType().getCategory() == MobCategory.MONSTER) {
                mob.setTarget(player);
            }
        }

        return true;
    }
}
