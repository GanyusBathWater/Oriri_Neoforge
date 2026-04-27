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
        public final java.util.UUID id; // null if not updatable
        public final BlockPos center;
        public final float radius; // For legacy/rendering purposes
        public final int durationTicks;
        public final int argbColor;
        public final long startTick;
        // Pre-calculated AABBs for the blocks to render
        public final java.util.List<net.minecraft.world.phys.AABB> cachedAabbs;

        public Indicator(java.util.UUID id, BlockPos center, float radius, int durationTicks, int argbColor, long startTick,
                java.util.List<net.minecraft.world.phys.AABB> cachedAabbs) {
            this.id = id;
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
        addCircleIndicator(null, center, radius, durationTicks, argbColor);
    }

    public static void addCircleIndicator(java.util.UUID id, BlockPos center, float radius, int durationTicks, int argbColor) {
        int rad = (int) Math.ceil(radius);
        int radSq = rad * rad;
        addCustomIndicator(id, center, rad, durationTicks, argbColor, (pos) -> {
            int dx = pos.getX() - center.getX();
            int dz = pos.getZ() - center.getZ();
            return (dx * dx + dz * dz) <= radSq;
        });
    }

    public static void addLineIndicator(java.util.UUID id, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end, float width, int durationTicks, int argbColor) {
        BlockPos center = BlockPos.containing(start.add(end).scale(0.5));
        int length = (int) Math.ceil(start.distanceTo(end) / 2.0);
        int checkRadius = length + (int) Math.ceil(width);
        
        addCustomIndicator(id, center, checkRadius, durationTicks, argbColor, (pos) -> {
            net.minecraft.world.phys.Vec3 p = new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            net.minecraft.world.phys.Vec3 lineDir = end.subtract(start);
            double lengthSq = lineDir.lengthSqr();
            if (lengthSq == 0) return p.distanceToSqr(start) <= width * width;
            
            double t = Math.max(0, Math.min(1, p.subtract(start).dot(lineDir) / lengthSq));
            net.minecraft.world.phys.Vec3 projection = start.add(lineDir.scale(t));
            double dx = p.x - projection.x;
            double dz = p.z - projection.z;
            return (dx * dx + dz * dz) <= (width * width);
        });
    }

    public static void addDonutIndicator(java.util.UUID id, BlockPos center, float innerRadius, float outerRadius, int durationTicks, int argbColor) {
        int rad = (int) Math.ceil(outerRadius);
        float innerSq = innerRadius * innerRadius;
        float outerSq = outerRadius * outerRadius;
        addCustomIndicator(id, center, rad, durationTicks, argbColor, (pos) -> {
            int dx = pos.getX() - center.getX();
            int dz = pos.getZ() - center.getZ();
            float distSq = dx * dx + dz * dz;
            return distSq >= innerSq && distSq <= outerSq;
        });
    }

    public static void addArcIndicator(java.util.UUID id, BlockPos center, float radius, float startAngleRad, float sweepAngleRad, int durationTicks, int argbColor) {
        int rad = (int) Math.ceil(radius);
        float radSq = radius * radius;
        float sweep = sweepAngleRad;
        float start = startAngleRad;
        if (sweep < 0) {
            start += sweep;
            sweep = -sweep;
        }
        final float finalStart = start;
        final float finalSweep = sweep;
        
        addCustomIndicator(id, center, rad, durationTicks, argbColor, (pos) -> {
            int dx = pos.getX() - center.getX();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dz * dz > radSq) return false;
            
            double angle = Math.atan2(dz, dx);
            double normalized = angle;
            while (normalized < finalStart) normalized += 2 * Math.PI;
            while (normalized >= finalStart + 2 * Math.PI) normalized -= 2 * Math.PI;
            
            return (normalized - finalStart) <= finalSweep;
        });
    }

    public static void addDonutArcIndicator(java.util.UUID id, BlockPos center, float innerRadius, float outerRadius, float startAngleRad, float sweepAngleRad, int durationTicks, int argbColor) {
        int rad = (int) Math.ceil(outerRadius);
        float innerSq = innerRadius * innerRadius;
        float outerSq = outerRadius * outerRadius;
        float sweep = sweepAngleRad;
        float start = startAngleRad;
        if (sweep < 0) {
            start += sweep;
            sweep = -sweep;
        }
        final float finalStart = start;
        final float finalSweep = sweep;
        
        addCustomIndicator(id, center, rad, durationTicks, argbColor, (pos) -> {
            int dx = pos.getX() - center.getX();
            int dz = pos.getZ() - center.getZ();
            float distSq = dx * dx + dz * dz;
            if (distSq < innerSq || distSq > outerSq) return false;
            
            double angle = Math.atan2(dz, dx);
            double normalized = angle;
            while (normalized < finalStart) normalized += 2 * Math.PI;
            while (normalized >= finalStart + 2 * Math.PI) normalized -= 2 * Math.PI;
            
            return (normalized - finalStart) <= finalSweep;
        });
    }

    public static void addCustomIndicator(java.util.UUID id, BlockPos center, int checkRadius, int durationTicks, int argbColor, java.util.function.Predicate<BlockPos> shapePredicate) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        long currentTick = mc.level.getGameTime();

        // If an indicator with this ID already exists, and we want to update it:
        // Actually, if we just remove the old one and add the new one, its startTick resets.
        // We should preserve the startTick if it's an update!
        long startTickToUse = currentTick;
        if (id != null) {
            for (Iterator<Indicator> it = ACTIVE_INDICATORS.iterator(); it.hasNext();) {
                Indicator existing = it.next();
                if (id.equals(existing.id)) {
                    startTickToUse = existing.startTick; // Preserve original start time
                    it.remove();
                    break;
                }
            }
        }

        // Pre-calculate the AABBs for the terrain
        java.util.List<net.minecraft.world.phys.AABB> aabbs = new java.util.ArrayList<>();

        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int z = -checkRadius; z <= checkRadius; z++) {
                BlockPos colPos = center.offset(x, 0, z);
                
                if (!shapePredicate.test(colPos)) continue;

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

        ACTIVE_INDICATORS.add(new Indicator(id, center, checkRadius, durationTicks, argbColor, startTickToUse, aabbs));
    }

    public static void cleanExpired(long currentTick) {
        ACTIVE_INDICATORS.removeIf(indicator -> indicator.isExpired(currentTick));
    }

    public static Iterable<Indicator> getActiveIndicators() {
        return ACTIVE_INDICATORS;
    }
}
