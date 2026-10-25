package net.ganyusbathwater.oririmod.client.physics;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * 3D Physics-Constrained Hair Engine for all female mobs and player cosmetics.
 * - Always faces toward the ground in world space (rotates AGAINST head pitch so hair never clips into the chest).
 * - Reacts dynamically to locomotion velocity, sprinting, leaping, and crawling.
 * - 2nd-order critically damped spring integration eliminates jitter and vibration.
 */
public class HairPhysicsController {

    public static class HairSimulation {
        public float pitch = 0.0F;
        public float pitchVel = 0.0F;
        public float prevPitch = 0.0F;

        public float roll = 0.0F;
        public float rollVel = 0.0F;
        public float prevRoll = 0.0F;

        public double lastX = 0;
        public double lastY = 0;
        public double lastZ = 0;
        public float lastBodyYaw = 0;
        public int lastSimTick = -1;
        public boolean initialized = false;

        public void step(LivingEntity entity, GeoBone headBone, float crawlAmount) {
            double curX = entity.getX();
            double curY = entity.getY();
            double curZ = entity.getZ();

            if (!initialized) {
                lastX = curX;
                lastY = curY;
                lastZ = curZ;
                lastBodyYaw = entity.yBodyRot;
                pitch = 0.0F;
                pitchVel = 0.0F;
                prevPitch = 0.0F;
                roll = 0.0F;
                rollVel = 0.0F;
                prevRoll = 0.0F;
                initialized = true;
                return;
            }

            prevPitch = pitch;
            prevRoll = roll;

            // 1. Movement displacement per tick
            double vx = curX - lastX;
            double vy = curY - lastY;
            double vz = curZ - lastZ;

            lastX = curX;
            lastY = curY;
            lastZ = curZ;

            // 2. Counter-rotation against Head Pitch & Roll so hair always points towards the ground
            // When head tilts up (+rotX), hair must rotate in reverse (-rotX) relative to the head to stay vertical.
            // When head tilts down (-rotX), hair must rotate in reverse (+rotX) relative to the head to stay vertical.
            float headRotXDeg = 0.0F;
            float headRotZDeg = 0.0F;
            if (headBone != null) {
                headRotXDeg = (float) Math.toDegrees(headBone.getRotX());
                headRotZDeg = (float) Math.toDegrees(headBone.getRotZ());
            }

            float groundPitch = -headRotXDeg;
            float groundRoll = -headRotZDeg;

            // 3. Movement drag & inertia
            float yawRad = entity.yBodyRot * Mth.DEG_TO_RAD;
            double forwardSpeed = -vx * Mth.sin(yawRad) + vz * Mth.cos(yawRad);
            double strafeSpeed = vx * Mth.cos(yawRad) + vz * Mth.sin(yawRad);

            // Forward movement pulls hair backward (+pitch)
            float pitchDrag = (float) (forwardSpeed * 110.0);
            // Lateral strafe swings hair opposite
            float rollDrag = (float) (-strafeSpeed * 110.0);

            // Vertical motion (falling/jumping)
            float vertDrag = (float) (-vy * 35.0);

            // Centrifugal turn inertia (turning body flings hair outwards)
            float deltaYaw = Mth.wrapDegrees(entity.yBodyRot - lastBodyYaw);
            lastBodyYaw = entity.yBodyRot;
            float rollCent = -deltaYaw * 0.70F;

            // 4. Crawl mode adjustment:
            // When crawling, body is horizontal. Locomotion streams hair along the spine toward the feet.
            if (crawlAmount > 0.0F) {
                float crawlStream = (float) (forwardSpeed * 140.0) * crawlAmount;
                pitchDrag += crawlStream;
            }

            // 5. Synthesize target angle
            float targetPitch = groundPitch + pitchDrag + vertDrag;
            float targetRoll = groundRoll + rollDrag + rollCent;

            // Subtle idle breathing when stationary
            if (vx * vx + vy * vy + vz * vz < 0.001) {
                targetPitch += (float) Math.sin(entity.tickCount * 0.08F) * 0.5F;
                targetRoll += (float) Math.cos(entity.tickCount * 0.06F) * 0.4F;
            }

            // 6. Strict anti-clipping clamp against back/neck:
            // Hair can never penetrate through the neck/skull into the chest (-10 deg max forward tilt),
            // and can never flip completely over the head (+85 deg max backward tilt).
            targetPitch = Mth.clamp(targetPitch, -10.0F, 85.0F);
            targetRoll = Mth.clamp(targetRoll, -38.0F, 38.0F);

            // 7. 2nd-order critically damped spring integration
            float forcePitch = (targetPitch - pitch) * 0.35F - pitchVel * 0.58F;
            pitchVel += forcePitch;
            pitch += pitchVel;

            float forceRoll = (targetRoll - roll) * 0.35F - rollVel * 0.58F;
            rollVel += forceRoll;
            roll += rollVel;
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

        GeoBone headBone = boneFinder.apply("head");

        float crawlAmount = 0.0F;
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            crawlAmount = player.getSwimAmount(partialTick);
            if (player.isVisuallySwimming() && crawlAmount < 0.01F) {
                crawlAmount = 1.0F;
            }
        } else if (entity.isVisuallySwimming()) {
            crawlAmount = 1.0F;
        }

        // Step simulation once per tick
        if (entity.tickCount != sim.lastSimTick) {
            sim.step(entity, headBone, crawlAmount);
            sim.lastSimTick = entity.tickCount;
        }

        // Sub-tick interpolation for 144Hz+ display smoothness
        float renderPitch = Mth.lerp(partialTick, sim.prevPitch, sim.pitch);
        float renderRoll = Mth.lerp(partialTick, sim.prevRoll, sim.roll);

        float radPitch = renderPitch * Mth.DEG_TO_RAD;
        float radRoll = renderRoll * Mth.DEG_TO_RAD;

        // Animate hair chains or standalone hair
        applyHairChain(boneFinder, "hair_back", radPitch, radRoll);
        applyHairChain(boneFinder, "hair_braid", radPitch, radRoll);
        applyHairChain(boneFinder, "hair_ponytail", radPitch, radRoll);
        applyHairChain(boneFinder, "hair", radPitch, radRoll);

        // Side strands
        applyHairChain(boneFinder, "hair_left", radPitch * 0.6F, radRoll * 0.8F);
        applyHairChain(boneFinder, "hair_right", radPitch * 0.6F, radRoll * 0.8F);
    }

    private static void applyHairChain(Function<String, GeoBone> boneFinder, String baseName, float radPitch, float radRoll) {
        GeoBone root = boneFinder.apply(baseName + "1");
        if (root != null) {
            applyRotation(root, radPitch * 0.80F, radRoll * 0.80F);

            GeoBone seg2 = boneFinder.apply(baseName + "2");
            if (seg2 != null) {
                applyRotation(seg2, radPitch * 1.15F, radRoll * 1.15F);

                GeoBone seg3 = boneFinder.apply(baseName + "3");
                if (seg3 != null) {
                    applyRotation(seg3, radPitch * 1.50F, radRoll * 1.50F);

                    GeoBone seg4 = boneFinder.apply(baseName + "4");
                    if (seg4 != null) {
                        applyRotation(seg4, radPitch * 1.85F, radRoll * 1.85F);
                    }
                }
            }
            return;
        }

        GeoBone single = boneFinder.apply(baseName);
        if (single != null) {
            applyRotation(single, radPitch, radRoll);
        }
    }

    private static void applyRotation(GeoBone bone, float pitchOffset, float rollOffset) {
        bone.setRotX(bone.getInitialSnapshot().getRotX() + pitchOffset);
        bone.setRotZ(bone.getInitialSnapshot().getRotZ() + rollOffset);
        bone.setRotY(bone.getInitialSnapshot().getRotY());
    }
}
