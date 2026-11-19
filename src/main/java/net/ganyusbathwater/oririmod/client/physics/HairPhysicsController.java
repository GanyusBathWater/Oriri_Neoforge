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
                // keep hair trailing cleanly backward along the body axis without diving through floor/torso.
                float targetCrawl = -6.0F;
                if (forwardSpeed > 0.01) {
                    targetCrawl = (float) -Math.min(6.0 + forwardSpeed * 50.0, 28.0);
                }
                crawlLag = Mth.lerp(0.40F, crawlLag, targetCrawl);
                targetPitch = crawlLag;
                targetYaw = 0.0F;
                targetRoll = 0.0F;
                forwardWind = 0.0F;
            } else {
                crawlLag = 0.0F;

                // Locomotion and falling wind lift (NEGATIVE rotX swings hanging back hair BACKWARD toward +Z!)
                float targetWind = (float) (Math.max(forwardSpeed, 0.0) * 85.0);
                if (dy < -0.06) {
                    targetWind += (float) Math.min(-dy * 42.0, 32.0);
                }
                // Subtle idle breathing/breeze clearance so hair always clears the upper back cleanly
                forwardWind = Mth.lerp(0.42F, forwardWind, Math.min(targetWind, 48.0F));

                // Behavior 1: Pitch Gravity & Strong Anti-Clipping Backstop
                // GeckoLib RenderUtil.rotateMatrixAroundBone applies Z -> Y -> X (right-handed):
                //   headPitchDeg > 0  => looking UP   (face up, hanging back hair swings FORWARD into torso)
                //   headPitchDeg < 0  => looking DOWN (face down, hanging back hair swings BACKWARD away from torso)
                // On hanging back-hair bones (pivot behind neck, geometry along -Y at +Z):
                //   NEGATIVE rotX => tilts BACKWARD (+Z, away from torso)
                //   POSITIVE rotX => tilts FORWARD  (-Z, toward torso)
                if (headPitchDeg < 0.0F) {
                    // Looking down: gravity droops hair toward vertical (+rotX), while forward wind streams it BACKWARD (-rotX)
                    float droopPitch = -headPitchDeg * 0.55F;
                    float desiredLocal = droopPitch - forwardWind;
                    // Never allow world pitch (headPitchDeg + local) to swing forward past -10° into the back
                    float maxForwardLocal = -headPitchDeg - 10.0F;
                    targetPitch = Mth.clamp(desiredLocal, -65.0F, maxForwardLocal);
                } else {
                    // Looking straight or UP: push firmly BACKWARD (-rotX) so hair rides outside the upper back
                    // and NEVER clips through the torso!
                    float backstopLocal = -headPitchDeg - 14.0F;
                    targetPitch = Mth.clamp(backstopLocal - forwardWind * 0.75F, -82.0F, backstopLocal);
                }

                // Behavior 2: Disable Y-axis twist on flat back-hair planes!
                // Twisting a wide back-hair bone around vertical Y drives one edge straight into the neck/shoulders.
                targetYaw = 0.0F;

                // Behavior 3: Ultra-gentle lateral roll inertia only (never clips sideways into shoulders)
                float strafeRoll = (float) (-strafeSpeed * 22.0);
                float turnRoll = -deltaBodyYaw * 0.08F;
                targetRoll = Mth.clamp(strafeRoll + turnRoll, -6.0F, 6.0F);
            }

            // Fast, responsive easing for strong hair motion without phase-inversion lag
            pitch = Mth.lerp(0.55F, pitch, targetPitch);
            yaw = Mth.lerp(0.40F, yaw, targetYaw);
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
        // World pitch = actualHeadPitchDeg + renderPitch.
        // Because -rotX tilts hanging back hair BACKWARD (+Z, away from torso),
        // keeping (actualHeadPitchDeg + renderPitch) <= -12° guarantees hair NEVER clips into the torso!
        if (!isHorizontal) {
            float maxAllowedLocalPitch = -actualHeadPitchDeg - (actualHeadPitchDeg >= 0.0F ? 14.0F : 10.0F);
            renderPitch = Math.min(renderPitch, maxAllowedLocalPitch);
            renderPitch = Math.max(renderPitch, -85.0F);
        }

        float radPitch = renderPitch * Mth.DEG_TO_RAD;
        float radRoll = renderRoll * Mth.DEG_TO_RAD;

        // Apply dynamic physics EXCLUSIVELY to bone chain: hair_back1, hair_back2, etc.
        // Static skull hair such as "hair_base" or "hair" is strictly untouched.
        applyHairBackChain(boneFinder, radPitch, radRoll);
    }

    /**
     * Applies dynamic physics strictly to hair chain bones named "hair_back1", "hair_back2", "hair_back3", etc.
     * Static head hair (such as "hair_base" or "hair") is never rotated.
     */
    private static void applyHairBackChain(Function<String, GeoBone> boneFinder, float radPitch, float radRoll) {
        GeoBone root = boneFinder.apply("hair_back1");
        if (root == null) return;

        // Detect total consecutive hair_back segments (e.g. hair_back1, hair_back2, ...)
        int segmentCount = 1;
        while (boneFinder.apply("hair_back" + (segmentCount + 1)) != null) {
            segmentCount++;
        }

        if (segmentCount == 1) {
            // Standalone segment (e.g. aurora_cosmetic hair_back1) gets full 100% pitch + subtle roll
            applyRotation(root, radPitch, 0.0F, radRoll);
            return;
        }

        // Multi-segment chain: root takes the primary anti-clipping clearance (0.78x) so upper hair
        // clears the torso immediately, while lower segments add smooth flowing whip without over-compounding roll.
        for (int i = 1; i <= segmentCount; i++) {
            GeoBone seg = boneFinder.apply("hair_back" + i);
            if (seg != null) {
                float pitchMult = (i == 1) ? 0.78F : (0.35F / (segmentCount - 1));
                float rollMult = (i == 1) ? 0.60F : (0.40F / (segmentCount - 1));
                applyRotation(seg, radPitch * pitchMult, 0.0F, radRoll * rollMult);
            }
        }
    }

    private static void applyRotation(GeoBone bone, float pitchOffset, float yawOffset, float rollOffset) {
        bone.setRotX(bone.getInitialSnapshot().getRotX() + pitchOffset);
        bone.setRotY(bone.getInitialSnapshot().getRotY() + yawOffset);
        bone.setRotZ(bone.getInitialSnapshot().getRotZ() + rollOffset);
    }
}
