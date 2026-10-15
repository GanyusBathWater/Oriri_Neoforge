package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class SnakeTailPhysics {

    public static class TailState {
        public final float[] segmentYaws = new float[41];
        public final float[] segmentHeights = new float[41];
        public final float[] prevSegmentHeights = new float[41];
        public float lastBodyYaw = 0.0F;
        public double lastX = 0.0D;
        public double lastY = 0.0D;
        public double lastZ = 0.0D;
        public float accumulatedMoveDist = 0.0F;
        public int lastTick = -1;
        public boolean initialized = false;

        public void init(float yaw, double x, double y, double z) {
            for (int i = 0; i <= 40; i++) {
                segmentYaws[i] = yaw;
                segmentHeights[i] = 0.0F;
                prevSegmentHeights[i] = 0.0F;
            }
            this.lastBodyYaw = yaw;
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            this.accumulatedMoveDist = 0.0F;
            this.initialized = true;
        }
    }

    private static final Map<Integer, TailState> ENTITY_STATES = new ConcurrentHashMap<>();

    public static TailState getOrCreateState(LivingEntity entity) {
        TailState state = ENTITY_STATES.computeIfAbsent(entity.getId(), id -> new TailState());
        double distSq = (entity.getX() - state.lastX) * (entity.getX() - state.lastX) +
                        (entity.getY() - state.lastY) * (entity.getY() - state.lastY) +
                        (entity.getZ() - state.lastZ) * (entity.getZ() - state.lastZ);

        if (!state.initialized || distSq > 64.0D) {
            state.init(entity.yBodyRot, entity.getX(), entity.getY(), entity.getZ());
        }
        return state;
    }

    public static float getGroundDistance(LivingEntity entity) {
        if (entity.onGround()) return 0.0F;
        Level level = entity.level();
        double entityY = entity.getY();
        BlockPos pos = entity.blockPosition();

        double highestSurface = Double.NEGATIVE_INFINITY;
        for (int dy = -1; dy <= 4; dy++) {
            BlockPos check = pos.below(dy);
            var blockState = level.getBlockState(check);
            VoxelShape shape = blockState.getCollisionShape(level, check);
            if (!shape.isEmpty()) {
                double surfaceY = check.getY() + shape.max(Direction.Axis.Y);
                if (surfaceY <= entityY + 0.15D && surfaceY > highestSurface) {
                    highestSurface = surfaceY;
                }
            }
        }

        if (highestSurface > Double.NEGATIVE_INFINITY) {
            double diff = entityY - highestSurface;
            if (diff >= 0.0D && diff <= 2.5D) {
                return (float) diff;
            }
        }
        return 0.0F;
    }

    public static void applyTailAnimation(Function<String, GeoBone> boneFinder,
                                          LivingEntity entity,
                                          float partialTick,
                                          boolean isChoking,
                                          boolean isSleeping,
                                          boolean isFlying,
                                          boolean isSwimming) {
        TailState state = getOrCreateState(entity);

        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSpeed = Math.min(entity.walkAnimation.speed(partialTick), 1.0F);
        float ageInTicks = entity.tickCount + partialTick;

        // Periodic cleanup of dead entity physics states
        int currentTick = entity.tickCount;
        if (currentTick % 100 == 0) {
            ENTITY_STATES.entrySet().removeIf(entry -> (currentTick - entry.getValue().lastTick) > 80);
        }

        // Update trailing simulation once per tick
        if (state.lastTick != currentTick) {
            state.lastTick = currentTick;
            float currentYaw = entity.yBodyRot;
            state.segmentYaws[0] = currentYaw;

            double dx = entity.getX() - state.lastX;
            double dz = entity.getZ() - state.lastZ;
            double movedDist = Math.sqrt(dx * dx + dz * dz);
            state.accumulatedMoveDist += (float) movedDist;

            // Decay distance slack when stopped
            if (movedDist < 0.015D) {
                state.accumulatedMoveDist = Math.max(0.0F, state.accumulatedMoveDist - 0.04F);
            }

            for (int i = 1; i <= 40; i++) {
                float diff = Mth.wrapDegrees(state.segmentYaws[i - 1] - state.segmentYaws[i]);
                float rate;
                if (limbSpeed > 0.05F) {
                    // Active slithering: segments follow along the path smoothly and responsively
                    rate = 0.35F + (limbSpeed * 0.45F);
                    state.segmentYaws[i] += diff * rate;
                } else {
                    // Stationary / looking around:
                    // Segments react quickly if turn exceeds deadband, so it doesn't take time to catch up
                    float deadband;
                    if (i <= 4) {
                        deadband = 1.0F; // Waist follows almost instantly
                        rate = 0.75F;
                    } else if (i <= 10) {
                        deadband = 2.0F; // Front ground reacts swiftly
                        rate = 0.50F;
                    } else if (i <= 20) {
                        deadband = 3.5F; // Mid tail
                        rate = 0.35F;
                    } else {
                        deadband = 5.0F; // Tail tip stays anchored with slack
                        rate = 0.25F;
                    }

                    if (Math.abs(diff) > deadband) {
                        float excess = diff - Math.signum(diff) * deadband;
                        state.segmentYaws[i] += excess * rate;
                    }
                }

                // Physical bend constraint per segment: cannot fold more than 11.5 degrees relative to neighbor
                float clampedDiff = Mth.wrapDegrees(state.segmentYaws[i - 1] - state.segmentYaws[i]);
                if (Math.abs(clampedDiff) > 11.5F) {
                    state.segmentYaws[i] = state.segmentYaws[i - 1] - Math.signum(clampedDiff) * 11.5F;
                }
            }

            // Vertical draping simulation once per tick
            System.arraycopy(state.segmentHeights, 0, state.prevSegmentHeights, 0, 41);
            float currentGroundDist = getGroundDistance(entity);
            state.segmentHeights[0] = currentGroundDist;

            for (int i = 1; i <= 40; i++) {
                float maxDrop = 0.14F;
                float targetH = Math.max(0.0F, state.segmentHeights[i - 1] - maxDrop);
                if (targetH > state.segmentHeights[i - 1]) {
                    targetH = state.segmentHeights[i - 1];
                }
                state.segmentHeights[i] = targetH;
            }

            state.lastX = entity.getX();
            state.lastY = entity.getY();
            state.lastZ = entity.getZ();
            state.lastBodyYaw = currentYaw;
        }

        float currentBodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float waistToGroundYaw = Mth.wrapDegrees(state.segmentYaws[5] - currentBodyYaw);

        // Dynamic slope and pitch draping for jumping
        float[] interpHeights = new float[41];
        for (int j = 0; j <= 40; j++) {
            interpHeights[j] = Mth.lerp(partialTick, state.prevSegmentHeights[j], state.segmentHeights[j]);
        }

        float[] rawSlope = new float[41];
        float segLen = 0.25F;
        for (int j = 5; j <= 40; j++) {
            float dh = interpHeights[j - 1] - interpHeights[j];
            float ratio = Mth.clamp(dh / segLen, 0.0F, 0.95F);
            rawSlope[j] = (float) Math.asin(ratio);
        }

        // 3-tap smoothing to eliminate sharp kinks
        float[] slope = new float[41];
        for (int j = 5; j <= 40; j++) {
            float p = (j > 5) ? rawSlope[j - 1] : 0.0F;
            float c = rawSlope[j];
            float n = (j < 40) ? rawSlope[j + 1] : 0.0F;
            slope[j] = (p * 0.25F) + (c * 0.50F) + (n * 0.25F);
        }

        // Ensure tail segments firmly on the ground stay identically 0.0
        for (int j = 12; j <= 40; j++) {
            if (interpHeights[j] <= 0.001F && interpHeights[j - 1] <= 0.001F && interpHeights[j - 2] <= 0.001F) {
                slope[j] = 0.0F;
            }
        }

        float[] deltaSlope = new float[41];
        for (int j = 5; j <= 40; j++) {
            float prevS = (j == 5) ? 0.0F : slope[j - 1];
            deltaSlope[j] = slope[j] - prevS;
        }

        // Apply procedural dynamics per segment
        for (int i = 1; i <= 40; i++) {
            GeoBone segment = boneFinder.apply("tail" + i);
            if (segment == null) continue;

            if (isSleeping) {
                float coilAngle = (float) Math.toRadians(12.0F + (i * 0.5F));
                segment.setRotZ(coilAngle);
                continue;
            }

            if (isFlying) {
                segment.setRotX(segment.getInitialSnapshot().getRotX());
                segment.setRotY(segment.getInitialSnapshot().getRotY());
                float flightWave = (float) Math.sin(ageInTicks * 0.3F - (i * 0.15F)) * 1.5F;
                segment.setRotZ((float) Math.toRadians(flightWave));
                segment.setPosY(0.0F);
                continue;
            }

            if (isSwimming) {
                float swimWave = (float) Math.sin(ageInTicks * 0.25F - (i * 0.2F));
                float swimAmp = 4.0F + (8.0F * ((float) i / 40.0F));
                segment.setRotZ((float) Math.toRadians(swimWave * swimAmp));
                continue;
            }

            if (isChoking) {
                if (i <= 5) {
                    segment.setRotX((float) Math.toRadians(-8.0F));
                } else if (i <= 10) {
                    // Sweep around Lamia hip from back to front
                    segment.setRotZ((float) Math.toRadians(-36.0F));
                } else if (i <= 35) {
                    // Tightly coil around the victim in front
                    segment.setRotZ((float) Math.toRadians(-24.0F));
                    segment.setRotX((float) Math.toRadians(-3.5F)); // Upward spiral
                } else {
                    // Tip constriction & twitch
                    float tipTwitch = (float) Math.sin(ageInTicks * 0.5F + i) * 3.5F;
                    segment.setRotZ((float) Math.toRadians(-18.0F + tipTwitch));
                }
                continue;
            }

            // Normal ground movement & slithering
            if (i <= 4) {
                // Segments 1 to 4 form the vertical waist pitch curve connecting torso to floor.
                segment.setRotX(segment.getInitialSnapshot().getRotX());
                segment.setRotY(segment.getInitialSnapshot().getRotY());
                segment.setPosY(0.0F);

                // Gently distribute waist twist across segments 3 and 4 so torso turning is natural:
                if (i == 3) {
                    segment.setRotZ((float) Math.toRadians(waistToGroundYaw * 0.20F));
                } else if (i == 4) {
                    segment.setRotZ((float) Math.toRadians(waistToGroundYaw * 0.25F));
                } else {
                    segment.setRotZ(segment.getInitialSnapshot().getRotZ());
                }
            } else {
                // Ground segments 5 through 40:
                // Pitch rotX slopes smoothly from waist down to ground, then locks flat on floor.
                // Inverted sign (- deltaSlope) ensures downward ground slope in the flipped coordinate frame.
                segment.setRotX(segment.getInitialSnapshot().getRotX() - deltaSlope[i]);
                segment.setRotY(segment.getInitialSnapshot().getRotY());

                // 2. Yaw counter-rotation and segment tracking:
                float turnAngle;
                if (i == 5) {
                    // Segment 5 completes the waist-to-ground twist (0.55 remaining to total 1.0)
                    turnAngle = waistToGroundYaw * 0.55F;
                } else {
                    // Segments 6..40 rotate relative to predecessor to preserve world shape
                    float diffYaw = Mth.wrapDegrees(state.segmentYaws[i] - state.segmentYaws[i - 1]);
                    turnAngle = Mth.clamp(diffYaw, -10.0F, 10.0F);
                }

                float wave;
                if (limbSpeed > 0.05F) {
                    wave = (float) Math.sin(limbSwing * 0.45F - (i * 0.22F)) * (3.0F + (5.0F * ((float) (i - 5) / 35.0F))) * limbSpeed;
                } else {
                    wave = (float) Math.sin(ageInTicks * 0.04F - (i * 0.18F)) * (1.0F + (2.0F * ((float) (i - 5) / 35.0F)));
                }

                // Strict inter-segment bend angle limit to eliminate corner clipping/penetration
                float totalBend = Mth.clamp(turnAngle + wave, -12.0F, 12.0F);
                segment.setRotZ((float) Math.toRadians(totalBend));

                // 3. 0.25-block separation slack:
                if (i <= 15) {
                    float stretchAmount = Mth.clamp(state.accumulatedMoveDist, 0.0F, 0.25F * 16.0F);
                    float perSegStretch = stretchAmount / 11.0F; // distribute across segments 5..15
                    segment.setPosY(-perSegStretch);
                } else {
                    segment.setPosY(0.0F);
                }
            }
        }
    }
}
