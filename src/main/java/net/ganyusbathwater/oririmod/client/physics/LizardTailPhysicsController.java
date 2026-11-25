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
 * Procedural biomechanical animation & spring-inertia controller for the 17-segment hovering lizard tail
 * ("tail", "tail2", ..., "tail17").
 *
 * Coordinate conventions (GeckoLib RenderLayer right-handed bone space, tail extending along +Z):
 * - Positive rotX pitches the tail DOWNWARD (-Y, toward ground); negative rotX pitches UPWARD (+Y).
 * - Positive rotY swings the tail toward +X (player's LEFT); negative rotY swings toward -X (player's RIGHT).
 * - Positive rotZ rolls the dorsal fin toward -X (player's RIGHT).
 *
 * All transitions are smoothly state-blended per tick with sub-tick partialTick interpolation for 144Hz+ fluidity.
 */
public final class LizardTailPhysicsController {

    private LizardTailPhysicsController() {}

    public static final int NUM_SEGMENTS = 17;

    private static final String[] BONE_NAMES = new String[NUM_SEGMENTS];

    static {
        BONE_NAMES[0] = "tail";
        for (int i = 1; i < NUM_SEGMENTS; i++) {
            BONE_NAMES[i] = "tail" + (i + 1);
        }
    }

    /**
     * Natural hovering lizard S-arch posture (degrees, local rotX per segment).
     * Starts at Y=15px, sweeps down behind the legs to lowest centerline Y=4.05px
     * (~2px / 0.13 blocks cleanly hovering above ground), then arches gently up to Y=6.17px and levels at the tip.
     */
    private static final float[] HOVER_PITCH_DEG = {
            18.0F, 14.0F,  8.0F,  2.0F, -5.0F,
            -8.0F, -9.0F, -9.0F, -8.0F, -6.0F,
            -4.0F, -3.0F, -2.0F,  0.0F,  3.0F,
             5.0F,  4.0F
    };

    /**
     * Higher, sleeker counterbalance carriage when sprinting (centerline stays around Y=9-10px).
     */
    private static final float[] SPRINT_PITCH_DEG = {
            11.0F,  8.0F,  4.0F,  0.0F, -4.0F,
            -5.5F, -5.5F, -4.5F, -3.5F, -2.5F,
            -1.5F, -1.0F, -0.5F,  0.0F,  1.0F,
             1.5F,  1.5F
    };

    /**
     * Horizontal body alignment (swimming, crawling, elytra glide, sleeping).
     * When vanilla pitches the player face-down, local +Z points UP and local -Y points BACKWARD along the legs.
     * Sweeping +88° across the base 5 segments aligns the tail smoothly straight behind the player.
     */
    private static final float[] HORIZONTAL_PITCH_DEG = {
            30.0F, 26.0F, 18.0F, 10.0F,  4.0F,
             0.0F,  0.0F,  0.0F,  0.0F,  0.0F,
             0.0F,  0.0F,  0.0F,  0.0F,  0.0F,
             0.0F,  0.0F
    };

    /**
     * Weight distribution for vertical air drag (jump dip / fall lift / landing bounce) across segments.
     * Sum = 1.0 so a total target pitch distributes cleanly down the tail without sharp kinks.
     */
    private static final float[] AIR_PITCH_WEIGHT = {
            0.13F, 0.11F, 0.10F, 0.09F, 0.08F,
            0.07F, 0.06F, 0.055F, 0.05F, 0.045F,
            0.04F, 0.035F, 0.035F, 0.03F, 0.025F,
            0.025F, 0.02F
    };

    /**
     * Distribution of crouch torso counter-pitch across the base 4 segments.
     */
    private static final float[] CROUCH_COUNTER_WEIGHT = {
            0.48F, 0.30F, 0.15F, 0.07F
    };

    public static final class TailState {
        // Spring-damped trailing yaw chain (degrees per segment)
        public final float[] lagYaw = new float[NUM_SEGMENTS];
        public final float[] prevLagYaw = new float[NUM_SEGMENTS];
        public final float[] lagYawVel = new float[NUM_SEGMENTS];

        // Vertical dynamics spring (jump dip, fall lift, landing bounce; total degrees)
        public float airPitch = 0.0F;
        public float prevAirPitch = 0.0F;
        public float airPitchVel = 0.0F;

        // Smooth state blends [0..1]
        public float moveBlend = 0.0F;
        public float prevMoveBlend = 0.0F;

        public float sprintBlend = 0.0F;
        public float prevSprintBlend = 0.0F;

        public float crouchBlend = 0.0F;
        public float prevCrouchBlend = 0.0F;

        public float horizontalBlend = 0.0F;
        public float prevHorizontalBlend = 0.0F;

        public float swimWaterBlend = 0.0F;
        public float prevSwimWaterBlend = 0.0F;

        public float crawlBlend = 0.0F;
        public float prevCrawlBlend = 0.0F;

        public float flyBlend = 0.0F;
        public float prevFlyBlend = 0.0F;

        public float mountBlend = 0.0F;
        public float prevMountBlend = 0.0F;

        public float sleepBlend = 0.0F;
        public float prevSleepBlend = 0.0F;

        // Idle tip-flick state (organic gecko/iguana tail-tip flicks)
        public int flickCooldown = 70;
        public int flickAge = -1;
        public int flickDuration = 20;
        public float flickDir = 1.0F;
        public float flickStrength = 1.0F;
        public float flickAmount = 0.0F;
        public float prevFlickAmount = 0.0F;

        // Mount gallop sway
        public float mountBob = 0.0F;
        public float prevMountBob = 0.0F;

        // Kinematics tracking
        public double lastX = 0.0D;
        public double lastY = 0.0D;
        public double lastZ = 0.0D;
        public float lastBodyYaw = 0.0F;
        public double lastVelY = 0.0D;
        public boolean wasOnGround = true;
        public int lastSimTick = -1;
        public boolean initialized = false;

        public void init(LivingEntity entity) {
            this.lastX = entity.getX();
            this.lastY = entity.getY();
            this.lastZ = entity.getZ();
            this.lastBodyYaw = entity.yBodyRot;
            this.lastVelY = 0.0D;
            this.wasOnGround = entity.onGround();

            java.util.Arrays.fill(this.lagYaw, 0.0F);
            java.util.Arrays.fill(this.prevLagYaw, 0.0F);
            java.util.Arrays.fill(this.lagYawVel, 0.0F);

            this.airPitch = 0.0F;
            this.prevAirPitch = 0.0F;
            this.airPitchVel = 0.0F;

            this.moveBlend = 0.0F;
            this.prevMoveBlend = 0.0F;
            this.sprintBlend = 0.0F;
            this.prevSprintBlend = 0.0F;
            this.crouchBlend = entity.isCrouching() ? 1.0F : 0.0F;
            this.prevCrouchBlend = this.crouchBlend;

            boolean isSleeping = entity.isSleeping() || entity.getPose() == Pose.SLEEPING;
            boolean isElytra = entity.isFallFlying() || entity.getPose() == Pose.FALL_FLYING;
            boolean isSwimPose = entity.isVisuallySwimming() || entity.getPose() == Pose.SWIMMING;
            boolean isHorizontal = isSleeping || isElytra || isSwimPose;

            this.horizontalBlend = isHorizontal ? 1.0F : 0.0F;
            this.prevHorizontalBlend = this.horizontalBlend;
            this.swimWaterBlend = (isSwimPose && entity.isInWater()) ? 1.0F : 0.0F;
            this.prevSwimWaterBlend = this.swimWaterBlend;
            this.crawlBlend = (isSwimPose && !entity.isInWater()) ? 1.0F : 0.0F;
            this.prevCrawlBlend = this.crawlBlend;
            this.flyBlend = (isElytra || (entity instanceof Player p && p.getAbilities().flying)) ? 1.0F : 0.0F;
            this.prevFlyBlend = this.flyBlend;
            this.mountBlend = entity.isPassenger() ? 1.0F : 0.0F;
            this.prevMountBlend = this.mountBlend;
            this.sleepBlend = isSleeping ? 1.0F : 0.0F;
            this.prevSleepBlend = this.sleepBlend;

            this.flickCooldown = 60 + Math.abs(entity.getId() * 31) % 70;
            this.flickAge = -1;
            this.flickAmount = 0.0F;
            this.prevFlickAmount = 0.0F;
            this.mountBob = 0.0F;
            this.prevMountBob = 0.0F;

            this.initialized = true;
        }

        public void step(LivingEntity entity) {
            double curX = entity.getX();
            double curY = entity.getY();
            double curZ = entity.getZ();

            double distSq = (curX - lastX) * (curX - lastX)
                    + (curY - lastY) * (curY - lastY)
                    + (curZ - lastZ) * (curZ - lastZ);

            if (!initialized || distSq > 64.0D) {
                init(entity);
                return;
            }

            // Snapshot previous tick state for partialTick interpolation
            System.arraycopy(lagYaw, 0, prevLagYaw, 0, NUM_SEGMENTS);
            prevAirPitch = airPitch;
            prevMoveBlend = moveBlend;
            prevSprintBlend = sprintBlend;
            prevCrouchBlend = crouchBlend;
            prevHorizontalBlend = horizontalBlend;
            prevSwimWaterBlend = swimWaterBlend;
            prevCrawlBlend = crawlBlend;
            prevFlyBlend = flyBlend;
            prevMountBlend = mountBlend;
            prevSleepBlend = sleepBlend;
            prevFlickAmount = flickAmount;
            prevMountBob = mountBob;

            // 1. Displacement & local velocities
            double dx = curX - lastX;
            double dy = curY - lastY;
            double dz = curZ - lastZ;
            lastX = curX;
            lastY = curY;
            lastZ = curZ;

            float yawRad = entity.yBodyRot * Mth.DEG_TO_RAD;
            // Right-handed Minecraft horizontal projection
            double strafeSpeed = dx * Mth.cos(yawRad) + dz * Mth.sin(yawRad);
            double horizSpeed = Math.sqrt(dx * dx + dz * dz);

            float deltaBodyYaw = Mth.clamp(Mth.wrapDegrees(entity.yBodyRot - lastBodyYaw), -45.0F, 45.0F);
            lastBodyYaw = entity.yBodyRot;

            // 2. Detect player posture & movement states
            boolean isSleeping = entity.isSleeping() || entity.getPose() == Pose.SLEEPING;
            boolean isElytra = entity.isFallFlying() || entity.getPose() == Pose.FALL_FLYING;
            boolean isCreativeFly = (entity instanceof Player p && p.getAbilities().flying && !entity.onGround());
            boolean isPassenger = entity.isPassenger() && entity.getVehicle() != null;

            float rawSwimAmount = (entity instanceof Player p) ? p.getSwimAmount(1.0F) : 0.0F;
            boolean isSwimPose = entity.isVisuallySwimming() || entity.getPose() == Pose.SWIMMING || rawSwimAmount > 0.05F;
            boolean isInWater = entity.isInWater()
                    || entity.isInFluidType((fluidType, height) -> entity.canSwimInFluidType(fluidType));

            boolean isSwimmingInWater = (isSwimPose && isInWater) || (isInWater && !entity.onGround() && !isPassenger);
            boolean isCrawlingOnLand = isSwimPose && !isInWater && !isElytra && !isSleeping;
            boolean isCrouching = entity.isCrouching() && !isSwimPose && !isSleeping && !isPassenger && !isElytra;

            float limbSpeed = Math.min(entity.walkAnimation.speed(), 1.0F);
            float effectiveMove = Mth.clamp((float) Math.max(limbSpeed, horizSpeed * 4.2D), 0.0F, 1.0F);
            boolean isSprinting = entity.isSprinting() && effectiveMove > 0.2F && !isCrouching && !isSwimPose && !isPassenger;

            // Target horizontal alignment (vanilla pitches player horizontal during swim, crawl, elytra, sleep)
            float targetHorizontal = 0.0F;
            if (isSleeping || isElytra) {
                targetHorizontal = 1.0F;
            } else if (isSwimPose) {
                targetHorizontal = Math.max(rawSwimAmount, 1.0F);
            }

            // Smooth state blending
            moveBlend = Mth.lerp(0.22F, moveBlend, (isPassenger || isSleeping) ? 0.0F : effectiveMove);
            sprintBlend = Mth.lerp(0.18F, sprintBlend, isSprinting ? 1.0F : 0.0F);
            crouchBlend = Mth.lerp(0.28F, crouchBlend, isCrouching ? 1.0F : 0.0F);
            horizontalBlend = Mth.lerp(0.25F, horizontalBlend, targetHorizontal);
            swimWaterBlend = Mth.lerp(0.22F, swimWaterBlend, isSwimmingInWater ? 1.0F : 0.0F);
            crawlBlend = Mth.lerp(0.22F, crawlBlend, isCrawlingOnLand ? 1.0F : 0.0F);
            flyBlend = Mth.lerp(0.20F, flyBlend, (isElytra || isCreativeFly) ? 1.0F : 0.0F);
            mountBlend = Mth.lerp(0.25F, mountBlend, isPassenger ? 1.0F : 0.0F);
            sleepBlend = Mth.lerp(0.22F, sleepBlend, isSleeping ? 1.0F : 0.0F);

            // 3. Turning & Strafe Inertia Chain (propagating spring-damped wave down the 17 segments)
            // Verified sign:
            //   deltaBodyYaw > 0 (turning RIGHT) -> tail lags to player's RIGHT (-X) -> NEGATIVE rotY
            //   strafeSpeed > 0 (strafing LEFT)  -> tail lags to player's RIGHT (-X) -> NEGATIVE rotY
            float inertiaScale = (isSleeping ? 0.05F : (isPassenger ? 0.35F : 1.0F));
            float rootImpulse = (-deltaBodyYaw * 0.42F - (float) strafeSpeed * 24.0F) * inertiaScale;
            rootImpulse = Mth.clamp(rootImpulse, -18.0F, 18.0F);

            // Segment 0 spring returns smoothly toward 0 while absorbing turn/strafe impulse
            lagYawVel[0] = (lagYawVel[0] + rootImpulse * 0.48F - lagYaw[0] * 0.24F) * 0.62F;
            lagYaw[0] = Mth.clamp(lagYaw[0] + lagYawVel[0], -16.0F, 16.0F);

            // Segments 1..16 follow predecessor with a traveling whip delay
            for (int i = 1; i < NUM_SEGMENTS; i++) {
                float u = (float) i / (float) (NUM_SEGMENTS - 1);
                // Slight progressive whip coupling near the lighter distal half
                float followGain = Mth.lerp(u, 0.90F, 0.78F);
                float stiffness = Mth.lerp(u, 0.34F, 0.21F);
                float damping = Mth.lerp(u, 0.64F, 0.69F);

                float target = lagYaw[i - 1] * followGain;
                // Add subtle extra angular momentum whip at the slender tip during sharp turns
                if (i >= 10) {
                    target += (lagYaw[i - 1] - lagYaw[i]) * 0.12F;
                }

                float accel = (target - lagYaw[i]) * stiffness;
                lagYawVel[i] = (lagYawVel[i] + accel) * damping;
                float maxSegLag = Mth.lerp(u, 11.5F, 9.5F);
                lagYaw[i] = Mth.clamp(lagYaw[i] + lagYawVel[i], -maxSegLag, maxSegLag);
            }

            // 4. Vertical Jump / Fall / Landing Spring
            boolean onGround = entity.onGround();
            float targetAirPitch = 0.0F;

            if (!isSleeping && !isPassenger && !isElytra && !isSwimmingInWater && !isCrawlingOnLand) {
                if (!onGround) {
                    if (dy > 0.03D) {
                        // Rising jump: tail trails slightly downward (+rotX)
                        targetAirPitch = (float) Mth.clamp(dy * 42.0D, 0.0D, 20.0D);
                    } else if (dy < -0.04D) {
                        // Falling: air drag lifts tail gracefully upward (-rotX)
                        targetAirPitch = (float) Mth.clamp(dy * 58.0D, -42.0D, 0.0D);
                    }
                } else if (!wasOnGround && lastVelY < -0.16D) {
                    // Landing impact: brief downward dip + rebound
                    float landImpulse = (float) Mth.clamp(-lastVelY * 14.0D, 2.5D, 10.5D);
                    airPitchVel += landImpulse;
                }
            }

            float airStiffness = onGround ? 0.28F : 0.20F;
            float airDamping = onGround ? 0.64F : 0.70F;
            airPitchVel = (airPitchVel + (targetAirPitch - airPitch) * airStiffness) * airDamping;
            airPitch = Mth.clamp(airPitch + airPitchVel, -45.0F, 24.0F);

            lastVelY = dy;
            wasOnGround = onGround;

            // 5. Occasional Organic Idle Tip Flicks (tail11..tail17)
            boolean canFlick = (moveBlend < 0.15F && !isSleeping && !isSwimmingInWater && !isElytra);
            if (flickAge >= 0) {
                flickAge++;
                if (flickAge >= flickDuration) {
                    flickAge = -1;
                    flickAmount = 0.0F;
                    // Deterministic pseudo-random interval (3.5s - 7.5s)
                    int hash = Math.abs((entity.tickCount * 1103515245 + entity.getId() * 12345) >>> 16);
                    flickCooldown = 70 + (hash % 85);
                } else {
                    float t = (float) flickAge / (float) flickDuration;
                    // Smooth double-humped or single-whip organic flick curve
                    flickAmount = Mth.sin(t * Mth.PI) * flickDir * flickStrength;
                }
            } else {
                flickAmount = 0.0F;
                if (canFlick) {
                    flickCooldown--;
                    if (flickCooldown <= 0) {
                        int hash = Math.abs((entity.tickCount * 1664525 + entity.getId() * 1013904223) >>> 16);
                        flickDir = ((hash & 1) == 0) ? 1.0F : -1.0F;
                        flickStrength = 0.75F + ((hash % 50) / 100.0F);
                        flickDuration = 16 + (hash % 10);
                        flickAge = 0;
                    }
                }
            }

            // 6. Mount gallop sway (if riding a LivingEntity like a horse)
            float targetMountBob = 0.0F;
            if (isPassenger && entity.getVehicle() instanceof LivingEntity mount) {
                float mSpeed = Math.min(mount.walkAnimation.speed(), 1.0F);
                float mPos = mount.walkAnimation.position();
                targetMountBob = Mth.sin(mPos * 0.75F) * 5.0F * mSpeed;
            }
            mountBob = Mth.lerp(0.30F, mountBob, targetMountBob);
        }
    }

    private static final Map<LivingEntity, TailState> STATES = new WeakHashMap<>();

    public static TailState getOrCreateState(LivingEntity entity) {
        return STATES.computeIfAbsent(entity, e -> new TailState());
    }

    /**
     * Updates simulation (once per tick) and applies smooth procedural lizard tail rotations
     * to bones "tail", "tail2", ..., "tail17".
     */
    public static void applyTailAnimation(LivingEntity entity,
                                          Function<String, GeoBone> boneFinder,
                                          float partialTick) {
        if (entity == null || boneFinder == null) return;

        TailState state = getOrCreateState(entity);

        if (entity.tickCount != state.lastSimTick) {
            state.step(entity);
            state.lastSimTick = entity.tickCount;
        }

        // Interpolated state weights for smooth sub-tick rendering
        float moveBlend = Mth.lerp(partialTick, state.prevMoveBlend, state.moveBlend);
        float sprintBlend = Mth.lerp(partialTick, state.prevSprintBlend, state.sprintBlend);
        float crouchBlend = Mth.lerp(partialTick, state.prevCrouchBlend, state.crouchBlend);
        float horizontalBlend = Mth.lerp(partialTick, state.prevHorizontalBlend, state.horizontalBlend);
        float swimWaterBlend = Mth.lerp(partialTick, state.prevSwimWaterBlend, state.swimWaterBlend);
        float crawlBlend = Mth.lerp(partialTick, state.prevCrawlBlend, state.crawlBlend);
        float flyBlend = Mth.lerp(partialTick, state.prevFlyBlend, state.flyBlend);
        float mountBlend = Mth.lerp(partialTick, state.prevMountBlend, state.mountBlend);
        float sleepBlend = Mth.lerp(partialTick, state.prevSleepBlend, state.sleepBlend);
        float airPitch = Mth.lerp(partialTick, state.prevAirPitch, state.airPitch);
        float flickAmount = Mth.lerp(partialTick, state.prevFlickAmount, state.flickAmount);
        float mountBob = Mth.lerp(partialTick, state.prevMountBob, state.mountBob);

        float ageInTicks = entity.tickCount + partialTick;
        float walkPos = entity.walkAnimation.position(partialTick);
        float walkSpeed = Math.min(entity.walkAnimation.speed(partialTick), 1.0F);

        // Read synced torso pitch so we can counter-pitch vanilla crouch lean cleanly
        float bodyRotXDeg = 0.0F;
        GeoBone bodyBone = boneFinder.apply("body");
        if (bodyBone != null) {
            bodyRotXDeg = bodyBone.getRotX() * Mth.RAD_TO_DEG;
        }

        // Upright weight (0 when swimming/crawling/elytra/sleeping)
        float uprightWeight = Mth.clamp(1.0F - horizontalBlend, 0.0F, 1.0F);
        // Ground-standing weight (upright, not mounted, not creative flying)
        float standingWeight = uprightWeight * (1.0F - mountBlend);
        float idleWeight = standingWeight * (1.0F - moveBlend);
        float walkWeight = standingWeight * moveBlend * (1.0F - sprintBlend);
        float sprintWeight = standingWeight * moveBlend * sprintBlend;

        // Creative flight (upright flying, not elytra horizontal)
        float creativeFlyWeight = flyBlend * uprightWeight * (1.0F - mountBlend);
        // Elytra horizontal flight
        float elytraWeight = flyBlend * horizontalBlend;

        // Water swim intensity scales cleanly between treading water and active swimming
        float swimDrive = 0.55F + 0.45F * moveBlend;

        for (int i = 0; i < NUM_SEGMENTS; i++) {
            GeoBone bone = boneFinder.apply(BONE_NAMES[i]);
            if (bone == null) continue;

            float u = (float) i / (float) (NUM_SEGMENTS - 1);
            // Progressive flexibility curve: stiffer muscular base, suppler whippy tip
            float flex = 0.25F + 0.75F * (u * u * 0.55F + u * 0.45F);

            // =====================================================================
            // 1. PITCH (rotX): Base posture + crouch counter-pitch + air drag + breath
            // =====================================================================

            // 1a. Upright hover vs sprint posture
            float uprightBasePitch = Mth.lerp(sprintBlend, HOVER_PITCH_DEG[i], SPRINT_PITCH_DEG[i]);

            // While crouching, reduce base droop slightly so lowered pelvis keeps tail cleanly above floor
            uprightBasePitch *= Mth.lerp(crouchBlend, 1.0F, 0.72F);

            // While creative flying (upright in air), lift tail into a graceful floating arch
            uprightBasePitch *= Mth.lerp(creativeFlyWeight, 1.0F, 0.50F);

            // While riding a mount/boat, lift base to clear saddle/horse hindquarters cleanly
            float mountPitch = HOVER_PITCH_DEG[i] * 0.42F + (i < 5 ? -2.0F : 0.5F);
            float verticalModePitch = Mth.lerp(mountBlend, uprightBasePitch, mountPitch);

            // 1b. Blend upright posture with horizontal posture (swim / crawl / elytra / sleep)
            float pitchDeg = Mth.lerp(horizontalBlend, verticalModePitch, HORIZONTAL_PITCH_DEG[i]);

            // 1c. Counter-pitch vanilla crouching torso lean (-bodyRotXDeg cancels torso's upward tilt of +Z)
            if (i < CROUCH_COUNTER_WEIGHT.length && crouchBlend > 0.001F && uprightWeight > 0.001F) {
                pitchDeg += (-bodyRotXDeg) * CROUCH_COUNTER_WEIGHT[i] * crouchBlend * uprightWeight;
            }

            // 1d. Vertical air dynamics (jump dip, fall lift, landing bounce)
            pitchDeg += airPitch * AIR_PITCH_WEIGHT[i] * uprightWeight;

            // 1e. Subtle vertical breathing / stride bounce
            float idleBreathPitch = Mth.sin(ageInTicks * 0.055F - i * 0.28F) * (0.20F + 0.75F * u) * idleWeight;
            float walkBouncePitch = Mth.sin(walkPos * 1.3324F - i * 0.35F) * (0.35F + 0.90F * u) * walkWeight * walkSpeed;
            float sprintBouncePitch = Mth.sin(walkPos * 1.3324F - i * 0.40F) * (0.45F + 1.15F * u) * sprintWeight;
            float flyFlutterPitch = Mth.sin(ageInTicks * 0.42F - i * 0.45F) * (0.35F + 2.20F * u * u) * (elytraWeight + creativeFlyWeight * 0.55F);
            float mountBouncePitch = mountBob * AIR_PITCH_WEIGHT[i] * 2.2F * mountBlend;
            float sleepBreathPitch = Mth.sin(ageInTicks * 0.045F - i * 0.20F) * (0.15F + 0.35F * u) * sleepBlend;

            pitchDeg += idleBreathPitch + walkBouncePitch + sprintBouncePitch
                    + flyFlutterPitch + mountBouncePitch + sleepBreathPitch;

            // Slight upward curl on the tip during an idle tip-flick
            if (i >= 10 && Math.abs(flickAmount) > 0.001F) {
                float tipFactor = (float) (i - 9) / (float) (NUM_SEGMENTS - 10);
                pitchDeg -= Math.abs(flickAmount) * (1.8F * tipFactor) * idleWeight;
            }

            // =====================================================================
            // 2. YAW (rotY): Traveling waves + turning inertia + pose sweeps
            // =====================================================================

            // 2a. Spring-damped turning & strafe inertia (interpolated)
            float inertiaYaw = Mth.lerp(partialTick, state.prevLagYaw[i], state.lagYaw[i]);

            // 2b. Idle hypnotic traveling S-wave
            float idleWaveYaw = Mth.sin(ageInTicks * 0.068F - i * 0.34F)
                    * Mth.lerp(flex, 0.75F, 4.20F) * idleWeight;

            // 2c. Walk & sprint counterbalance S-waves synced to vanilla stride (0.6662F)
            float walkWaveYaw = Mth.sin(walkPos * 0.6662F - i * 0.38F)
                    * Mth.lerp(flex, 1.85F, 5.40F) * walkWeight * Math.max(walkSpeed, 0.35F);

            float sprintWaveYaw = Mth.sin(walkPos * 0.6662F - i * 0.44F)
                    * Mth.lerp(flex, 2.50F, 7.20F) * sprintWeight;

            // 2d. Swimming crocodilian / iguana lateral sculling propulsion
            float swimWaveYaw = Mth.sin(ageInTicks * 0.34F - i * 0.38F)
                    * Mth.lerp(flex, 2.20F, 10.50F) * swimWaterBlend * swimDrive;

            // 2e. Land crawling subtle horizontal slither
            float crawlWaveYaw = Mth.sin((walkPos * 0.75F + ageInTicks * 0.06F) - i * 0.36F)
                    * Mth.lerp(flex, 1.20F, 4.80F) * crawlBlend;

            // 2f. Elytra / creative flight aerodynamic flutter
            float flyWaveYaw = Mth.sin(ageInTicks * 0.56F - i * 0.50F)
                    * (0.40F + 2.80F * u * u) * (elytraWeight + creativeFlyWeight * 0.60F);

            // 2g. Riding mount: gentle side-sweep out from behind the saddle + subtle sway
            float mountSweepYaw = 0.0F;
            if (mountBlend > 0.001F) {
                float sideCurve = (i < 7) ? (5.5F - i * 0.35F) : (1.5F - (i - 7) * 0.25F);
                float mountIdleSway = Mth.sin(ageInTicks * 0.08F - i * 0.30F) * (0.5F + 2.2F * u);
                mountSweepYaw = (sideCurve + mountIdleSway) * mountBlend;
            }

            // 2h. Sleeping: relaxed curl along the bed toward one side
            float sleepCurlYaw = 0.0F;
            if (sleepBlend > 0.001F) {
                float curl = (i >= 3) ? (3.6F + u * 2.4F) : 1.0F;
                float sleepSway = Mth.sin(ageInTicks * 0.04F - i * 0.25F) * (0.2F + 0.8F * u);
                sleepCurlYaw = (curl + sleepSway) * sleepBlend;
            }

            // 2i. Idle organic tip flick (localized to tail11..tail17)
            float tipFlickYaw = 0.0F;
            if (i >= 10 && Math.abs(flickAmount) > 0.001F) {
                float tipFactor = (float) (i - 9) / (float) (NUM_SEGMENTS - 10);
                tipFlickYaw = flickAmount * (7.5F * tipFactor * tipFactor) * idleWeight;
            }

            float yawDeg = inertiaYaw + idleWaveYaw + walkWaveYaw + sprintWaveYaw
                    + swimWaveYaw + crawlWaveYaw + flyWaveYaw
                    + mountSweepYaw + sleepCurlYaw + tipFlickYaw;

            // =====================================================================
            // 3. SAFETY CLAMPS & SUBTLE DORSAL-FIN BANKING ROLL (rotZ)
            // =====================================================================
            // Keep per-joint bends comfortably within the model's 1px cube overlap tolerance
            float maxPitchPos = (i <= 1) ? 38.0F : 26.0F;
            float maxPitchNeg = (i <= 1) ? -24.0F : -20.0F;
            pitchDeg = Mth.clamp(pitchDeg, maxPitchNeg, maxPitchPos);

            float maxYaw = (i <= 2) ? 18.0F : 16.0F;
            yawDeg = Mth.clamp(yawDeg, -maxYaw, maxYaw);

            // Subtle natural banking of the dorsal fin into curves (disabled when sleeping)
            float rollDeg = Mth.clamp(-yawDeg * 0.14F * (1.0F - sleepBlend), -3.5F, 3.5F);

            var initial = bone.getInitialSnapshot();
            bone.setRotX(initial.getRotX() + pitchDeg * Mth.DEG_TO_RAD);
            bone.setRotY(initial.getRotY() + yawDeg * Mth.DEG_TO_RAD);
            bone.setRotZ(initial.getRotZ() + rollDeg * Mth.DEG_TO_RAD);
        }
    }
}
