package net.ganyusbathwater.oririmod.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.awt.Color;

/**
 * CircuitryParticleUtil — single entry-point for spawning an angular
 * "cyber-magic motherboard trace" visual effect.
 *
 * <h2>Architecture</h2>
 * Spawns a {@link CircuitryPathEntity} on the <em>client level only</em>.
 * The entity stores 4 static world-space waypoints that form 3 sharp-angled
 * segments (≤ 0.5 blocks total span) and self-destructs after its lifespan.
 * The {@link net.ganyusbathwater.oririmod.client.render.CircuitryPathRenderer}
 * draws it as camera-facing billboard quads with a rapid draw-in animation
 * followed by a smooth fade-out.
 *
 * <h2>Node geometry</h2>
 * The 4 nodes are generated with 90° / 45° angular turns in the XZ and Y
 * axes, randomised per call so each instance looks unique. All offsets are
 * within a 0.5-block radius sphere centered on the given origin.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Cyan (default)
 * CircuitryParticleUtil.spawnCircuitEffect(level, origin, null);
 *
 * // Custom magenta
 * CircuitryParticleUtil.spawnCircuitEffect(level, origin, new Color(255, 0, 200));
 * }</pre>
 *
 * <p><strong>Must only be called on the client side</strong> (e.g. inside an
 * {@code if (level.isClientSide)} block or from a client-only event handler).</p>
 */
public final class CircuitryParticleUtil {

    private CircuitryParticleUtil() {}

    // ── Geometry constants ─────────────────────────────────────────────────────

    /**
     * Step sizes for each angular segment (in blocks).
     * Three segments → four nodes.
     * All values chosen so the total extent stays ≤ 0.5 blocks.
     */
    private static final float[] STEP = {0.15f, 0.10f, 0.12f};

    /**
     * Axis-aligned and 45°-diagonal direction vectors that simulate PCB traces.
     * Each entry is a normalised (dx, dy, dz) triple.
     */
    private static final float[][] DIRECTIONS = {
            // Cardinal XZ
            { 1, 0, 0}, {-1, 0, 0},
            { 0, 0, 1}, { 0, 0,-1},
            // Cardinal Y
            { 0, 1, 0}, { 0,-1, 0},
            // 45° XZ diagonals
            { 0.7071f, 0, 0.7071f}, {-0.7071f, 0, 0.7071f},
            { 0.7071f, 0,-0.7071f}, {-0.7071f, 0,-0.7071f},
            // 45° up/diag
            { 0.7071f, 0.7071f, 0}, {-0.7071f, 0.7071f, 0},
            { 0, 0.7071f, 0.7071f}, { 0, 0.7071f,-0.7071f},
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Spawns one circuitry trace effect centred at {@code origin}.
     *
     * <p>Call this method <strong>only on the client side</strong>.
     *
     * @param level  The client level.  The method is a no-op on a server level.
     * @param origin World-space origin of the trace.
     * @param color  Colour of the trace; {@code null} defaults to bright cyan.
     */
    public static void spawnCircuitEffect(Level level, Vec3 origin, @Nullable Color color) {
        if (!level.isClientSide) return; // safety guard

        // ── Colour ────────────────────────────────────────────────────────────
        float cr, cg, cb;
        if (color == null) {
            cr = 0.0f; cg = 0.95f; cb = 1.0f; // electric cyan
        } else {
            cr = color.getRed()   / 255f;
            cg = color.getGreen() / 255f;
            cb = color.getBlue()  / 255f;
        }

        // ── Generate 4 static waypoints ───────────────────────────────────────
        Vec3[] nodes = buildNodes(origin);

        // ── Spawn instance ────────────────────────────────────────────────────
        net.ganyusbathwater.oririmod.client.render.CircuitryRenderSystem.ACTIVE_EFFECTS.add(
                new net.ganyusbathwater.oririmod.client.render.CircuitryRenderSystem.CircuitryInstance(nodes, cr, cg, cb)
        );
    }

    /**
     * Legacy overload that matches the old {@code spawn(level, origin, color)} signature
     * used by {@link net.ganyusbathwater.oririmod.item.custom.magic.ParticleDebugWandItem}.
     *
     * @see #spawnCircuitEffect(Level, Vec3, Color)
     */
    public static void spawn(Level level, Vec3 origin, @Nullable Color color) {
        spawnCircuitEffect(level, origin, color);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Node generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Produces 4 world-space waypoints with sharp angular turns.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Pick a random starting direction from {@link #DIRECTIONS}.</li>
     *   <li>Walk {@code STEP[0]} in that direction from {@code origin} → node 1.</li>
     *   <li>Force a 90° or 45° turn: pick a direction that is <em>not</em> parallel
     *       to the previous one.</li>
     *   <li>Repeat for node 2 → node 3.</li>
     * </ol>
     * This guarantees angular PCB-like geometry without any curve interpolation.</p>
     *
     * @param origin The world-space start of the trace.
     * @return Array of 4 Vec3 waypoints (index 0 = origin).
     */
    private static Vec3[] buildNodes(Vec3 origin) {
        // Deterministic pseudo-random seed based on world position
        long seed = Double.doubleToLongBits(origin.x) ^
                    Double.doubleToLongBits(origin.y * 31) ^
                    Double.doubleToLongBits(origin.z * 97) ^
                    System.nanoTime();
        java.util.Random rng = new java.util.Random(seed);

        Vec3[] nodes = new Vec3[4];
        nodes[0] = origin;

        int dirIdx = rng.nextInt(DIRECTIONS.length);

        Vec3 cursor = origin;
        for (int seg = 0; seg < 3; seg++) {
            float[] d = DIRECTIONS[dirIdx];
            float step = STEP[seg];
            cursor = cursor.add(d[0] * step, d[1] * step, d[2] * step);
            nodes[seg + 1] = cursor;

            // Choose next direction — must NOT be (anti-)parallel to current.
            // We measure this by checking the absolute dot product.
            int nextDir;
            int attempts = 0;
            do {
                nextDir = rng.nextInt(DIRECTIONS.length);
                attempts++;
            } while (isParallel(DIRECTIONS[dirIdx], DIRECTIONS[nextDir]) && attempts < 20);
            dirIdx = nextDir;
        }

        return nodes;
    }

    /**
     * Returns {@code true} when two direction vectors are parallel or anti-parallel
     * (dot product magnitude ≥ 0.99).
     */
    private static boolean isParallel(float[] a, float[] b) {
        float dot = a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
        return Math.abs(dot) > 0.99f;
    }
}
