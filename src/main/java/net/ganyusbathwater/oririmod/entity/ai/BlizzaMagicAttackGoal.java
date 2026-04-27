package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.entity.EyeOfTheStormEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity;
import net.ganyusbathwater.oririmod.util.IcicleStormUtil;
import net.ganyusbathwater.oririmod.util.MagicWaveUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Handles all three of Blizza's magic attacks:
 *   ATTACK_ICICLE  (magic_1) – IcicleStormUtil rain on the target
 *   ATTACK_STORM   (magic_2) – EyeOfTheStormEntity spawned AT BLIZZA
 *   ATTACK_ILLAGER (magic_3) – MagicWaveUtil.spawnIllagerSpecial
 *
 * Attack durations:
 *   Icicle  ≈ 3 s  → 60-tick cooldown
 *   Storm   ≈ 20 s → 400-tick cooldown
 *   Illager ≈ 5 s  → 100-tick cooldown
 *
 * If Blizza takes > 25 hearts (250 HP) during the cooldown, the next attack
 * fires immediately (cooldown reset to 0).
 *
 * If an Eye of the Storm is active, Blizza uses Illager Special instead of
 * standing still.
 */
public class BlizzaMagicAttackGoal extends Goal {

    // ── Duration the goal runs to play the cast animation ─────────────────
    /** Ticks the cast animation lasts (≈ 2 s). */
    private static final int ANIM_TICKS   = 40;
    /** Frame at which the actual attack effect fires (start of animation, issue #2). */
    private static final int EFFECT_FRAME = 1;

    // ── Attack-specific cooldowns (match attack duration) ─────────────────
    /** Icicle Rain: ~3 s active → 3 s cooldown = 60 ticks */
    private static final int CD_ICICLE  = 60;
    /** Eye of the Storm: ~20 s active → 20 s cooldown = 400 ticks */
    private static final int CD_STORM   = 400;
    /** Illager Special: ~5 s active → 5 s cooldown = 100 ticks */
    private static final int CD_ILLAGER = 100;

    /** Range beyond which Blizza won't start a new magic attack. */
    private static final double MAX_RANGE_SQ = 48.0 * 48.0;

    // Triple-special sub-cast config
    private static final int TRIPLE_CAST_DELAY = 15;

    // ── "Hurt interrupt" threshold: 25 hearts = 50 HP ─────────────────────
    private static final float INTERRUPT_DAMAGE_THRESHOLD = 50.0f;

    private final BlizzaEntity blizza;
    private int timer         = 0;
    private int cooldownTimer = 0;
    private int chosenAttack  = BlizzaEntity.ATTACK_NONE;

    /** Health snapshot at cooldown start — used to detect 25-heart burst damage. */
    private float healthAtCooldownStart = 0f;

    // Triple-special state
    private boolean tripleMode  = false;
    private int     tripleCount = 0;
    private int     tripleTimer = 0;

    public BlizzaMagicAttackGoal(BlizzaEntity blizza) {
        this.blizza = blizza;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── canUse / canContinueToUse ──────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (blizza.isDefeated()) return false;
        if (cooldownTimer > 0) return false;
        if (blizza.getAttackType() != BlizzaEntity.ATTACK_NONE) return false;
        LivingEntity target = blizza.getTarget();
        if (target == null || !target.isAlive()) return false;
        return blizza.distanceToSqr(target) <= MAX_RANGE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        if (blizza.isDefeated()) return false;
        
        // Issue #12: shorten duration to 10 ticks (0.5s) if storm is active during Illager Special
        int limit = (chosenAttack == BlizzaEntity.ATTACK_ILLAGER && blizza.isEyeOfStormActive()) ? 10 : ANIM_TICKS;
        return (timer < limit) || (tripleMode && tripleCount < 3);
    }

    // ── start / tick / stop ───────────────────────────────────────────────

    @Override
    public void start() {
        timer       = 0;
        tripleMode  = false;
        tripleCount = 0;
        tripleTimer = 0;

        // Issue #7: if Eye of the Storm is active, force Illager Special
        int pick;
        if (blizza.isEyeOfStormActive()) {
            pick = BlizzaEntity.ATTACK_ILLAGER;
        } else {
            int last = blizza.getLastAttack();
            pick = pickAttack(last);
        }

        // Phase 2 triple-special override (30% chance)
        if (blizza.isPhase2() && pick == BlizzaEntity.ATTACK_ILLAGER
                && blizza.getRandom().nextFloat() < 0.30f) {
            tripleMode = true;
        }

        chosenAttack = pick;
        blizza.setAttackType(chosenAttack);
        triggerAnimation(chosenAttack);
    }

    @Override
    public void tick() {
        timer++;

        LivingEntity target = blizza.getTarget();
        if (target != null && target.isAlive()) {
            // Increase rotation speed to 100.0F for responsive tracking (issue #11)
            blizza.getLookControl().setLookAt(target, 100.0F, 100.0F);
        }

        // Issue #7 & #12: keep Blizza stationary during Eye of the Storm
        if (chosenAttack == BlizzaEntity.ATTACK_STORM || blizza.isEyeOfStormActive()) {
            blizza.getNavigation().stop();
        }

        // Main attack effect fires at EFFECT_FRAME
        if (timer == EFFECT_FRAME) {
            fireEffect(chosenAttack, 0f);
        }

        // Triple-special extra casts
        if (tripleMode && timer > ANIM_TICKS) {
            tripleTimer++;
            if (tripleTimer % TRIPLE_CAST_DELAY == 0 && tripleCount < 3) {
                // Issue #3: Pass 0f so it uses Blizza's current yaw (which tracks the player)
                fireEffect(BlizzaEntity.ATTACK_ILLAGER, 0f);
                tripleCount++;
            }
        }
    }

    @Override
    public void stop() {
        blizza.setAttackType(BlizzaEntity.ATTACK_NONE);
        blizza.setLastAttack(chosenAttack);
        timer       = 0;
        tripleMode  = false;
        tripleCount = 0;
        tripleTimer = 0;

        // Issue #3: set cooldown equal to attack duration
        cooldownTimer = cooldownForAttack(chosenAttack);
        healthAtCooldownStart = blizza.getHealth();

        chosenAttack = BlizzaEntity.ATTACK_NONE;
    }

    // ── Per-tick cooldown decrement (called from BlizzaEntity.tick()) ──────

    /** Called every tick by BlizzaEntity so the cooldown decrements even while idle. */
    public void tickCooldown() {
        if (cooldownTimer <= 0) return;
        cooldownTimer--;

        // Issue #3: if Blizza takes 25+ hearts during cooldown, cut it short
        float currentHealth = blizza.getHealth();
        if (healthAtCooldownStart - currentHealth >= INTERRUPT_DAMAGE_THRESHOLD) {
            cooldownTimer = 0;
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    /** Returns the cooldown (in ticks) matching the duration of the attack. */
    private static int cooldownForAttack(int attack) {
        return switch (attack) {
            case BlizzaEntity.ATTACK_STORM   -> CD_STORM;
            case BlizzaEntity.ATTACK_ICICLE  -> CD_ICICLE;
            case BlizzaEntity.ATTACK_ILLAGER -> CD_ILLAGER;
            default -> CD_ILLAGER;
        };
    }

    /** Pick a random attack from {ICICLE, STORM, ILLAGER} ≠ last. */
    private int pickAttack(int last) {
        LivingEntity target = blizza.getTarget();
        if (target != null && blizza.distanceToSqr(target) <= 25.0) {
            // If target is close, 25% chance to force Illager Special (issue #10)
            if (blizza.getRandom().nextFloat() < 0.25f) {
                return BlizzaEntity.ATTACK_ILLAGER;
            }
        }

        int[] options = { BlizzaEntity.ATTACK_ICICLE, BlizzaEntity.ATTACK_STORM, BlizzaEntity.ATTACK_ILLAGER };
        int pick = options[blizza.getRandom().nextInt(options.length)];
        for (int i = 0; i < 10 && pick == last; i++) {
            pick = options[blizza.getRandom().nextInt(options.length)];
        }
        return pick;
    }

    private void triggerAnimation(int attack) {
        String animName = switch (attack) {
            case BlizzaEntity.ATTACK_ICICLE  -> "blizza_magic_1";
            case BlizzaEntity.ATTACK_STORM   -> "blizza_magic_2";
            case BlizzaEntity.ATTACK_ILLAGER -> "blizza_magic_3";
            default -> "blizza_magic_1";
        };
        blizza.triggerAnim("attack_controller", animName);
    }

    private void fireEffect(int attack, float yawOverride) {
        if (blizza.level().isClientSide) return;
        if (!(blizza.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity target = blizza.getTarget();
        if (target == null || !target.isAlive()) return;

        switch (attack) {
            case BlizzaEntity.ATTACK_ICICLE -> {
                // Icicle Rain on the target
                IcicleStormUtil.unleash(serverLevel, target.blockPosition(), blizza);
            }
            case BlizzaEntity.ATTACK_STORM -> {
                // Issue #6: spawn Eye of the Storm at Blizza's position (not the player's)
                EyeOfTheStormEntity storm = new EyeOfTheStormEntity(
                        ModEntities.EYE_OF_THE_STORM.get(), serverLevel);
                storm.setPos(blizza.getX(), blizza.getY(), blizza.getZ());
                serverLevel.addFreshEntity(storm);
            }
            case BlizzaEntity.ATTACK_ILLAGER -> {
                // Issue #11 & #12: calculate yaw directly and use dynamic chargeDelay
                float yaw;
                if (yawOverride != 0f) {
                    yaw = yawOverride;
                } else {
                    double dx = target.getX() - blizza.getX();
                    double dz = target.getZ() - blizza.getZ();
                    yaw = (float) Math.atan2(dx, dz);
                }
                
                // 10 ticks (0.5s) if storm is active, else 40 ticks (2s)
                int chargeDelay = blizza.isEyeOfStormActive() ? 10 : 40;
                
                Vec3 origin = blizza.position();
                MagicWaveUtil.spawnIllagerSpecial(serverLevel, origin, yaw, blizza.getId(), chargeDelay);
            }
        }
    }
}
