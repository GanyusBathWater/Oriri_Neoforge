package net.ganyusbathwater.oririmod.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class AoEIndicatorClientState {

    public static class Indicator {
        public final BlockPos center;
        public final float radius;
        public final int durationTicks;
        public final int argbColor;
        public final long startTick;
        // Pre-calculated AABBs for the blocks to render
        public final java.util.List<net.minecraft.world.phys.AABB> cachedAabbs;

        public Indicator(BlockPos center, float radius, int durationTicks, int argbColor, long startTick,
                java.util.List<net.minecraft.world.phys.AABB> cachedAabbs) {
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.argbColor = argbColor;
            this.startTick = startTick;
            this.cachedAabbs = cachedAabbs;
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

            // Pre-calculate the AABBs for the terrain
            java.util.List<net.minecraft.world.phys.AABB> aabbs = new java.util.ArrayList<>();
            int rad = (int) Math.ceil(radius);
            int radSq = rad * rad;

            for (int x = -rad; x <= rad; x++) {
                for (int z = -rad; z <= rad; z++) {
                    if (x * x + z * z > radSq)
                        continue;

                    BlockPos colPos = center.offset(x, 0, z);

                    // Find highest solid block surface within +/- 4 blocks Y
                    BlockPos surfacePos = null;
                    net.minecraft.world.phys.shapes.VoxelShape surfaceShape = null;

                    for (int y = 4; y >= -4; y--) {
                        BlockPos checkPos = colPos.offset(0, y, 0);
                        net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(checkPos);

                        boolean isSolid = !state.getCollisionShape(mc.level, checkPos).isEmpty();
                        boolean isFluid = !state.getFluidState().isEmpty();

                        if (isSolid || isFluid) {
                            surfacePos = checkPos;
                            surfaceShape = state.getShape(mc.level, checkPos);

                            // Fluids might have an empty visual shape if full
                            if (surfaceShape.isEmpty()) {
                                surfaceShape = Shapes.block();
                            }
                            break;
                        }
                    }

                    if (surfacePos != null && surfaceShape != null) {
                        aabbs.add(surfaceShape.bounds().move(surfacePos));
                    }
                }
            }

            ACTIVE_INDICATORS.add(new Indicator(center, radius, durationTicks, argbColor, currentTick, aabbs));
        }
    }

    public static void cleanExpired(long currentTick) {
        ACTIVE_INDICATORS.removeIf(indicator -> indicator.isExpired(currentTick));
    }

    public static Iterable<Indicator> getActiveIndicators() {
        return ACTIVE_INDICATORS;
    }
}
