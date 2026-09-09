package net.ganyusbathwater.oririmod.client.render.world;

import net.minecraft.world.phys.Vec3;

public class TimestopState {
    private static long timestopEndTime = 0;
    private static long timestopStartTime = 0;
    private static Vec3 origin = Vec3.ZERO;

    public static final long EXPANSION_DURATION_MS = 1000; // 1 second expansion
    public static final float MAX_RADIUS = 200f;

    public static void triggerTimestop(long durationMs, Vec3 playerPos) {
        long now = System.currentTimeMillis();
        timestopStartTime = now;
        timestopEndTime = now + durationMs;
        origin = playerPos;
    }

    public static boolean isActive() {
        return System.currentTimeMillis() < timestopEndTime;
    }

    public static float getExpansionRadius() {
        if (!isActive()) return 0f;
        long elapsed = System.currentTimeMillis() - timestopStartTime;
        if (elapsed >= EXPANSION_DURATION_MS) return MAX_RADIUS;
        
        // Smooth ease-out expansion
        float progress = (float) elapsed / EXPANSION_DURATION_MS;
        progress = (float) (1.0 - Math.pow(1.0 - progress, 3)); 
        return progress * MAX_RADIUS;
    }

    public static Vec3 getOrigin() {
        return origin;
    }
}
