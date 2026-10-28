package net.ganyusbathwater.oririmod.client.physics;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Clean, lightweight dynamic hair engine for all female mobs and player cosmetics.
 * - Applies gravity droop when looking down so hair doesn't stick out like a rigid board.
 * - Anti-clipping backstop when looking up so hair rests on the neck/back and never enters the chest or sticks up into the sky.
 * - Per-frame boundary enforcement eliminates phase-inversion and back-clipping during fast flicks and jumping.
 * - Soft yaw lag when looking sideways so hair trails head turns organically.
 * - Safely disables vertical gravity when swimming, crawling, flying, or sleeping in a bed.
 * - Multi-segment cascading and exponential decay easing ensure fluid movement with zero idle jitter.
 */
public class HairPhysicsController {

    public static class HairSimulation {
        public float pitch = 0.0F;
        public float prevPitch = 0.0F;

        public float yaw = 0.0F;
        public float prevYaw = 0.0F;

        public float roll = 0.0F;
        public float prevRoll = 0.0F;

        public float forwardWind = 0.0F;
        public float prevForwardWind = 0.0F;

        public float crawlLag = 0.0F;
        public float prevCrawlLag = 0.0F;

        public double lastX = 0;
        public double lastY = 0;
        public double lastZ = 0;
        public float lastBodyYaw = 0;
        public int lastSimTick = -1;
        public boolean initialized = false;

        public void step(LivingEntity entity, float headYawDeg, float headPitchDeg, boolean isHorizontal) {
            double curX = entity.getX();
            double curY = entity.getY();
            double curZ = entity.getZ();

            if (!initialized) {
                lastX = curX;
                lastY = curY;
                lastZ = curZ;
                lastBodyYaw = entity.yBodyRot;
                pitch = 0.0F;
                prevPitch = 0.0F;
                yaw = 0.0F;
                prevYaw = 0.0F;
                roll = 0.0F;
                prevRoll = 0.0F;
                forwardWind = 0.0F;
                prevForwardWind = 0.0F;
                crawlLag = 0.0F;
                prevCrawlLag = 0.0F;
                initialized = true;
                return;
            }

            prevPitch = pitch;
            prevYaw = yaw;
            prevRoll = roll;
            prevForwardWind = forwardWind;
            prevCrawlLag = crawlLag;

            // 1. Movement displacement per tick
            double dx = curX - lastX;
            double dy = curY - lastY;
            double dz = curZ - lastZ;

            lastX = curX;
            lastY = curY;
            lastZ = curZ;

            float yawRad = entity.yBodyRot * Mth.DEG_TO_RAD;
            double forwardSpeed = -dx * Mth.sin(yawRad) + dz * Mth.cos(yawRad);
            double strafeSpeed = dx * Mth.cos(yawRad) + dz * Mth.sin(yawRad);

            float deltaBodyYaw = Mth.wrapDegrees(entity.yBodyRot - lastBodyYaw);
            lastBodyYaw = entity.yBodyRot;

            float targetPitch;
            float targetYaw;
            float targetRoll;

            if (isHorizontal) {
                // GATEKEEPER: When crawling, swimming, flying, or sleeping in bed,
                // vertical gravity is completely disabled so hair doesn't bend 90° through floor, bed, or torso.
                float targetCrawl = 0.0F;
                if (forwardSpeed > 0.01) {
                    targetCrawl = (float) Math.min(forwardSpeed * 35.0, 15.0);
                }
                crawlLag = Mth.lerp(0.35F, crawlLag, targetCrawl);
                targetPitch = crawlLag;
                targetYaw = 0.0F;
                targetRoll = 0.0F;
                forwardWind = 0.0F;
            } else {
                crawlLag = 0.0F;

                // Locomotion and falling wind lift
                float targetWind = (float) (Math.max(forwardSpeed, 0.0) * 45.0);
                if (dy < -0.1) {
                    targetWind += (float) Math.min(-dy * 20.0, 15.0);
                }
                forwardWind = Mth.lerp(0.35F, forwardWind, Math.min(targetWind, 25.0F));

                // Behavior 1: Pitch Gravity (Droop down vs Rest on back)
                // In GeckoLib: headPitchDeg < 0 is looking DOWN (face toward feet).
                //              headPitchDeg >= 0 is looking UP (face toward sky).
                if (headPitchDeg < 0.0F) {
                    // Looking down: droop backward relative to head to hang down toward the floor (+rotX)
                    float droopPitch = -headPitchDeg * 0.65F;
                    targetPitch = Math.min(droopPitch + forwardWind, 55.0F);
                } else {
                    // Looking up: hair must rest against back (-rotX relative to head)
                    float restPitch = -headPitchDeg * 0.70F;
                    float backstopLimit = -headPitchDeg + 10.0F; // world angle >= +10° along back
                    targetPitch = Math.max(restPitch, backstopLimit);
                    targetPitch = Math.min(targetPitch, 0.0F);
                }

                // Behavior 2: Soft Yaw Lag when Looking Sideways
                float yawLag = -headYawDeg * 0.20F;
                float centrifugalYaw = deltaBodyYaw * 0.25F;
                targetYaw = Mth.clamp(yawLag + centrifugalYaw, -25.0F, 25.0F);

                // Behavior 3: Lateral Roll Sway on Strafe and Turn
                float strafeRoll = (float) (-strafeSpeed * 55.0);
                float centrifugalRoll = -deltaBodyYaw * 0.20F;
                targetRoll = Mth.clamp(strafeRoll + centrifugalRoll, -18.0F, 18.0F);
            }

            // Exponential easing with fast 0.45F responsiveness to eliminate lag-induced phase inversion
            pitch = Mth.lerp(0.45F, pitch, targetPitch);
            yaw = Mth.lerp(0.35F, yaw, targetYaw);
            roll = Mth.lerp(0.35F, roll, targetRoll);
        }
    }

    private static final Map<LivingEntity, HairSimulation> SIMULATIONS = new WeakHashMap<>();

    public static HairSimulation getOrCreateSimulation(LivingEntity entity) {
        return SIMULATIONS.computeIfAbsent(entity, e -> new HairSimulation());
    }

    /**
     * Updates and applies dynamic hair physics during model rendering.
     *
     * @param entity       The mob or player
     * @param boneFinder   Function to lookup GeoBones by name
     * @param partialTick  Frame render fraction for sub-tick interpolation
     * @param netHeadYaw   Head yaw relative to body yaw (degrees)
     * @param headPitch    Head pitch (degrees)
     */
    public static void applyHairPhysics(LivingEntity entity,
                                        Function<String, GeoBone> boneFinder,
                                        float partialTick,
                                        float netHeadYaw,
                                        float headPitch) {
        if (entity == null || boneFinder == null) return;

        HairSimulation sim = getOrCreateSimulation(entity);

        // Detect horizontal states (swimming, crawling, sleeping, flying)
        boolean isHorizontal = entity.isSleeping()
                || entity.getPose() == Pose.SLEEPING
                || entity.isVisuallySwimming()
                || entity.getPose() == Pose.SWIMMING
                || entity.isFallFlying()
                || entity.getPose() == Pose.FALL_FLYING;

        if (entity instanceof Player player) {
            if (player.getSwimAmount(partialTick) > 0.05F) {
                isHorizontal = true;
            }
        }

        // Retrieve actual head rotation directly from the model's head bone if present
        float actualHeadPitchDeg;
        float actualHeadYawDeg;
        GeoBone headBone = boneFinder.apply("head");
        if (headBone != null) {
            actualHeadPitchDeg = headBone.getRotX() * Mth.RAD_TO_DEG;
            actualHeadYawDeg = headBone.getRotY() * Mth.RAD_TO_DEG;
        } else {
            // Fallback for models without a direct 'head' bone
            actualHeadPitchDeg = -headPitch;
            actualHeadYawDeg = -netHeadYaw;
        }

        // Step simulation once per tick
        if (entity.tickCount != sim.lastSimTick) {
            sim.step(entity, actualHeadYawDeg, actualHeadPitchDeg, isHorizontal);
            sim.lastSimTick = entity.tickCount;
        }

        // Sub-tick interpolation for 144Hz+ display smoothness
        float renderPitch = Mth.lerp(partialTick, sim.prevPitch, sim.pitch);
        float renderYaw = Mth.lerp(partialTick, sim.prevYaw, sim.yaw);
        float renderRoll = Mth.lerp(partialTick, sim.prevRoll, sim.roll);

        // ABSOLUTE ANTI-CLIPPING BOUNDARIES ENFORCED PER-FRAME:
        // By clamping renderPitch against actualHeadPitchDeg in real time, the hair
        // can NEVER penetrate the back during fast upward flicks, and can NEVER invert through the chest.
        if (!isHorizontal) {
            if (actualHeadPitchDeg >= 0.0F) {
                // When looking up: hair MUST rest against back and NEVER penetrate spine or chest
                float backstop = -actualHeadPitchDeg + 10.0F; // world angle >= +10° along back
                renderPitch = Math.max(renderPitch, backstop);
                renderPitch = Math.min(renderPitch, 0.0F); // never tilt backward into the sky
            } else {
                // When looking down: hair MUST droop backward relative to head and NEVER penetrate forward
                renderPitch = Math.max(renderPitch, 0.0F);
                renderPitch = Math.min(renderPitch, 55.0F);
            }
        }

        float radPitch = renderPitch * Mth.DEG_TO_RAD;
        float radYaw = renderYaw * Mth.DEG_TO_RAD;
        float radRoll = renderRoll * Mth.DEG_TO_RAD;

        // Apply dynamic physics EXCLUSIVELY to bone chain: hair_back1, hair_back2, etc.
        // Static skull hair such as "hair_base" or "hair" is strictly untouched.
        applyHairBackChain(boneFinder, radPitch, radYaw, radRoll);
    }

    /**
     * Applies dynamic physics strictly to hair chain bones named "hair_back1", "hair_back2", "hair_back3", etc.
     * Static head hair (such as "hair_base" or "hair") is never rotated.
     */
    private static void applyHairBackChain(Function<String, GeoBone> boneFinder, float radPitch, float radYaw, float radRoll) {
        GeoBone root = boneFinder.apply("hair_back1");
        if (root == null) return;

        // Detect total consecutive hair_back segments (e.g. hair_back1, hair_back2, ...)
        int segmentCount = 1;
        while (boneFinder.apply("hair_back" + (segmentCount + 1)) != null) {
            segmentCount++;
        }

        if (segmentCount == 1) {
            // Standalone segment (e.g. aurora_cosmetic hair_back1) gets full 100% rotation
            applyRotation(root, radPitch, radYaw, radRoll);
            return;
        }

        // Multi-segment chain: cascade curvature smoothly from root (0.70x) to tip (1.75x)
        for (int i = 1; i <= segmentCount; i++) {
            GeoBone seg = boneFinder.apply("hair_back" + i);
            if (seg != null) {
                float progress = (float) (i - 1) / (float) (segmentCount - 1);
                float mult = Mth.lerp(progress, 0.70F, 1.75F);
                applyRotation(seg, radPitch * mult, radYaw * mult, radRoll * mult);
            }
        }
    }

    private static void applyRotation(GeoBone bone, float pitchOffset, float yawOffset, float rollOffset) {
        bone.setRotX(bone.getInitialSnapshot().getRotX() + pitchOffset);
        bone.setRotY(bone.getInitialSnapshot().getRotY() + yawOffset);
        bone.setRotZ(bone.getInitialSnapshot().getRotZ() + rollOffset);
    }
}
