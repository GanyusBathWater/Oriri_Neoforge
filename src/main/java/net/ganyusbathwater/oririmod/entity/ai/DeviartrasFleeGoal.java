package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * DeviartrasFleeGoal — the "Hit & Run" mechanic.
 *
 * Normally dormant. When {@link #triggerFlee()} is called by DeviartrasEntity
 * (after a target stays within 3 blocks for 100 consecutive ticks / 5 seconds),
 * the boss sprints away from the target for 60 ticks (3 seconds) to regain
 * range for its preferred ranged attacks.
 *
 * The goal blocks the MOVE flag while active so other movement goals yield.
 */
public class DeviartrasFleeGoal extends Goal {

    /** Duration of the flee sprint in ticks (3 s). */
    private static final int FLEE_DURATION_TICKS = 60;
    /** Sprint speed multiplier applied during the flee. */
    private static final double SPRINT_SPEED = 0.38D; // ≈ double walk speed

    private final DeviartrasEntity boss;
    /** How many ticks the flee sprint has been running (0 = inactive). */
    private int fleeTicks = 0;
    /** True when an external trigger has requested a flee cycle. */
    private boolean triggered = false;

    public DeviartrasFleeGoal(DeviartrasEntity boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /**
     * Called by DeviartrasEntity.tick() after the 100-tick proximity threshold.
     * The goal will activate on the next canUse() check (next tick).
     */
    public void triggerFlee() {
        triggered = true;
    }

    @Override
    public boolean canUse() {
        if (boss.isDefeated()) return false;
        return triggered;
    }

    @Override
    public boolean canContinueToUse() {
        return fleeTicks < FLEE_DURATION_TICKS && !boss.isDefeated();
    }

    @Override
    public void start() {
        fleeTicks = 0;
        triggered = false;
        boss.setSprinting(true);
    }

    @Override
    public void tick() {
        fleeTicks++;

        LivingEntity target = boss.getTarget();
        if (target == null) return;

        // Calculate a direction vector pointing AWAY from the target
        Vec3 awayDir = boss.position()
                .subtract(target.position())
                .normalize();

        // Move the boss in that direction using the navigation system
        Vec3 fleeTarget = boss.position().add(awayDir.scale(8.0));
        boss.getNavigation().moveTo(
                fleeTarget.x, fleeTarget.y, fleeTarget.z,
                SPRINT_SPEED);

        // Keep facing the target even while running away
        boss.getLookControl().setLookAt(target, 30.0f, 30.0f);
    }

    @Override
    public void stop() {
        fleeTicks = 0;
        boss.setSprinting(false);
        boss.getNavigation().stop();
    }
}
