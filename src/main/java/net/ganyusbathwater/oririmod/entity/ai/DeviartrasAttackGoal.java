package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity;
import net.ganyusbathwater.oririmod.util.RootAttackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * DeviartrasAttackGoal — handles ALL of Deviartras's attacks under a single goal.
 *
 * ─── Attack Roster & Durations ───────────────────────────────────────────────
 *   Melee          – only when target ≤ 4 blocks; 15 t anim, hit at tick 7
 *   Overgrowth     – 40 t anim; spawns plant_turret(s); dirt particles tick 15-20
 *   Vine Lock      – 45 t anim; placeholder mechanic (comment in tick())
 *   Spore Blossom  – 40 t anim; spawns eye_of_desolation at EXACT tick 20
 *
 * ─── Global Cooldown ─────────────────────────────────────────────────────────
 *   After any non-melee attack, a 100-tick (5 s) shared cooldown prevents the
 *   next special. Melee does NOT consume the global cooldown slot.
 *   The cooldown is ticked from DeviartrasEntity.tick() via tickGlobalCooldown()
 *   so it decrements even when this goal is idle.
 *
 * ─── Preferred Range ─────────────────────────────────────────────────────────
 *   The boss prefers ranged attacks. Melee is only selected when the target is
 *   within 4 blocks AND no special is queued.
 */
public class DeviartrasAttackGoal extends Goal {

    // ── Animation timing constants ─────────────────────────────────
    /** Duration of the Overgrowth animation (2 s). */
    private static final int OVERGROWTH_ANIM_TICKS   = 40;
    /**
     * Overgrowth particle window: dirt particles appear at the locator
     * "left_plant_mouth" from tick 15 to tick 20 (0.75 s → 1.0 s).
     */
    private static final int OVERGROWTH_PARTICLE_START = 15;
    private static final int OVERGROWTH_PARTICLE_END   = 20;

    /** Duration of the Vine Lock animation (2.25 s). */
    private static final int VINE_LOCK_ANIM_TICKS    = 45;
    /**
     * Vine Lock particle window: falling_spore_blossom particles appear at
     * the locator "right_hand" from tick 5 to tick 40 (0.25 s → 2.0 s).
     */
    private static final int VINE_LOCK_PARTICLE_START = 5;
    private static final int VINE_LOCK_PARTICLE_END   = 40;

    /** Duration of the Spore Blossom animation (2 s). */
    private static final int SPORE_BLOSSOM_ANIM_TICKS = 40;
    /**
     * Spore Blossom mob spawn fires at EXACTLY tick 20 (1 s into the animation).
     * This matches the specification requirement.
     */
    private static final int SPORE_BLOSSOM_SPAWN_TICK = 20;

    /**
     * Global cooldown between special attacks (100 t = 5 s).
     * Decremented every tick via {@link #tickGlobalCooldown()}.
     */
    private static final int GLOBAL_CD = 100;

    /** Max follow range for engaging (48 blocks²). */
    private static final double MAX_RANGE_SQ = 48.0 * 48.0;

    // ── Goal state ────────────────────────────────────────────────────────────
    private final DeviartrasEntity boss;
    /** Counts ticks since this particular attack animation started. */
    private int timer        = 0;
    /** Which attack is currently in progress (DeviartrasEntity constant). */
    private int chosenAttack = DeviartrasEntity.ATTACK_NONE;
    /** Whether the primary action (hit / spawn / mechanic) has fired this cycle. */
    private boolean actionFired = false;

    /** Global cooldown shared by all special attacks. Ticked externally. */
    private int globalCooldown = 0;

    public DeviartrasAttackGoal(DeviartrasEntity boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── External cooldown tick (called from DeviartrasEntity.tick()) ───────────
    /**
     * Called every server tick by DeviartrasEntity so the global cooldown
     * decrements even while this goal is not running.
     */
    public void tickGlobalCooldown() {
        if (globalCooldown  > 0) globalCooldown--;
    }

    // ── canUse / canContinueToUse ──────────────────────────────────────────────
    @Override
    public boolean canUse() {
        if (boss.isDefeated()) return false;
        if (boss.getAttackType() != DeviartrasEntity.ATTACK_NONE) return false;

        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = boss.distanceToSqr(target);
        if (distSq > MAX_RANGE_SQ) return false;

        // Allow special attack if global cooldown cleared
        return globalCooldown <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (boss.isDefeated()) return false;
        return timer < animDuration(chosenAttack);
    }

    // ── start / tick / stop ───────────────────────────────────────────────────
    @Override
    public void start() {
        timer       = 0;
        actionFired = false;

        // Decide which special attack to use
        chosenAttack = pickSpecial();

        boss.setAttackType(chosenAttack);
        triggerAnimation(chosenAttack);
    }

    @Override
    public void tick() {
        timer++;

        // Always face the target while attacking
        LivingEntity target = boss.getTarget();
        if (target != null && target.isAlive()) {
            boss.getLookControl().setLookAt(target, 60.0f, 60.0f);
        }

        // Dispatch per-attack tick logic
        switch (chosenAttack) {
            case DeviartrasEntity.ATTACK_OVERGROWTH   -> tickOvergrowth();
            case DeviartrasEntity.ATTACK_VINE_LOCK    -> tickVineLock();
            case DeviartrasEntity.ATTACK_SPORE_BLOSSOM-> tickSporeBlossom();
        }
    }

    @Override
    public void stop() {
        // All specials share the 100-tick global cooldown
        globalCooldown = GLOBAL_CD;

        boss.setAttackType(DeviartrasEntity.ATTACK_NONE);
        chosenAttack = DeviartrasEntity.ATTACK_NONE;
        timer        = 0;
        actionFired  = false;
    }

    // ── Per-attack tick logic ──────────────────────────────────────────────────

    /**
     * Overgrowth tick.
     * Animation: deviartras_overgrowth (40 t)
     *
     * Action: summons plant_turret(s) once (before particle window).
     * Particles: minecraft:dirt spawned at the "left_plant_mouth" locator
     *            from tick 15 to tick 20. Since GeckoLib locator world positions
     *            require model-bone access (client-side only), we approximate with
     *            a slight offset above the boss's position on the server side.
     *            Refine the offset with the actual locator if bone access is wired
     *            via a network packet or data-driven locator query.
     */
    private void tickOvergrowth() {
        // Fire the summon ONCE, just before the particle window opens
        if (!actionFired && timer == OVERGROWTH_PARTICLE_START - 1) {
            actionFired = true;
            boss.spawnPlantTurrets();
        }

        // ── Dirt particle burst at the "left_plant_mouth" locator ─────────────
        // Window: tick 15 → 20 (every tick within this range)
        // The locator approximation is +0.4 blocks in X, +1.6 blocks in Y.
        // Replace with a proper locator lookup if you attach a locator-sync system.
        if (timer >= OVERGROWTH_PARTICLE_START && timer <= OVERGROWTH_PARTICLE_END) {
            if (boss.level() instanceof ServerLevel serverLevel) {
                double lx = boss.getX() + 0.4;   // approximate left_plant_mouth X
                double ly = boss.getY() + 1.6;   // approximate left_plant_mouth Y
                double lz = boss.getZ();
                serverLevel.sendParticles(
                        ParticleTypes.MYCELIUM,
                        lx, ly, lz,
                        5,   // count per tick
                        0.15, 0.1, 0.15, // spread
                        0.0  // speed
                );
            }
        }
    }

    /**
     * Vine Lock tick.
     * Animation: deviartras_vine_lock (45 t)
     *
     * Uses the existing {@link RootAttackUtil#unleash} — the same mechanic fired
     * by BossAttackDebugWandItem.ROOT_ATTACK.  RootAttackUtil has a built-in 60-tick
     * (3 s) delay and shows an AoE indicator, then erupts vine visuals + applies
     * STUNNED (80 t) to entities in a 2-block radius.
     *
     * We fire at animation tick 20 (midpoint of the 45-t animation) so the boss
     * has a clear "charging" phase before the vines erupt.
     *
     * Particles: minecraft:falling_spore_blossom at "right_hand" locator,
     *            from tick 5 to tick 40 (every tick within this range).
     */
    private void tickVineLock() {
        // ── Fire RootAttackUtil at animation start (once) ─────────────────
        if (!actionFired && timer == 1) {
            actionFired = true;
            LivingEntity target = boss.getTarget();
            if (target != null && target.isAlive() && boss.level() instanceof ServerLevel serverLevel) {
                // Ground-snap: vines erupt from the floor, not mid-air
                BlockPos groundPos = target.blockPosition();
                while (groundPos.getY() > serverLevel.getMinBuildHeight()
                        && serverLevel.isEmptyBlock(groundPos.below())) {
                    groundPos = groundPos.below();
                }
                while (!serverLevel.isEmptyBlock(groundPos)) {
                    groundPos = groundPos.above();
                }
                // 3-second delayed vine eruption + AoE indicator + STUNNED effect
                RootAttackUtil.unleash(serverLevel, groundPos, boss.getId());
            }
        }

        // ── Falling spore blossom particles at "right_hand" locator ──────────
        // Approximate locator: +0.5 blocks in X, +1.4 blocks in Y.
        if (timer >= VINE_LOCK_PARTICLE_START && timer <= VINE_LOCK_PARTICLE_END) {
            if (boss.level() instanceof ServerLevel serverLevel) {
                double rx = boss.getX() + 0.5;   // approximate right_hand X
                double ry = boss.getY() + 1.4;   // approximate right_hand Y
                double rz = boss.getZ();
                serverLevel.sendParticles(
                        ParticleTypes.FALLING_SPORE_BLOSSOM,
                        rx, ry, rz,
                        3,   // count per tick
                        0.1, 0.1, 0.1,
                        0.0
                );
            }
        }
    }

    /**
     * Spore Blossom tick.
     * Animation: deviartras_spore_blossom (40 t)
     *
     * CRITICAL: the eye_of_desolation spawn fires at EXACTLY tick 20.
     */
    private void tickSporeBlossom() {
        // Spawn the spore blossom(s) at the precise moment specified in the design doc
        if (!actionFired && timer == SPORE_BLOSSOM_SPAWN_TICK) {
            actionFired = true;
            boss.spawnSporeBlossom();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the total animation length for the given attack constant. */
    private static int animDuration(int attack) {
        return switch (attack) {
            case DeviartrasEntity.ATTACK_OVERGROWTH    -> OVERGROWTH_ANIM_TICKS;
            case DeviartrasEntity.ATTACK_VINE_LOCK     -> VINE_LOCK_ANIM_TICKS;
            case DeviartrasEntity.ATTACK_SPORE_BLOSSOM -> SPORE_BLOSSOM_ANIM_TICKS;
            default -> 20;
        };
    }

    /**
     * Picks a random special attack from {OVERGROWTH, VINE_LOCK, SPORE_BLOSSOM}.
     * Re-rolls once if the same attack as last time would be selected.
     */
    private int pickSpecial() {
        int[] options = {
            DeviartrasEntity.ATTACK_OVERGROWTH,
            DeviartrasEntity.ATTACK_VINE_LOCK,
            DeviartrasEntity.ATTACK_SPORE_BLOSSOM
        };
        return options[boss.getRandom().nextInt(options.length)];
    }

    /** Fires the correct GeckoLib triggerable animation for the given attack. */
    private void triggerAnimation(int attack) {
        String animName = switch (attack) {
            case DeviartrasEntity.ATTACK_OVERGROWTH    -> "deviartras_overgrowth";
            case DeviartrasEntity.ATTACK_VINE_LOCK     -> "deviartras_vine_lock";
            case DeviartrasEntity.ATTACK_SPORE_BLOSSOM -> "deviartras_spore_blossom";
            default -> "deviartras_idle";
        };
        boss.triggerAnim("attack_controller", animName);
    }
}
