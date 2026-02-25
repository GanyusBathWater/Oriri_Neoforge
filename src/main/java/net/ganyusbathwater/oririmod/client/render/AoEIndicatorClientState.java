package net.ganyusbathwater.oririmod.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AoEIndicatorClientState {

    public static class Indicator {
        public final BlockPos center;
        public final float radius;
        public final int durationTicks;
        public final int argbColor;
        public final long startTick;

        public Indicator(BlockPos center, float radius, int durationTicks, int argbColor, long startTick) {
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.argbColor = argbColor;
            this.startTick = startTick;
        }

        public boolean isExpired(long currentTick) {
            return (currentTick - startTick) > durationTicks;
        }
    }

    private static final Queue<Indicator> ACTIVE_INDICATORS = new ConcurrentLinkedQueue<>();

    public static void addIndicator(BlockPos center, float radius, int durationTicks, int argbColor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            long currentTick = mc.level.getGameTime();
            ACTIVE_INDICATORS.add(new Indicator(center, radius, durationTicks, argbColor, currentTick));
        }
    }

    public static void cleanExpired(long currentTick) {
        ACTIVE_INDICATORS.removeIf(indicator -> indicator.isExpired(currentTick));
    }

    public static Iterable<Indicator> getActiveIndicators() {
        return ACTIVE_INDICATORS;
    }
}
