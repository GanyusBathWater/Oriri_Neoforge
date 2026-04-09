package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.entity.LaserBeamEntity;
import net.ganyusbathwater.oririmod.entity.MagicWaveEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * MagicWaveUtil — factory methods for spawning the three wave attack patterns.
 *
 * <h2>Patterns</h2>
 * <ul>
 *   <li>{@link #spawnCircular}  — 16 waves in all 360°</li>
 *   <li>{@link #spawnCone}      — 7 waves in a widening ±45° fan</li>
 *   <li>{@link #spawnPlainWave} — 3 parallel waves</li>
 * </ul>
 *
 * <p>All patterns also spawn a single spinning magic circle flat on the ground
 * under the caster using the existing LaserBeamUtil clock-orbit system.</p>
 */
public final class MagicWaveUtil {

    /** How far (blocks) each wave spawns from the caster origin. */
    private static final double SPAWN_OFFSET = 2.0;

    private MagicWaveUtil() {}

    // ─────────────────────────────────────────────────────────────────────────
    // CIRCULAR — 360° ripple outward
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Spawns {@code count} waves evenly distributed around the full 360° circle.
     * Also spawns one flat spinning magic circle at caster ground level.
     *
     * @param origin  Caster foot position (Y = ground level)
     * @param count   Number of wave projectiles (suggest 16)
     * @param color   ARGB packed color
     * @param damage  Damage per mob contact
     * @param ownerId Server entity ID of caster (immune to damage)
     */
    public static void spawnCircular(ServerLevel level, Vec3 origin,
                                     int count, int color, float damage, int ownerId) {
        spawnGroundCircle(level, origin, color);

        double angleStep = (2.0 * Math.PI) / count;
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;
            float dx = (float) Math.cos(angle);
            float dz = (float) Math.sin(angle);
            spawnWave(level, origin, dx, dz, color, damage, ownerId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONE — widening fan in the caster's facing direction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Spawns 13 waves in a ±45° fan around the given facing direction.
     * Because all waves travel straight, the fan naturally widens with distance.
     *
     * @param origin     Caster foot position
     * @param facingYaw  Caster's yaw angle in radians (Minecraft convention: 0 = south/+Z)
     */
    public static void spawnCone(ServerLevel level, Vec3 origin, float facingYaw,
                                 int color, float damage, int ownerId) {
        spawnGroundCircle(level, origin, color);

        // 13 waves: every 7.5° from -45° to +45°
        float[] offsetsDeg = { 
            -45f, -37.5f, -30f, -22.5f, -15f, -7.5f, 
            0f, 
            7.5f, 15f, 22.5f, 30f, 37.5f, 45f 
        };
        for (float deg : offsetsDeg) {
            float rad = facingYaw + (float) Math.toRadians(deg);
            float dx  = (float) Math.sin(rad);
            float dz  = (float) Math.cos(rad);
            spawnWave(level, origin, dx, dz, color, damage, ownerId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLAIN WAVE — 3 parallel waves
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Spawns 3 parallel waves in the caster's facing direction, side-by-side.
     *
     * @param facingYaw  Caster's yaw angle in radians
     */
    public static void spawnPlainWave(ServerLevel level, Vec3 origin, float facingYaw,
                                      int color, float damage, int ownerId) {
        spawnGroundCircle(level, origin, color);

        float dx = (float) Math.sin(facingYaw);
        float dz = (float) Math.cos(facingYaw);

        // Right-perpendicular vector for lateral spread
        float rx = -dz;
        float rz =  dx;

        // 3 waves: center, 0.75 left, 0.75 right
        float[] spreads = { 0f, 0.75f, -0.75f };
        for (float spread : spreads) {
            Vec3 lateralOrigin = origin.add(rx * spread, 0, rz * spread);
            spawnWave(level, lateralOrigin, dx, dz, color, damage, ownerId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void spawnWave(ServerLevel level, Vec3 origin,
                                   float dx, float dz,
                                   int color, float damage, int ownerId) {
        MagicWaveEntity wave = ModEntities.MAGIC_WAVE.get().create(level);
        if (wave == null) return;

        // Spawn SPAWN_OFFSET blocks ahead of caster in this wave's direction
        Vec3 spawnPos = origin.add(dx * SPAWN_OFFSET, 0, dz * SPAWN_OFFSET);
        wave.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        wave.setDirection(dx, dz);
        wave.setWaveColor(color);
        wave.setWaveDamage(damage);
        wave.setOwnerId(ownerId);
        level.addFreshEntity(wave);
    }

    /**
     * Spawns a flat, slowly-spinning magic circle on the ground beneath the caster
     * that lasts as long as the waves do (400 ticks / 20 s).
     * Uses the LaserBeam clock-orbit with tiny radius so it renders purely as a circle.
     */
    private static void spawnGroundCircle(ServerLevel level, Vec3 origin, int color) {
        LaserBeamUtil.LaserBeamConfig config = new LaserBeamUtil.LaserBeamConfig(
                origin, origin,
                0.45f,          // width (increased radially by 50% for a larger footprint)
                color,
                400,            // 20 second lifetime
                0f,             // no damage
                0,              // no damage interval
                -1,             // no owner immunity
                0               // no charge time — circle appears instantly
        );

        LaserBeamEntity beam = LaserBeamUtil.unleash(level, config);
        if (beam != null) {
            // Because start=end natively, it points exactly Up (dy=1).
            // This naturally forces the renderer to pitch it -90 degrees exactly flat onto the ground! 
            // It spins completely autonomously via the Renderer without physical orbital movement.
            beam.setSilent(true); // Don't spam ambient noises from the visual anchor
            beam.setCoreHidden(true); // Completely strip beam geometry layer
            beam.setUseWaveCircle(true); // Use the designated wave_circle.png asset
        }
    }
}
