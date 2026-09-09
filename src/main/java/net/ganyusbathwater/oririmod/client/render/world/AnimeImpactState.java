package net.ganyusbathwater.oririmod.client.render.world;

public class AnimeImpactState {
    private static long impactEndTime = 0;

    /**
     * Triggers the anime impact invert flash.
     * @param durationMs The duration in milliseconds.
     */
    public static void triggerImpact(long durationMs) {
        impactEndTime = System.currentTimeMillis() + durationMs;
    }

    /**
     * Checks if the impact flash is currently active.
     */
    public static boolean isActive() {
        return System.currentTimeMillis() < impactEndTime;
    }
}
