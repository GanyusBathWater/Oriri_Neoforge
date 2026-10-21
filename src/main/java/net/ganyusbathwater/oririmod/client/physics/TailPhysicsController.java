package net.ganyusbathwater.oririmod.client.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 3D Physics-Constrained Path Verlet Engine for the Lamia Tail.
 * - Follow-the-leader breadcrumb trail eliminates wall clipping.
 * - Supple inter-segment curvature smoothing (max 7-8 deg per segment) eliminates sharp angles.
 * - Pure upright waist geometry eliminates sideways roll and dislocation.
 * - Bounded height interpolation eliminates sky-climbing and banana curves.
 */
public class TailPhysicsController {

    public static final int NUM_SEGMENTS = 36; // tail5 through tail40
    public static final int NUM_POINTS = NUM_SEGMENTS + 1; // 37 points (0 is anchor, 1..36 are ground tail)

    public static class TailSimulation {
        public final double[] px = new double[NUM_POINTS];
        public final double[] py = new double[NUM_POINTS];
        public final double[] pz = new double[NUM_POINTS];

        public final double[] prevPx = new double[NUM_POINTS];
        public final double[] prevPy = new double[NUM_POINTS];
        public final double[] prevPz = new double[NUM_POINTS];

        public float anchorDeflection = 0.0F;
        public float prevAnchorDeflection = 0.0F;
        public float lastRootYawSign = 1.0F;
        public float lastStationaryTurnSign = 1.0F;
        public float lastBodyYaw = 0.0F;
        public int idleTicks = 0;
        public float idleCoilBlend = 0.0F;
        public float prevIdleCoilBlend = 0.0F;

        private final float[] restLengths = new float[NUM_SEGMENTS];
        private final float[] radii = new float[NUM_POINTS];
        private final float[] wallRadii = new float[NUM_POINTS];
        private final float[] lastBoneRotZ = new float[NUM_SEGMENTS];
        private final float[] lastBoneRotX = new float[NUM_SEGMENTS];

        private final double[] wallPushBuf = new double[2];
        private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        private final BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        private boolean isAirOrPassable(Level level, int x, int y, int z) {
            neighborPos.set(x, y, z);
            if (!level.hasChunkAt(neighborPos)) return true;
            BlockState state = level.getBlockState(neighborPos);
            VoxelShape shape = state.getCollisionShape(level, neighborPos);
            if (shape.isEmpty()) return true;
            return shape.bounds().maxY < 0.25D;
        }

        public int lastTick = -1;
        public double lastEntityX;
        public double lastEntityY;
        public double lastEntityZ;
        public boolean initialized = false;
        public boolean wasPassenger = false;

        public TailSimulation() {
            // Segment lengths: tail5 and tail6 are 4 units (0.25m), tail7..tail40 are 3 units (0.1875m)
            for (int i = 0; i < NUM_SEGMENTS; i++) {
                restLengths[i] = (i <= 1) ? 0.25F : 0.1875F;
            }

            // Vertical floor radii (tail height half-thickness: 4 units = 0.25m -> half is ~0.12m at base, 0.06m at tip)
            for (int i = 0; i < NUM_POINTS; i++) {
                radii[i] = Mth.lerp((float) i / (float) (NUM_POINTS - 1), 0.12F, 0.06F);
            }

            // Horizontal wall radii (matches actual Blockbench mesh width: 10 units = 0.625m -> half is 0.31m at base, 0.10m at tip)
            for (int i = 0; i < NUM_POINTS; i++) {
                wallRadii[i] = Mth.lerp((float) i / (float) (NUM_POINTS - 1), 0.31F, 0.10F);
            }
        }

        public void init(LivingEntity entity) {
            double anchorX, anchorY, anchorZ;
            Vec3 initDir;
            if (entity.isFallFlying()) {
                initDir = entity.getViewVector(1.0F);
                anchorX = entity.getX() - initDir.x * 0.6D;
                anchorY = entity.getY() + 0.3D - initDir.y * 0.6D;
                anchorZ = entity.getZ() - initDir.z * 0.6D;
            } else if (entity.isVisuallySwimming() && entity.isInWater()) {
                initDir = entity.getViewVector(1.0F);
                anchorX = entity.getX() - initDir.x * 0.6D;
                anchorY = entity.getY() + 0.3D - initDir.y * 0.6D;
                anchorZ = entity.getZ() - initDir.z * 0.6D;
            } else {
                float bodyYaw = entity.yBodyRot;
                float rad = (float) Math.toRadians(bodyYaw);
                float backX = Mth.sin(rad);
                float backZ = -Mth.cos(rad);
                initDir = new Vec3(-backX, 0.0D, -backZ);
                float crouchOffset = entity.isVisuallySwimming() ? 0.65F : (entity.isCrouching() ? 0.45F : 0.35F);

                anchorX = entity.getX() + backX * crouchOffset;
                anchorZ = entity.getZ() + backZ * crouchOffset;
                double floorBelow = getFloorHeight(entity.level(), anchorX, entity.getY(), anchorZ);
                anchorY = (floorBelow > Double.NEGATIVE_INFINITY) ? floorBelow + radii[0] : entity.getY() + radii[0];
            }

            double accumDist = 0.0D;
            for (int i = 0; i < NUM_POINTS; i++) {
                if (i > 0) {
                    accumDist += restLengths[i - 1];
                }
                double x, y, z;
                if (entity.isFallFlying() || (entity.isVisuallySwimming() && entity.isInWater())) {
                    x = anchorX - initDir.x * accumDist;
                    y = anchorY - initDir.y * accumDist;
                    z = anchorZ - initDir.z * accumDist;
                } else {
                    x = anchorX - initDir.x * accumDist;
                    z = anchorZ - initDir.z * accumDist;
                    double fl = getFloorHeight(entity.level(), x, anchorY, z);
                    y = (fl > Double.NEGATIVE_INFINITY) ? fl + radii[i] : anchorY;
                }

                px[i] = x;
                py[i] = y;
                pz[i] = z;

                prevPx[i] = x;
                prevPy[i] = y;
                prevPz[i] = z;
            }

            this.lastEntityX = entity.getX();
            this.lastEntityY = entity.getY();
            this.lastEntityZ = entity.getZ();
            this.anchorDeflection = 0.0F;
            this.prevAnchorDeflection = 0.0F;
            this.lastRootYawSign = 1.0F;
            this.lastStationaryTurnSign = 1.0F;
            this.lastBodyYaw = entity.yBodyRot;
            this.idleTicks = 0;
            this.idleCoilBlend = 0.0F;
            this.prevIdleCoilBlend = 0.0F;
            java.util.Arrays.fill(this.lastBoneRotZ, 0.0F);
            java.util.Arrays.fill(this.lastBoneRotX, 0.0F);
            this.initialized = true;
        }

        public void updateTick(LivingEntity entity, boolean isFlying, boolean isSwimming) {
            updateTick(entity, isFlying, isSwimming, entity.isPassenger());
        }

        public void updateTick(LivingEntity entity, boolean isFlying, boolean isSwimming, boolean isPassenger) {
            if (isPassenger) {
                this.wasPassenger = true;
                this.lastEntityX = entity.getX();
                this.lastEntityY = entity.getY();
                this.lastEntityZ = entity.getZ();
                return;
            }

            if (this.wasPassenger) {
                this.wasPassenger = false;
                init(entity);
            }

            Level level = entity.level();

            double distSq = (entity.getX() - lastEntityX) * (entity.getX() - lastEntityX) +
                            (entity.getY() - lastEntityY) * (entity.getY() - lastEntityY) +
                            (entity.getZ() - lastEntityZ) * (entity.getZ() - lastEntityZ);

            if (!initialized || distSq > 64.0D) {
                init(entity);
            }

            // 1. Commit positions for render interpolation
            System.arraycopy(px, 0, prevPx, 0, NUM_POINTS);
            System.arraycopy(py, 0, prevPy, 0, NUM_POINTS);
            System.arraycopy(pz, 0, prevPz, 0, NUM_POINTS);
            this.prevAnchorDeflection = this.anchorDeflection;

            // 2. Update anchor Point 0
            float bodyYaw = entity.yBodyRot;
            float rad = (float) Math.toRadians(bodyYaw);
            float backX = Mth.sin(rad);
            float backZ = -Mth.cos(rad);

            double anchorX, anchorY, anchorZ;
            if (entity.isFallFlying()) {
                Vec3 look = entity.getViewVector(1.0F);
                anchorX = entity.getX() - look.x * 0.6D;
                anchorY = entity.getY() + 0.3D - look.y * 0.6D;
                anchorZ = entity.getZ() - look.z * 0.6D;
                this.anchorDeflection = 0.0F;
            } else if (entity.isVisuallySwimming() && entity.isInWater()) {
                Vec3 look = entity.getViewVector(1.0F);
                anchorX = entity.getX() - look.x * 0.6D;
                anchorY = entity.getY() + 0.3D - look.y * 0.6D;
                anchorZ = entity.getZ() - look.z * 0.6D;
                this.anchorDeflection = 0.0F;
            } else {
                float crawlOffset = entity.isVisuallySwimming() ? 0.65F : (entity.isCrouching() ? 0.45F : 0.35F);

                double uncollidedBackX = backX * crawlOffset;
                double uncollidedBackZ = backZ * crawlOffset;
                anchorX = entity.getX() + uncollidedBackX;
                anchorZ = entity.getZ() + uncollidedBackZ;
                anchorY = entity.getY() + radii[0];

                float targetDeflection = 0.0F;
                if (getWallPush(level, anchorX, anchorY, anchorZ, wallRadii[0], wallPushBuf)) {
                    anchorX += wallPushBuf[0];
                    anchorZ += wallPushBuf[1];

                    double collidedBackX = uncollidedBackX + wallPushBuf[0];
                    double collidedBackZ = uncollidedBackZ + wallPushBuf[1];
                    float uncollidedYaw = (float) Math.toDegrees(Mth.atan2(-uncollidedBackX, uncollidedBackZ));
                    float collidedYaw = (float) Math.toDegrees(Mth.atan2(-collidedBackX, collidedBackZ));
                    targetDeflection = Mth.clamp(Mth.wrapDegrees(collidedYaw - uncollidedYaw), -14.0F, 14.0F);
                }
                this.anchorDeflection = Mth.approach(this.anchorDeflection, targetDeflection, 2.5F);
            }

            px[0] = anchorX;
            py[0] = anchorY;
            pz[0] = anchorZ;

            float limbSpeed = Math.min(entity.walkAnimation.speed(), 1.0F);

            boolean in3DFlightOrWater = isFlying || (isSwimming && (entity.isInWater() || !entity.onGround()));

            // Rotational Drag: when player rotates their body/hips on ground,
            // sweep the tail particles around the player in a natural lagging curve.
            // This prevents the tail from falling behind by 180 degrees, eliminates compression inversion,
            // and ensures the tail curves fluidly along the floor instead of acting like a rigid board.
            float bodyYawDelta = Mth.wrapDegrees(bodyYaw - this.lastBodyYaw);
            if (Math.abs(bodyYawDelta) > 0.05F && !in3DFlightOrWater) {
                double deltaRad = Math.toRadians(bodyYawDelta);
                for (int i = 1; i < NUM_POINTS; i++) {
                    double factor = 0.90D - 0.75D * ((double) i / (double) NUM_POINTS);
                    double angle = deltaRad * factor;
                    double cosA = Math.cos(angle);
                    double sinA = Math.sin(angle);

                    double rx = px[i] - entity.getX();
                    double rz = pz[i] - entity.getZ();

                    px[i] = entity.getX() + (rx * cosA - rz * sinA);
                    pz[i] = entity.getZ() + (rx * sinA + rz * cosA);
                }
            }
            this.lastBodyYaw = bodyYaw;

            // Prolonged idle coiling timer and blend
            this.prevIdleCoilBlend = this.idleCoilBlend;

            // Check flank clearance before coiling: in tight corridors or narrow spaces, the coil cannot form without thrashing against walls
            float flank = (this.lastRootYawSign >= 0.0F) ? 1.0F : -1.0F;
            float perpX = -backZ * flank;
            float perpZ = backX * flank;
            boolean hasCoilClearance = !getWallPush(level, entity.getX() + perpX * 0.7D, entity.getY() + 0.2D, entity.getZ() + perpZ * 0.7D, 0.40F, wallPushBuf);

            boolean isCompletelyStillOnGround = limbSpeed <= 0.05F && Math.abs(bodyYawDelta) < 0.5F && !in3DFlightOrWater && hasCoilClearance;
            if (isCompletelyStillOnGround) {
                this.idleTicks++;
                if (this.idleTicks > 100) {
                    this.idleCoilBlend = Math.min(1.0F, this.idleCoilBlend + 0.025F);
                } else {
                    this.idleCoilBlend = Math.max(0.0F, this.idleCoilBlend - 0.05F);
                }
            } else {
                this.idleTicks = 0;
                this.idleCoilBlend = Math.max(0.0F, this.idleCoilBlend - 0.08F);
            }
            if (in3DFlightOrWater) {
                Vec3 deltaMove = entity.getDeltaMovement();
                double moveSpeed = deltaMove.length();
                int iterations = (entity.isFallFlying() && moveSpeed > 0.8D) ? 2 : 1;
                for (int iter = 0; iter < iterations; iter++) {
                    for (int i = 1; i < NUM_POINTS; i++) {
                        double dx = px[i] - px[i - 1];
                        double dy = py[i] - py[i - 1];
                        double dz = pz[i] - pz[i - 1];
                        double rest = restLengths[i - 1];
                        double dist3D = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist3D > rest) {
                            px[i] = px[i - 1] + (dx / dist3D) * rest;
                            py[i] = py[i - 1] + (dy / dist3D) * rest;
                            pz[i] = pz[i - 1] + (dz / dist3D) * rest;
                        }
                    }
                }
            } else if (limbSpeed > 0.05F) {
                // Moving on ground:
                // When moving backwards (towards player's back), shift the tail points along the backward displacement
                // to prevent compression buckling, division by near-zero, and tail inversion
                double moveDeltaX = entity.getX() - this.lastEntityX;
                double moveDeltaZ = entity.getZ() - this.lastEntityZ;
                double backDot = moveDeltaX * backX + moveDeltaZ * backZ;
                if (backDot > 0.001D) {
                    for (int i = 1; i < NUM_POINTS; i++) {
                        px[i] += moveDeltaX;
                        pz[i] += moveDeltaZ;
                    }
                }

                // Follow-the-leader path trailing
                for (int i = 1; i < NUM_POINTS; i++) {
                    double dx = px[i] - px[i - 1];
                    double dz = pz[i] - pz[i - 1];
                    double hDist = Math.sqrt(dx * dx + dz * dz);
                    double rest = restLengths[i - 1];

                    double segX, segZ;
                    if (hDist < 0.05D) {
                        segX = px[i - 1] + backX * rest;
                        segZ = pz[i - 1] + backZ * rest;
                    } else {
                        segX = px[i - 1] + (dx / hDist) * rest;
                        segZ = pz[i - 1] + (dz / hDist) * rest;
                    }

                    double uncollidedDx = segX - px[i - 1];
                    double uncollidedDz = segZ - pz[i - 1];
                    float baseYaw = (float) Math.toDegrees(Mth.atan2(-uncollidedDx, uncollidedDz));

                    // Smooth wall collision: push out from walls without exceeding joint angle capacity
                    if (getWallPush(level, segX, py[i], segZ, wallRadii[i], wallPushBuf)) {
                        double pushedX = segX + wallPushBuf[0];
                        double pushedZ = segZ + wallPushBuf[1];

                        double curDx = pushedX - px[i - 1];
                        double curDz = pushedZ - pz[i - 1];
                        float deflectedYaw = (float) Math.toDegrees(Mth.atan2(-curDx, curDz));

                        // Clamp wall deflection to the physical joint limit (+/- 14.0 deg)
                        // If corridor is too tight or angle too sharp, joint cannot bend further and gracefully gives way (clips)
                        float wallAngleDelta = Mth.clamp(Mth.wrapDegrees(deflectedYaw - baseYaw), -14.0F, 14.0F);
                        float finalYaw = Mth.wrapDegrees(baseYaw + wallAngleDelta);

                        float finalRad = (float) Math.toRadians(finalYaw);
                        segX = px[i - 1] - Mth.sin(finalRad) * rest;
                        segZ = pz[i - 1] + Mth.cos(finalRad) * rest;
                    }

                    px[i] = segX;
                    pz[i] = segZ;

                    applyTerrainAnd3D(level, i, rest);
                }
            } else {
                // Stationary on ground: Supple serpentine angle propagation
                // 1. Capture current segment orientations before moving particles
                float[] curSegYaw = new float[NUM_SEGMENTS];
                for (int i = 0; i < NUM_SEGMENTS; i++) {
                    double dx = px[i + 1] - px[i];
                    double dz = pz[i + 1] - pz[i];
                    curSegYaw[i] = (float) Math.toDegrees(Mth.atan2(-dx, dz));
                }

                // 2. Propagate angle easing, prolonged idle coiling, and wall collision down the spine segment-by-segment
                float parentYaw = bodyYaw + 180.0F + this.anchorDeflection;
                flank = (this.lastRootYawSign >= 0.0F) ? 1.0F : -1.0F;
                float smoothCoilBlend = this.idleCoilBlend * this.idleCoilBlend * (3.0F - 2.0F * this.idleCoilBlend);

                for (int i = 0; i < NUM_SEGMENTS; i++) {
                    double rest = restLengths[i];
                    float diff = Mth.wrapDegrees(parentYaw - curSegYaw[i]);

                    // Hysteresis on root segment turning to prevent rapid sign alternation near 180 deg
                    if (i == 0) {
                        if (Math.abs(diff) < 150.0F) {
                            if (Math.abs(diff) > 1.0F) {
                                this.lastStationaryTurnSign = Math.signum(diff);
                            }
                        } else {
                            if (this.lastStationaryTurnSign > 0.0F && diff < 0.0F) {
                                diff = 180.0F;
                            } else if (this.lastStationaryTurnSign < 0.0F && diff > 0.0F) {
                                diff = -180.0F;
                            }
                        }
                    }

                    // Organic easing: taper turn rate from base (0.8 deg/tick) down to tip (0.08 deg/tick)
                    float maxTurn = Mth.lerp((float) i / (float) NUM_SEGMENTS, 0.8F, 0.08F);
                    // Deadzone: if bend is under 1.0 deg, do not straighten at all (preserves natural resting curves)
                    float turn = (Math.abs(diff) < 1.0F) ? 0.0F : Mth.clamp(diff * 0.025F, -maxTurn, maxTurn);
                    float normalTargetYaw = Mth.wrapDegrees(curSegYaw[i] + turn);

                    // Coiled target yaw for prolonged idle: concentric loop around the flank
                    float coilSegBend;
                    if (i < 4) coilSegBend = flank * (8.0F + i * 1.5F);
                    else if (i < 15) coilSegBend = flank * 15.0F;
                    else if (i < 28) coilSegBend = flank * 18.0F;
                    else coilSegBend = flank * 14.0F;

                    float coilTargetYaw = Mth.wrapDegrees(parentYaw - coilSegBend);
                    float targetYaw = (smoothCoilBlend > 0.001F) ? Mth.rotLerp(smoothCoilBlend, normalTargetYaw, coilTargetYaw) : normalTargetYaw;

                    // Tentative uncollided position for point i + 1
                    float uncollidedYaw = targetYaw;
                    float sRad = (float) Math.toRadians(uncollidedYaw);
                    double candX = px[i] - Mth.sin(sRad) * rest;
                    double candZ = pz[i] + Mth.cos(sRad) * rest;

                    // Push out from walls with joint-constrained deflection
                    if (getWallPush(level, candX, py[i + 1], candZ, wallRadii[i + 1], wallPushBuf)) {
                        double pushedX = candX + wallPushBuf[0];
                        double pushedZ = candZ + wallPushBuf[1];

                        double curDx = pushedX - px[i];
                        double curDz = pushedZ - pz[i];
                        float deflectedYaw = (float) Math.toDegrees(Mth.atan2(-curDx, curDz));

                        // Limit wall deflection angle to the maximum joint angle (+/- 14.0 deg)
                        // If angle is higher, the joint cannot bend further and gracefully gives way (clips)
                        float wallAngleDelta = Mth.clamp(Mth.wrapDegrees(deflectedYaw - uncollidedYaw), -14.0F, 14.0F);
                        targetYaw = Mth.wrapDegrees(uncollidedYaw + wallAngleDelta);

                        float finalRad = (float) Math.toRadians(targetYaw);
                        candX = px[i] - Mth.sin(finalRad) * rest;
                        candZ = pz[i] + Mth.cos(finalRad) * rest;
                    }

                    px[i + 1] = candX;
                    pz[i + 1] = candZ;

                    // Propagate uncollided spine orientation to prevent local wall collisions from accumulating down the spine
                    parentYaw = uncollidedYaw;

                    applyTerrainAnd3D(level, i + 1, rest);
                }
            }

            this.lastEntityX = entity.getX();
            this.lastEntityY = entity.getY();
            this.lastEntityZ = entity.getZ();
        }

        /**
         * Detects horizontal wall penetration against solid voxel collision shapes.
         * Returns true if a horizontal pushout was applied, filling pushOut with (dx, dz).
         * Uses getFloorHeight to guarantee floors are NEVER mistaken for walls.
         */
        private boolean getWallPush(Level level, double x, double y, double z, float radius, double[] pushOut) {
            pushOut[0] = 0.0D;
            pushOut[1] = 0.0D;
            if (level == null) return false;

            double floorY = getFloorHeight(level, x, y, z);
            double minWallY = (floorY > Double.NEGATIVE_INFINITY) ? floorY + 0.15D : y - 0.2D;
            double maxWallY = (floorY > Double.NEGATIVE_INFINITY) ? floorY + 0.35D : y + 0.25D;

            int bx = Mth.floor(x);
            int by = Mth.floor(y);
            int bz = Mth.floor(z);

            double accPushX = 0.0D;
            double accPushZ = 0.0D;
            int collCount = 0;

            for (int cy = by - 1; cy <= by + 1; cy++) {
                for (int cx = bx - 1; cx <= bx + 1; cx++) {
                    for (int cz = bz - 1; cz <= bz + 1; cz++) {
                        mutablePos.set(cx, cy, cz);
                        if (!level.hasChunkAt(mutablePos)) continue;

                        BlockState state = level.getBlockState(mutablePos);
                        VoxelShape shape = state.getCollisionShape(level, mutablePos);
                        if (shape.isEmpty()) continue;

                        AABB box = shape.bounds().move(mutablePos);

                        // Ignore blocks that are floor (top surface at or below minWallY)
                        if (box.maxY <= minWallY) continue;
                        // Ignore ceilings high above
                        if (box.minY >= maxWallY) continue;

                        // Horizontal closest point on block bounding box
                        double closeX = Mth.clamp(x, box.minX, box.maxX);
                        double closeZ = Mth.clamp(z, box.minZ, box.maxZ);

                        double diffX = x - closeX;
                        double diffZ = z - closeZ;
                        double distSq = diffX * diffX + diffZ * diffZ;

                        if (distSq > 1.0E-8D) {
                            if (distSq < (radius * radius)) {
                                double dist = Math.sqrt(distSq);
                                double pen = radius - dist;
                                accPushX += (diffX / dist) * pen;
                                accPushZ += (diffZ / dist) * pen;
                                collCount++;
                            }
                        } else {
                            // Inside block: find nearest air face
                            double dWest  = isAirOrPassable(level, cx - 1, cy, cz) ? (x - box.minX) : Double.MAX_VALUE;
                            double dEast  = isAirOrPassable(level, cx + 1, cy, cz) ? (box.maxX - x) : Double.MAX_VALUE;
                            double dNorth = isAirOrPassable(level, cx, cy, cz - 1) ? (z - box.minZ) : Double.MAX_VALUE;
                            double dSouth = isAirOrPassable(level, cx, cy, cz + 1) ? (box.maxZ - z) : Double.MAX_VALUE;

                            double minD = Math.min(Math.min(dWest, dEast), Math.min(dNorth, dSouth));
                            double nx = 0.0D, nz = 0.0D;

                            if (minD < Double.MAX_VALUE) {
                                if (minD == dWest)       { nx = -1.0D; nz = 0.0D; }
                                else if (minD == dEast)  { nx = 1.0D;  nz = 0.0D; }
                                else if (minD == dNorth) { nx = 0.0D;  nz = -1.0D; }
                                else if (minD == dSouth) { nx = 0.0D;  nz = 1.0D; }
                            } else {
                                double toRootX = px[0] - x;
                                double toRootZ = pz[0] - z;
                                double rLen = Math.sqrt(toRootX * toRootX + toRootZ * toRootZ);
                                if (rLen > 1.0E-4D) {
                                    nx = toRootX / rLen;
                                    nz = toRootZ / rLen;
                                    minD = 0.3D;
                                }
                            }

                            double pen = minD + radius;
                            accPushX += nx * pen;
                            accPushZ += nz * pen;
                            collCount++;
                        }
                    }
                }
            }

            if (collCount > 0) {
                double pushLenSq = accPushX * accPushX + accPushZ * accPushZ;
                if (pushLenSq > 1.0E-6D) {
                    double pushLen = Math.sqrt(pushLenSq);
                    double maxGive = 0.30D;
                    double actualPush = Math.min(pushLen, maxGive);
                    double scale = actualPush / pushLen;

                    pushOut[0] = accPushX * scale;
                    pushOut[1] = accPushZ * scale;
                    return true;
                }
            }

            return false;
        }

        private void applyTerrainAnd3D(Level level, int i, double rest) {
            // Sample terrain height at point (px[i], pz[i])
            double floorY = getFloorHeight(level, px[i], py[i], pz[i]);

            if (floorY > Double.NEGATIVE_INFINITY) {
                // Floor detected: smoothly drape to floor height, limited by max segment drop
                double targetY = floorY + radii[i];
                double maxDrop = py[i - 1] - (rest * 0.90D);
                double maxRise = py[i - 1] + (rest * 0.90D);
                py[i] = Mth.clamp(targetY, maxDrop, maxRise);
            } else {
                // Off a cliff / in air: drop under gravity, constrained by segment reach
                double maxDrop = py[i - 1] - (rest * 0.90D);
                py[i] = Math.max(py[i] - 0.12D, maxDrop);
            }

            // Enforce 3D distance limit so tail cannot stretch across cliffs
            double d3x = px[i] - px[i - 1];
            double d3y = py[i] - py[i - 1];
            double d3z = pz[i] - pz[i - 1];
            double d3 = Math.sqrt(d3x * d3x + d3y * d3y + d3z * d3z);
            if (d3 > rest * 1.02D) {
                double factor = rest / d3;
                px[i] = px[i - 1] + d3x * factor;
                py[i] = py[i - 1] + d3y * factor;
                pz[i] = pz[i - 1] + d3z * factor;
            }
        }

        public void applyToBones(Function<String, GeoBone> boneFinder,
                                 LivingEntity entity,
                                 float partialTick,
                                 boolean isChoking,
                                 boolean isSleeping,
                                 boolean isFlying,
                                 boolean isSwimming) {
            applyToBones(boneFinder, entity, partialTick, isChoking, isSleeping, isFlying, isSwimming,
                    entity.isPassenger(), entity.getVehicle());
        }

        public void applyToBones(Function<String, GeoBone> boneFinder,
                                 LivingEntity entity,
                                 float partialTick,
                                 boolean isChoking,
                                 boolean isSleeping,
                                 boolean isFlying,
                                 boolean isSwimming,
                                 boolean isPassenger,
                                 net.minecraft.world.entity.Entity vehicle) {
            float ageInTicks = entity.tickCount + partialTick;

            // Scripted override: Riding Mount or Vehicle
            if (isPassenger) {
                boolean isLivingMount = (vehicle instanceof LivingEntity);

                // Determine gallop sway if riding a living mount
                float gallopSway = 0.0F;
                if (isLivingMount && vehicle instanceof LivingEntity livingVehicle) {
                    float speed = livingVehicle.walkAnimation.speed(partialTick);
                    float pos = livingVehicle.walkAnimation.position(partialTick);
                    gallopSway = (float) Math.sin(pos * 0.85F) * 3.5F * Math.min(speed, 1.0F);
                }

                // 1. Segments 1 to 4: Forward lap transition at seat level (prevent dropping 0.75m through vehicle floor)
                GeoBone t1 = boneFinder.apply("tail1");
                if (t1 != null) {
                    t1.setRotX((float) Math.toRadians(75.0F));
                    t1.setRotY(0.0F);
                    t1.setRotZ((float) Math.toRadians(-22.0F + gallopSway * 0.2F));
                    t1.setPosY(0.0F);
                }
                GeoBone t2 = boneFinder.apply("tail2");
                if (t2 != null) {
                    t2.setRotX((float) Math.toRadians(12.0F));
                    t2.setRotY(0.0F);
                    t2.setRotZ((float) Math.toRadians(58.0F));
                    t2.setPosY(0.0F);
                }
                GeoBone t3 = boneFinder.apply("tail3");
                if (t3 != null) {
                    t3.setRotX(0.0F);
                    t3.setRotY(0.0F);
                    t3.setRotZ((float) Math.toRadians(58.0F));
                    t3.setPosY(0.0F);
                }
                GeoBone t4 = boneFinder.apply("tail4");
                if (t4 != null) {
                    t4.setRotX(0.0F);
                    t4.setRotY(0.0F);
                    t4.setRotZ((float) Math.toRadians(58.0F));
                    t4.setPosY(0.0F);
                }

                // 2. Segments 5 to 40: Centered concentric lap coil resting directly on vehicle seat/floor
                for (int i = 5; i <= 40; i++) {
                    GeoBone bone = boneFinder.apply("tail" + i);
                    if (bone == null) continue;

                    int segIdx = i - 5;
                    float stepYaw = 31.0F + segIdx * 0.12F;

                    // Add subtle breathing undulation across the coil
                    float coilBreathe = (float) Math.sin(ageInTicks * 0.08F + segIdx * 0.12F) * 0.7F;
                    float segmentSway = gallopSway * (float) (segIdx) / 40.0F;

                    // Tail tip twitch (tail35..tail40)
                    float tipTwitch = 0.0F;
                    if (i >= 35) {
                        tipTwitch = (float) Math.sin(ageInTicks * 0.18F + i * 0.5F) * 2.5F;
                    }

                    float finalYaw = stepYaw + coilBreathe + segmentSway + tipTwitch;

                    bone.setRotZ((float) Math.toRadians(finalYaw));
                    bone.setRotX(0.0F);
                    bone.setRotY(0.0F);
                    bone.setPosY(0.0F);
                }
                return;
            }

            // Scripted override: Sleeping (Waterfall cascade from bed to floor)
            if (isSleeping) {
                // 1. Segments 1 to 3: Lie flat on mattress along length of bed towards footboard
                GeoBone t1 = boneFinder.apply("tail1");
                if (t1 != null) {
                    t1.setRotX(0.0F);
                    t1.setRotY(0.0F);
                    t1.setRotZ((float) Math.toRadians(-8.0F));
                    t1.setPosY(0.0F);
                }
                GeoBone t2 = boneFinder.apply("tail2");
                if (t2 != null) {
                    t2.setRotX(0.0F);
                    t2.setRotY(0.0F);
                    t2.setRotZ((float) Math.toRadians(-8.0F));
                    t2.setPosY(0.0F);
                }
                GeoBone t3 = boneFinder.apply("tail3");
                if (t3 != null) {
                    t3.setRotX(0.0F);
                    t3.setRotY(0.0F);
                    t3.setRotZ((float) Math.toRadians(-8.0F));
                    t3.setPosY(0.0F);
                }

                // 2. Segments 4 to 6: Waterfall drape cascading over the foot of the bed down to the floor
                GeoBone t4 = boneFinder.apply("tail4");
                if (t4 != null) {
                    t4.setRotX((float) Math.toRadians(-50.0F));
                    t4.setRotY(0.0F);
                    t4.setRotZ(0.0F);
                    t4.setPosY(0.0F);
                }
                GeoBone t5 = boneFinder.apply("tail5");
                if (t5 != null) {
                    t5.setRotX(0.0F);
                    t5.setRotY(0.0F);
                    t5.setRotZ(0.0F);
                    t5.setPosY(0.0F);
                }
                GeoBone t6 = boneFinder.apply("tail6");
                if (t6 != null) {
                    t6.setRotX(0.0F);
                    t6.setRotY(0.0F);
                    t6.setRotZ(0.0F);
                    t6.setPosY(0.0F);
                }

                // 3. Segment 7: Levels off onto the floor
                GeoBone t7 = boneFinder.apply("tail7");
                if (t7 != null) {
                    t7.setRotX((float) Math.toRadians(50.0F));
                    t7.setRotY(0.0F);
                    t7.setRotZ(0.0F);
                    t7.setPosY(0.0F);
                }

                // 4. Segments 8 to 40: Flat concentric resting coil on the bedroom floor at foot of bed
                for (int i = 8; i <= 40; i++) {
                    GeoBone bone = boneFinder.apply("tail" + i);
                    if (bone == null) continue;

                    int segIdx = i - 8;
                    float stepYaw = 34.0F + segIdx * 0.15F;
                    float sleepBreathe = (float) Math.sin(ageInTicks * 0.05F + segIdx * 0.08F) * 0.5F;
                    float tipTwitch = (i >= 36) ? (float) Math.sin(ageInTicks * 0.1F + i) * 1.5F : 0.0F;

                    float finalYaw = stepYaw + sleepBreathe + tipTwitch;

                    bone.setRotZ((float) Math.toRadians(finalYaw));
                    bone.setRotX(0.0F);
                    bone.setRotY(0.0F);
                    bone.setPosY(0.0F);
                }
                return;
            }

            // Scripted override: Mob Chokehold
            if (isChoking) {
                for (int i = 1; i <= 40; i++) {
                    GeoBone bone = boneFinder.apply("tail" + i);
                    if (bone == null) continue;
                    if (i <= 4) {
                        float initRotX = bone.getInitialSnapshot().getRotX();
                        float rotX = (entity instanceof net.minecraft.world.entity.player.Player) ? -initRotX : initRotX;
                        bone.setRotX(rotX);
                        bone.setRotY(bone.getInitialSnapshot().getRotY());
                        bone.setRotZ(bone.getInitialSnapshot().getRotZ());
                    } else if (i <= 5) {
                        bone.setRotX((float) Math.toRadians(-8.0F));
                        bone.setRotZ(0.0F);
                    } else if (i <= 10) {
                        bone.setRotZ((float) Math.toRadians(-36.0F));
                    } else if (i <= 35) {
                        bone.setRotZ((float) Math.toRadians(-24.0F));
                        bone.setRotX((float) Math.toRadians(-3.5F));
                    } else {
                        float tipTwitch = (float) Math.sin(ageInTicks * 0.5F + i) * 3.5F;
                        bone.setRotZ((float) Math.toRadians(-18.0F + tipTwitch));
                    }
                }
                return;
            }

            // Sub-tick render positions with lateral slither undulation
            double[] rx = new double[NUM_POINTS];
            double[] ry = new double[NUM_POINTS];
            double[] rz = new double[NUM_POINTS];

            for (int i = 0; i < NUM_POINTS; i++) {
                rx[i] = Mth.lerp(partialTick, prevPx[i], px[i]);
                ry[i] = Mth.lerp(partialTick, prevPy[i], py[i]);
                rz[i] = Mth.lerp(partialTick, prevPz[i], pz[i]);
            }

            float limbSwing = entity.walkAnimation.position(partialTick);
            float limbSpeed = Math.min(entity.walkAnimation.speed(partialTick), 1.0F);

            float crawlAmount = 0.0F;
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                crawlAmount = player.getSwimAmount(partialTick);
                if (player.isFallFlying()) {
                    float fallFlyingTicks = (float) player.getFallFlyingTicks() + partialTick;
                    crawlAmount = Math.max(crawlAmount, Mth.clamp(fallFlyingTicks * fallFlyingTicks / 100.0F, 0.0F, 1.0F));
                } else if (player.isVisuallySwimming() && crawlAmount < 0.01F) {
                    crawlAmount = 1.0F;
                }
            } else {
                if (entity.isFallFlying() || entity.isVisuallySwimming() || isSwimming) {
                    crawlAmount = 1.0F;
                }
            }

            // Add travelling sinusoidal serpentine wave when moving
            if (limbSpeed > 0.05F) {
                Level level = entity.level();
                float crawlDampen = 1.0F - (crawlAmount * 0.70F);
                for (int i = 1; i < NUM_POINTS; i++) {
                    double dx = rx[i] - rx[i - 1];
                    double dz = rz[i] - rz[i - 1];
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 1.0E-4D) {
                        double nx = -dz / dist;
                        double nz = dx / dist;

                        float wave = (float) Math.sin(limbSwing * 0.45F - (i * 0.22F));
                        float amp = 0.035F * (1.0F + (float) i / (float) NUM_POINTS) * limbSpeed * crawlDampen;

                        double testX = rx[i] + nx * wave * amp;
                        double testZ = rz[i] + nz * wave * amp;

                        // If displaced point enters a wall, dampen wave amplitude against wall instead of violently shoving
                        if (getWallPush(level, testX, ry[i], testZ, wallRadii[i], wallPushBuf)) {
                            amp *= 0.20F;
                            rx[i] += nx * wave * amp;
                            rz[i] += nz * wave * amp;
                        } else {
                            rx[i] = testX;
                            rz[i] = testZ;
                        }
                    }
                }
            }

            float currentBodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            float refBackYaw = currentBodyYaw + 180.0F;

            // Interpolate clean wall deflection on anchor Point 0
            float renderAnchorDeflection = Mth.rotLerp(partialTick, this.prevAnchorDeflection, this.anchorDeflection);

            // 1. Segments 1 to 4: The vertical waist curve connecting torso to floor
            // When standing, applies the full -90 degree downward curve from torso to floor.
            // When crawling, swimming, or elytra flying, waist straightens out (0 degree bend) flush with torso.
            float waistBendFactor = 1.0F - crawlAmount;

            for (int i = 1; i <= 4; i++) {
                GeoBone bone = boneFinder.apply("tail" + i);
                if (bone == null) continue;

                // Invert snapshot rotation for Player cosmetic (where root/waist was modeled angled backwards -90 deg),
                // but preserve forward rotation for Lamia mob (where waist was already modeled curving downwards +90 deg).
                // Multiplying by waistBendFactor dynamically uncurls the waist into a straight spine when crawling.
                float initRotX = bone.getInitialSnapshot().getRotX();
                float rotX = (entity instanceof net.minecraft.world.entity.player.Player) ? -initRotX : initRotX;
                bone.setRotX(rotX * waistBendFactor);
                bone.setRotY(bone.getInitialSnapshot().getRotY() * waistBendFactor);
                float waistYaw = (bone.getInitialSnapshot().getRotZ() * waistBendFactor) + (float) Math.toRadians(renderAnchorDeflection * (i / 4.0F));
                bone.setRotZ(waistYaw);
                bone.setPosY(0.0F);
            }

            // 2. Segments 5 to 40: Orientation alignment
            boolean in3DFlight = entity.isFallFlying() || (isSwimming && entity.isInWater());
            if (in3DFlight) {
                // 3D Flight & Aquatic Swimming Solver: Local Model Space Projection
                // Vanilla PlayerRenderer setupRotations already applies Axis.YP(180 - bodyYaw) and Axis.XP(-90 - viewXRot) to PoseStack.
                // In PoseStack local space, the straight trailing direction is identically (0, -1, 0) for ANY pitch or yaw.
                // Projecting world segment vectors by inverse PoseStack rotation eliminates all camera tilting glitches and right-angle kinks.
                float viewXRot = (entity instanceof net.minecraft.world.entity.player.Player p) ? p.getViewXRot(partialTick) : entity.getXRot();
                float fallFlyingTicks = (entity instanceof net.minecraft.world.entity.player.Player p) ? (float) p.getFallFlyingTicks() + partialTick : 100.0F;
                float flightBlend = entity.isFallFlying() ? Mth.clamp(fallFlyingTicks * fallFlyingTicks / 100.0F, 0.0F, 1.0F) : 1.0F;
                float effectivePitch = flightBlend * viewXRot;

                float invYawRad = (float) Math.toRadians(-(180.0F - currentBodyYaw));
                double cosY = Math.cos(invYawRad);
                double sinY = Math.sin(invYawRad);

                float invPitchRad = (float) Math.toRadians(90.0F * flightBlend + effectivePitch);
                double cosP = Math.cos(invPitchRad);
                double sinP = Math.sin(invPitchRad);

                Vec3 vel = entity.getDeltaMovement();
                double flySpeed = vel.length();
                float speedRatio = (float) Mth.clamp((flySpeed - 0.2D) / 1.0D, 0.0D, 1.0D);

                float prevLocalPitch = 0.0F;
                float prevLocalYaw = 0.0F;

                for (int i = 5; i <= 40; i++) {
                    GeoBone bone = boneFinder.apply("tail" + i);
                    if (bone == null) continue;

                    int segIdx = i - 5;
                    double dx = rx[segIdx + 1] - rx[segIdx];
                    double dy = ry[segIdx + 1] - ry[segIdx];
                    double dz = rz[segIdx + 1] - rz[segIdx];

                    // 1. Inverse Yaw around Y
                    double dx1 = dx * cosY + dz * sinY;
                    double dy1 = dy;
                    double dz1 = -dx * sinY + dz * cosY;

                    // 2. Inverse Pitch around X
                    double dx2 = dx1;
                    double dy2 = dy1 * cosP - dz1 * sinP;
                    double dz2 = dy1 * sinP + dz1 * cosP;

                    double dLen = Math.sqrt(dx2 * dx2 + dy2 * dy2 + dz2 * dz2);
                    if (dLen < 1.0E-5D) {
                        bone.setRotX(bone.getInitialSnapshot().getRotX());
                        bone.setRotY(bone.getInitialSnapshot().getRotY());
                        bone.setRotZ(bone.getInitialSnapshot().getRotZ());
                        continue;
                    }

                    // In local model space, the rest vector points along -Y: (0, -1, 0)
                    // dz2 is local pitch (tilt up/down relative to torso spine)
                    // -dx2 is local yaw (tilt left/right relative to torso spine)
                    float targetLocalPitch = (float) Math.toDegrees(Mth.atan2(dz2, Math.max(-dy2, 1.0E-4D)));
                    float targetLocalYaw = (float) Math.toDegrees(Mth.atan2(-dx2, Math.max(-dy2, 1.0E-4D)));

                    float deltaPitch = targetLocalPitch - prevLocalPitch;
                    float deltaYaw = targetLocalYaw - prevLocalYaw;

                    // Aerodynamic Streamer Wave Enhancement
                    float extraZ = 0.0F;
                    if (isSwimming && entity.isInWater()) {
                        float swimWave = (float) Math.sin(ageInTicks * 0.25F - (i * 0.2F));
                        extraZ = swimWave * (2.0F + (4.0F * ((float) i / 40.0F)));
                    } else if (entity.isFallFlying()) {
                        float slowWave = (float) Math.sin(ageInTicks * 0.25F - (i * 0.15F)) * 1.8F;
                        float tautFactor = 1.0F - (speedRatio * 0.75F);
                        float flutterFreq = ageInTicks * (0.6F + 0.8F * speedRatio) - (i * 0.3F);
                        float flutterAmp = 0.6F * (float) Math.sin(flutterFreq) * ((float) i / 40.0F);
                        extraZ = (slowWave * tautFactor) + (flutterAmp * speedRatio);
                    }

                    float yawLimit = entity.isFallFlying() ? Mth.lerp(speedRatio, 6.5F, 4.0F) : 6.5F;
                    float localYaw = Mth.clamp(deltaYaw + extraZ, -yawLimit, yawLimit);
                    float localPitch = Mth.clamp(deltaPitch, -20.0F, 20.0F);

                    if (Float.isNaN(localYaw)) localYaw = 0.0F;
                    if (Float.isNaN(localPitch)) localPitch = 0.0F;

                    bone.setRotZ((float) Math.toRadians(localYaw));
                    bone.setRotX(bone.getInitialSnapshot().getRotX() + (float) Math.toRadians(localPitch));
                    bone.setRotY(bone.getInitialSnapshot().getRotY());
                    bone.setPosY(0.0F);

                    prevLocalPitch += localPitch;
                    prevLocalYaw += localYaw;
                }
            } else {
                // Ground Trailing Solver: Direct segment differential in world yaw/pitch with terrain clamping
                // 1. Precalculate world yaw and pitch for every segment directly from interpolated physics particles
                float[] segYaw = new float[NUM_SEGMENTS];
                float[] segPitch = new float[NUM_SEGMENTS];
                float waistYaw = refBackYaw + renderAnchorDeflection;

                for (int s = 0; s < NUM_SEGMENTS; s++) {
                    double dx = rx[s + 1] - rx[s];
                    double dy = ry[s + 1] - ry[s];
                    double dz = rz[s + 1] - rz[s];
                    float horizDist = (float) Math.sqrt(dx * dx + dz * dz);
                    segYaw[s] = (horizDist < 1.0E-4F)
                        ? (s == 0 ? waistYaw : segYaw[s - 1])
                        : (float) Math.toDegrees(Mth.atan2(-dx, dz));
                    segPitch[s] = (float) Math.toDegrees(Mth.atan2(-dy, Math.max(horizDist, 1.0E-4F)));
                }

                // Interpolate idle coil blend for smooth rendering
                float renderIdleCoil = Mth.lerp(partialTick, this.prevIdleCoilBlend, this.idleCoilBlend);
                float smoothRenderCoil = renderIdleCoil * renderIdleCoil * (3.0F - 2.0F * renderIdleCoil);

                // 2. Bone alignment: prevYaw tracks the world orientation of the parent bone,
                // allowing each bone to curve smoothly to match its physical segment on the floor.
                float prevYaw = waistYaw;
                float prevPitch = 0.0F;

                for (int i = 5; i <= 40; i++) {
                    GeoBone bone = boneFinder.apply("tail" + i);
                    if (bone == null) continue;

                    int segIdx = i - 5;
                    float targetYaw = segYaw[segIdx];
                    float targetPitch = segPitch[segIdx];

                    float deltaYaw = Mth.wrapDegrees(prevYaw - targetYaw);
                    float deltaPitch = targetPitch - prevPitch;

                    float extraZ = 0.0F;
                    if (isFlying) {
                        float flyWave = (float) Math.sin(ageInTicks * 0.3F - (i * 0.15F));
                        extraZ = flyWave * 1.5F;
                    } else if (smoothRenderCoil > 0.001F) {
                        // Subtle breathing undulation and lazy tip twitching when resting in coil
                        float idleBreathe = (float) Math.sin(ageInTicks * 0.06F + segIdx * 0.12F) * 0.8F * smoothRenderCoil;
                        float idleTipTwitch = (i >= 35) ? (float) Math.sin(ageInTicks * 0.15F + i * 0.5F) * 2.0F * smoothRenderCoil : 0.0F;
                        extraZ = idleBreathe + idleTipTwitch;
                    }

                    float rawTargetYaw = Mth.clamp(deltaYaw + extraZ, -14.0F, 14.0F);

                    // Slew-rate clamping: no bone can jump more than 3.5 deg per render frame
                    // This physically guarantees ZERO high-speed side-to-side flapping or flickering
                    float localYaw = Mth.clamp(rawTargetYaw, this.lastBoneRotZ[segIdx] - 3.5F, this.lastBoneRotZ[segIdx] + 3.5F);
                    this.lastBoneRotZ[segIdx] = localYaw;
                    boolean isCrawlingOnLand = crawlAmount > 0.5F && !entity.isInWater() && !entity.isFallFlying() && entity.onGround();
                    float rawPitchLimit = isCrawlingOnLand ? 8.0F : (i == 5 ? 65.0F : 20.0F);
                    float rawTargetPitch = Mth.clamp(deltaPitch, -rawPitchLimit, rawPitchLimit);
                    float localPitch = Mth.clamp(rawTargetPitch, this.lastBoneRotX[segIdx] - 3.5F, this.lastBoneRotX[segIdx] + 3.5F);
                    this.lastBoneRotX[segIdx] = localPitch;

                    if (Float.isNaN(localYaw)) localYaw = 0.0F;
                    if (Float.isNaN(localPitch)) localPitch = 0.0F;

                    bone.setRotZ((float) Math.toRadians(localYaw));
                    bone.setRotX(bone.getInitialSnapshot().getRotX() + (float) Math.toRadians(localPitch));
                    bone.setRotY(bone.getInitialSnapshot().getRotY());
                    bone.setPosY(0.0F);

                    prevYaw -= localYaw;
                    prevPitch += localPitch;
                }
            }
        }
    }

    /**
     * Fast, reliable terrain height query for stairs, slabs, blocks, and cliffs.
     * Returns Double.NEGATIVE_INFINITY when particle is suspended in air.
     */
    public static double getFloorHeight(Level level, double x, double y, double z) {
        if (level == null) return Double.NEGATIVE_INFINITY;
        BlockPos pos = BlockPos.containing(x, y, z);

        double highest = Double.NEGATIVE_INFINITY;
        // Search 1 block above down to 4 blocks below
        for (int dy = 1; dy >= -4; dy--) {
            BlockPos check = pos.above(dy);
            if (!level.hasChunkAt(check)) continue;
            BlockState state = level.getBlockState(check);
            VoxelShape shape = state.getCollisionShape(level, check);
            if (!shape.isEmpty()) {
                double surfaceY = check.getY() + shape.max(Direction.Axis.Y);
                if (surfaceY > highest && surfaceY <= y + 0.35D) {
                    highest = surfaceY;
                }
            }
        }

        return highest;
    }

    private static final Map<Integer, TailSimulation> ENTITY_TAILS = new ConcurrentHashMap<>();

    public static TailSimulation getOrCreateSimulation(LivingEntity entity) {
        return ENTITY_TAILS.computeIfAbsent(entity.getId(), id -> new TailSimulation());
    }

    public static void applyTailAnimation(Function<String, GeoBone> boneFinder,
                                          LivingEntity entity,
                                          float partialTick,
                                          boolean isChoking,
                                          boolean isSleeping,
                                          boolean isFlying,
                                          boolean isSwimming) {
        applyTailAnimation(boneFinder, entity, partialTick, isChoking, isSleeping, isFlying, isSwimming,
                entity.isPassenger(), entity.getVehicle());
    }

    public static void applyTailAnimation(Function<String, GeoBone> boneFinder,
                                          LivingEntity entity,
                                          float partialTick,
                                          boolean isChoking,
                                          boolean isSleeping,
                                          boolean isFlying,
                                          boolean isSwimming,
                                          boolean isPassenger,
                                          net.minecraft.world.entity.Entity vehicle) {
        TailSimulation sim = getOrCreateSimulation(entity);

        int currentTick = entity.tickCount;
        if (currentTick % 100 == 0) {
            ENTITY_TAILS.entrySet().removeIf(entry -> (currentTick - entry.getValue().lastTick) > 80);
        }

        if (sim.lastTick != currentTick) {
            sim.lastTick = currentTick;
            sim.updateTick(entity, isFlying, isSwimming, isPassenger);
        }

        sim.applyToBones(boneFinder, entity, partialTick, isChoking, isSleeping, isFlying, isSwimming, isPassenger, vehicle);
    }
}
