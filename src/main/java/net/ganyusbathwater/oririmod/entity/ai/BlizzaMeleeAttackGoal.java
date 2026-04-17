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
        LivingEntity target = blizza.getTarget();
        if (target == null || !target.isAlive()) return false;
        // Issue #7: if storm is active, use melee slot regardless of range so Blizza
        // can cast Illager Special from the storm centre
        if (blizza.isEyeOfStormActive()) return blizza.distanceToSqr(target) <= 48.0 * 48.0;
        return blizza.distanceToSqr(target) <= MELEE_RANGE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        return timer < ANIM_TICKS && !blizza.isDefeated();
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
            blizza.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (useIllager) {
            // Stay put while casting
            blizza.getNavigation().stop();
            if (!hitDealt && timer == ILLAGER_FRAME) {
                hitDealt = true;
                if (!blizza.level().isClientSide && blizza.level() instanceof ServerLevel sl) {
                    float yaw = (float) Math.toRadians(-blizza.getYRot());
                    Vec3 origin = blizza.position();
                    MagicWaveUtil.spawnIllagerSpecial(sl, origin, yaw, blizza.getId());
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
    }
}
