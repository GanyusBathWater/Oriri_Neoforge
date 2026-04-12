package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.entity.LaserBeamEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * LaserBeamUtil — the single entry point for spawning laser beam attacks.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Simple beam from player eye to target block
 * LaserBeamUtil.unleash(serverLevel, LaserBeamConfig.simple(eyePos, targetPos, 0xFF_00AAFF));
 *
 * // Fully custom beam
 * LaserBeamEntity beam = LaserBeamUtil.unleash(serverLevel,
 *     new LaserBeamUtil.LaserBeamConfig(start, end,
 *         width   : 1.5f,
 *         color   : 0xFF_FF2200,   // red-orange
 *         duration: 100,           // 5 seconds
 *         damage  : 8.0f,
 *         interval: 10,            // hit every 0.5s
 *         ownerId : boss.getId()));
 *
 * // Orbiting beam — update positions each tick from your own tick handler:
 * double angle = Math.toRadians(tickCount * 6.0); // 6°/tick → full circle in 60 ticks
 * Vec3 beamStart = bossPos;
 * Vec3 beamEnd   = bossPos.add(Math.cos(angle)*12, 0, Math.sin(angle)*12);
 * beam.updateBeamPositions(beamStart, beamEnd);
 *
 * // Ground upward blast
 * LaserBeamUtil.unleash(serverLevel, LaserBeamConfig.upward(groundPos, 20, 0xFF_FFFF00));
 * }</pre>
 */
public final class LaserBeamUtil {

    private LaserBeamUtil() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Config record
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Immutable configuration for a single laser beam instance.
     *
     * @param start            World-space origin of the beam.
     * @param end              World-space tip of the beam.
     * @param width            Visual half-width of the core beam (metres).  Glow layers are drawn at 2× and 3× this value.
     * @param color            ARGB packed color for the core beam (e.g. {@code 0xFF_00AAFF} = solid electric blue).
     * @param durationTicks    How many server ticks the beam lives.
     * @param damage           Damage dealt per damage interval.
     * @param damageInterval   Ticks between each damage application (0 = no damage).
     * @param ownerId          Server entity ID of the caster; they are immune to the beam's damage (-1 = no immunity).
     */
    public record LaserBeamConfig(
            Vec3  start,
            Vec3  end,
            float width,
            int   color,
            int   durationTicks,
            float damage,
            int   damageInterval,
            int   ownerId,
            int   chargeTicks,
            boolean silent
    ) {
        // ── Convenience factories ──────────────────────────────────────────

        /**
         * A simple, sensible beam with a custom color.
         * Width: 0.4, Duration: 60 ticks (3 s), Damage: 5.0, Interval: 10 ticks (0.5 s).
         */
        public static LaserBeamConfig simple(Vec3 start, Vec3 end, int color) {
            return new LaserBeamConfig(start, end, 0.4f, color, 60, 5.0f, 10, -1, 40, false);
        }

        /**
         * A thick, powerful beam — good for boss finishers.
         * Width: 1.5, Duration: 80 ticks, Damage: 12.0, Interval: 8 ticks.
         */
        public static LaserBeamConfig heavy(Vec3 start, Vec3 end, int color, int ownerId) {
            return new LaserBeamConfig(start, end, 1.5f, color, 80, 12.0f, 8, ownerId, 40, false);
        }

        /**
         * A vertical ground-to-sky blast of {@code heightBlocks} blocks.
         * Thin and fast — uses yellow-white color by default.
         */
        public static LaserBeamConfig upward(Vec3 groundPos, double heightBlocks, int color) {
            return new LaserBeamConfig(
                    groundPos,
                    groundPos.add(0, heightBlocks, 0),
                    0.3f, color, 40, 6.0f, 5, -1, 40, false);
        }

        /**
         * A long, thin sniper beam — very narrow, high damage, short duration.
         */
        public static LaserBeamConfig sniper(Vec3 start, Vec3 end, int color, int ownerId) {
            return new LaserBeamConfig(start, end, 0.15f, color, 20, 20.0f, 20, ownerId, 40, false);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Factory method
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Spawns a laser beam on the server with the given configuration.
     *
     * <p>Returns the spawned {@link LaserBeamEntity} so the caller can hold a
     * reference and update the beam positions each tick for dynamic beams
     * (orbiting, tracking, etc.).  If the entity failed to create (very rare),
     * returns {@code null}.
     *
     * @param level  The server level to spawn in.
     * @param config Beam configuration.
     * @return The spawned entity, or {@code null} on failure.
     */
    public static LaserBeamEntity unleash(ServerLevel level, LaserBeamConfig config) {
        LaserBeamEntity beam = ModEntities.LASER_BEAM.get().create(level);
        if (beam == null) return null;

        // Apply configuration and strictly pin to the real world position!
        beam.setPos(config.start().x, config.start().y, config.start().z);
        beam.updateBeamPositions(config.start(), config.end());
        beam.setBeamWidth(config.width());
        beam.setBeamColor(config.color());
        beam.setDurationTicks(config.durationTicks());
        beam.setDamage(config.damage());
        beam.setDamageIntervalTicks(config.damageInterval());
        beam.setOwnerId(config.ownerId());
        beam.setChargeTicks(config.chargeTicks());
        beam.setSilent(config.silent());

        level.addFreshEntity(beam);
        return beam;
    }

    /**
     * Spawns a vertical pillar laser beam that automatically rotates around a center point natively on the server.
     */
    public static LaserBeamEntity orbitingCylinder(ServerLevel level, Vec3 center, float radius, float height, float rotSpeedDegPerTick, int color, int duration, float startAngleRad) {
        // Create a dummy simple config (positions will be immediately overwritten by orbit logic)
        LaserBeamConfig config = new LaserBeamConfig(
                center, center, 0.4f, color, duration, 5.0f, 2, -1, 40, false);
                
        LaserBeamEntity beam = unleash(level, config);
        if (beam != null) {
            beam.setCylinderOrbit(center, radius, height, (float) Math.toRadians(rotSpeedDegPerTick), startAngleRad);
        }
        return beam;
    }

    public static LaserBeamEntity orbitSphere(ServerLevel level, Vec3 center, float radius, float staticPitchRad, float spinSpeedDeg, int color, int durationTicks, float startYawRad, boolean silent) {
        LaserBeamConfig config = new LaserBeamConfig(
                center, center, 0.4f, color, durationTicks, 5.0f, 2, -1, 40, silent);

        LaserBeamEntity beam = unleash(level, config);
        if (beam != null) {
            beam.setSphereOrbit(center, radius, staticPitchRad, (float) Math.toRadians(spinSpeedDeg), startYawRad);
            beam.setSilent(silent);
        }
        return beam;
    }

    /**
     * Spawns a horizontal laser beam that automatically sweeps around a center point natively on the server, like a clock hand.
     */
    public static LaserBeamEntity orbitingClockAngle(ServerLevel level, Vec3 center, float radius, float rotSpeedDegPerTick, int color, int duration, float startAngleRad) {
        // Create a dummy simple config
        LaserBeamConfig config = new LaserBeamConfig(
                center, center, 0.4f, color, duration, 5.0f, 2, -1, 40, false);

        LaserBeamEntity beam = unleash(level, config);
        if (beam != null) {
            beam.setClockOrbit(center, radius, (float) Math.toRadians(rotSpeedDegPerTick), startAngleRad);
        }
        return beam;
    }
}
