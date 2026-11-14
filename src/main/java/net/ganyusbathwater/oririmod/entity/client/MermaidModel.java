package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.MermaidEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Dynamic GeckoLib model for the MermaidEntity.
 * Automatically switches between the water (tail) and land (legs) .geo.json
 * and .animation.json files based on the entity's current state.
 */
public class MermaidModel extends GeoModel<MermaidEntity> {

    private static final ResourceLocation MODEL_WATER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid.geo.json");
    private static final ResourceLocation MODEL_LAND =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid_land.geo.json");

    private static final ResourceLocation ANIM_WATER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid.animation.json");
    private static final ResourceLocation ANIM_LAND =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_land.animation.json");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid.png");

    @Override
    public ResourceLocation getModelResource(MermaidEntity entity) {
        return entity.isInWaterState() ? MODEL_WATER : MODEL_LAND;
    }

    @Override
    public ResourceLocation getTextureResource(MermaidEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidEntity entity) {
        return entity.isInWaterState() ? ANIM_WATER : ANIM_LAND;
    }

    private static final java.util.Map<MermaidEntity, SwimPoseState> SWIM_POSE_STATES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final class SwimPoseState {
        boolean initialized;
        int lastTick = Integer.MIN_VALUE;
        double prevX;
        double prevY;
        double prevZ;
        double smoothVx;
        double smoothVy;
        double smoothVz;
        int idleTicks;
        float prevBodyPitchDeg;
        float curBodyPitchDeg;
    }

    @Override
    public void setCustomAnimations(MermaidEntity animatable, long instanceId,
                                    AnimationState<MermaidEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        applyMermaidCustomAnimations(animatable, name -> getAnimationProcessor().getBone(name), animationState);
    }

    /**
     * Applies dynamic 3D swimming pitch to the root "mermaid" bone based on client movement trajectory,
     * compensates the "head" bone so it continues looking smoothly forward/at targets, and updates hair physics.
     */
    public static void applyMermaidCustomAnimations(
            MermaidEntity animatable,
            java.util.function.Function<String, GeoBone> boneFinder,
            AnimationState<? extends MermaidEntity> animationState) {
        float partialTick = animationState.getPartialTick();
        float bodyPitchDeg = computeSmoothedSwimPitchDeg(animatable, partialTick);

        GeoBone rootBone = boneFinder.apply("mermaid");
        if (rootBone != null) {
            float rad = bodyPitchDeg * Mth.DEG_TO_RAD;
            float baseRotX = rootBone.getInitialSnapshot().getRotX();
            float basePosY = rootBone.getInitialSnapshot().getOffsetY();
            float basePosZ = rootBone.getInitialSnapshot().getOffsetZ();

            rootBone.setRotX(baseRotX + rad);
            if (Math.abs(bodyPitchDeg) > 0.05F) {
                // Keep waist pivot (Y=12px) centered as the body tilts around the origin
                float pivotH = 12.0F;
                rootBone.setPosY(basePosY + pivotH * (1.0F - Mth.cos(rad)));
                rootBone.setPosZ(basePosZ - pivotH * Mth.sin(rad));
            } else {
                rootBone.setPosY(basePosY);
                rootBone.setPosZ(basePosZ);
            }
        }

        GeoBone head = boneFinder.apply("head");
        float netHeadYaw = 0.0F;
        float headPitch = 0.0F;
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            if (entityData != null) {
                netHeadYaw = entityData.netHeadYaw();
                headPitch = entityData.headPitch();
                float localHeadPitchDeg = Mth.clamp(headPitch - bodyPitchDeg, -45.0F, 75.0F);
                float tiltBlend = Mth.clamp(-bodyPitchDeg / 85.0F, 0.0F, 1.0F);
                float yawScale = Mth.lerp(tiltBlend, 1.0F, 0.65F);
                float effectiveYaw = Mth.clamp(netHeadYaw * yawScale, -75.0F, 75.0F);
                head.setRotX(localHeadPitchDeg * Mth.DEG_TO_RAD);
                head.setRotY(effectiveYaw * Mth.DEG_TO_RAD);
            }
        }

        net.ganyusbathwater.oririmod.client.physics.HairPhysicsController.applyHairPhysics(
                animatable,
                boneFinder,
                partialTick,
                netHeadYaw,
                headPitch
        );
    }

    private static float computeSmoothedSwimPitchDeg(MermaidEntity entity, float partialTick) {
        SwimPoseState state = SWIM_POSE_STATES.computeIfAbsent(entity, ignored -> new SwimPoseState());

        if (!entity.isInWaterState() || entity.isDeadOrDying()) {
            state.initialized = true;
            state.prevX = entity.getX();
            state.prevY = entity.getY();
            state.prevZ = entity.getZ();
            state.smoothVx = 0.0D;
            state.smoothVy = 0.0D;
            state.smoothVz = 0.0D;
            state.idleTicks = 0;
            state.prevBodyPitchDeg = 0.0F;
            state.curBodyPitchDeg = 0.0F;
            state.lastTick = entity.tickCount;
            return 0.0F;
        }

        if (!state.initialized) {
            state.initialized = true;
            state.prevX = entity.getX();
            state.prevY = entity.getY();
            state.prevZ = entity.getZ();
            state.lastTick = entity.tickCount;
        }

        if (state.lastTick != entity.tickCount) {
            int dt = Mth.clamp(entity.tickCount - state.lastTick, 1, 5);
            double instVx = (entity.getX() - state.prevX) / dt;
            double instVy = (entity.getY() - state.prevY) / dt;
            double instVz = (entity.getZ() - state.prevZ) / dt;

            if (instVx * instVx + instVy * instVy + instVz * instVz > 9.0D) {
                state.smoothVx = 0.0D;
                state.smoothVy = 0.0D;
                state.smoothVz = 0.0D;
            } else {
                state.smoothVx = Mth.lerp(0.42F, (float) state.smoothVx, (float) instVx);
                state.smoothVy = Mth.lerp(0.42F, (float) state.smoothVy, (float) instVy);
                state.smoothVz = Mth.lerp(0.42F, (float) state.smoothVz, (float) instVz);
            }

            state.prevX = entity.getX();
            state.prevY = entity.getY();
            state.prevZ = entity.getZ();
            state.lastTick = entity.tickCount;

            double horizSpeed = Math.sqrt(state.smoothVx * state.smoothVx + state.smoothVz * state.smoothVz);
            double totalSpeed = Math.sqrt(horizSpeed * horizSpeed + state.smoothVy * state.smoothVy);

            float targetPitchDeg;
            if (totalSpeed > 0.018D) {
                state.idleTicks = 0;
                float climbDeg = (float) (Mth.atan2(state.smoothVy, horizSpeed) * Mth.RAD_TO_DEG);
                // Belly-down level swim is -85 deg; ascending pitches closer to upright, descending pitches head-first
                float swimPitchDeg = Mth.clamp(-85.0F + climbDeg, -160.0F, -12.0F);
                float speedBlend = Mth.clamp((float) ((totalSpeed - 0.018D) / 0.055D), 0.0F, 1.0F);
                targetPitchDeg = Mth.lerp(speedBlend, 0.0F, swimPitchDeg);
            } else {
                state.idleTicks++;
                targetPitchDeg = state.idleTicks > 4 ? 0.0F : state.curBodyPitchDeg;
            }

            state.prevBodyPitchDeg = state.curBodyPitchDeg;
            state.curBodyPitchDeg = Mth.lerp(0.28F, state.curBodyPitchDeg, targetPitchDeg);
        }

        return Mth.lerp(partialTick, state.prevBodyPitchDeg, state.curBodyPitchDeg);
    }
}
