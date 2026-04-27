package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.ganyusbathwater.oririmod.util.MagicWaveUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Handles Blizza's melee attack. Waits until she is close enough,
 * triggers the blizza_normal_attack animation, deals damage at frame ~7.
 *
 * Issue #7 addition: if an Eye of the Storm is active, Blizza fires the
 * Illager Special (magic_3) instead of the melee swing, so she stays
 * dangerous while standing in the centre of her own storm.
 */
public class BlizzaMeleeAttackGoal extends Goal {

    private static final int ANIM_TICKS     = 15;  // 0.75 s
    private static final int HIT_FRAME      = 7;   // melee damage frame
    private static final int ILLAGER_FRAME  = 1;   // illager special fires at start
    private static final double MELEE_RANGE_SQ = 5.0 * 5.0;

    private final BlizzaEntity blizza;
    private int timer    = 0;
    private int nextAttackTick = 0;
    private boolean hitDealt = false;
    /** True when this cycle was substituted with the Illager Special. */
    private boolean useIllager = false;

    public BlizzaMeleeAttackGoal(BlizzaEntity blizza) {
        this.blizza = blizza;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (blizza.isDefeated()) return false;
        if (blizza.getAttackType() != BlizzaEntity.ATTACK_NONE) return false;
        if (blizza.tickCount < nextAttackTick) return false;

        LivingEntity target = blizza.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = blizza.distanceToSqr(target);

        // Issue #7: if storm is active, use melee slot regardless of range so Blizza
        // can cast Illager Special from the storm centre
        if (blizza.isEyeOfStormActive()) {
            return distSq <= 48.0 * 48.0;
        }

        if (distSq <= MELEE_RANGE_SQ) {
            // 50% chance to skip the melee swing this time to allow magic attacks to fire
            if (blizza.getRandom().nextFloat() > 0.5f) {
                // Set a small delay before we check for melee again (0.5s)
                nextAttackTick = blizza.tickCount + 10;
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        // Issue #12: shorten to 10 ticks (0.5s) if storm-boosted special
        int limit = (useIllager && blizza.isEyeOfStormActive()) ? 10 : ANIM_TICKS;
        return timer < limit && !blizza.isDefeated();
    }

    @Override
    public void start() {
        timer      = 0;
        hitDealt   = false;
        useIllager = blizza.isEyeOfStormActive();

        if (useIllager) {
            blizza.setAttackType(BlizzaEntity.ATTACK_ILLAGER);
            blizza.triggerAnim("attack_controller", "blizza_magic_3");
        } else {
            blizza.setAttackType(BlizzaEntity.ATTACK_MELEE);
            blizza.triggerAnim("attack_controller", "blizza_normal_attack");
        }
    }

    @Override
    public void tick() {
        timer++;

        LivingEntity target = blizza.getTarget();
        if (target != null && target.isAlive()) {
            // Increase rotation speed to 100.0F for responsive tracking (issue #11)
            blizza.getLookControl().setLookAt(target, 100.0F, 100.0F);
        }

        if (useIllager) {
            // Stay put while casting
            blizza.getNavigation().stop();
            if (!hitDealt && timer == ILLAGER_FRAME) {
                hitDealt = true;
                if (!blizza.level().isClientSide && blizza.level() instanceof ServerLevel sl) {
                    double dx = target.getX() - blizza.getX();
                    double dz = target.getZ() - blizza.getZ();
                    float yaw = (float) Math.atan2(dx, dz);
                    Vec3 origin = blizza.position();
                    
                    // 10 ticks (0.5s) charge delay during storm (issue #12)
                    MagicWaveUtil.spawnIllagerSpecial(sl, origin, yaw, blizza.getId(), 10);
                }
            }
        } else {
            if (!hitDealt && timer == HIT_FRAME && target != null && target.isAlive()) {
                blizza.doMeleeHit(target);
                hitDealt = true;
            }
        }
    }

    @Override
    public void stop() {
        timer      = 0;
        hitDealt   = false;
        blizza.setAttackType(BlizzaEntity.ATTACK_NONE);
        blizza.setLastAttack(useIllager ? BlizzaEntity.ATTACK_ILLAGER : BlizzaEntity.ATTACK_MELEE);
        useIllager = false;
        // 2 second cooldown after a melee swing (or substitution)
        nextAttackTick = blizza.tickCount + 40;
    }
}
