package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.FairyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public class FairyRenderer extends GeoEntityRenderer<FairyEntity> {
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy.png");
    private static final ResourceLocation HAIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy_hair.png");
    private static final ResourceLocation WING_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy_wings.png");

    public FairyRenderer(EntityRendererProvider.Context context) {
        super(context, new FairyModel());

        // Render held sword AFTER the main fairy.png pass completes so switching MultiBufferSource
        // to the item atlas never flushes/invalidates the active buffer mid-traversal under Iris/Sodium
        // (which previously caused bones hashed after 'arms'—left_arm and body—to disappear).
        this.addRenderLayer(new FairyHeldItemLayer(this));
        this.addRenderLayer(new FairyTintLayer(this, HAIR_TEXTURE, FairyEntity::getHairColor));
        this.addRenderLayer(new FairyTintLayer(this, WING_TEXTURE, FairyEntity::getWingColor));
    }

    @Override
    public ResourceLocation getTextureLocation(FairyEntity animatable) {
        return BASE_TEXTURE;
    }

    private int particleTick = 0;

    @Override
    public void render(FairyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        particleTick++;
        // ~60 fps, so 150 frames is about 2.5 seconds
        if (particleTick >= 150) {
            particleTick = 0;
            spawnShiningParticles(entity);
        }
    }

    private void spawnShiningParticles(FairyEntity entity) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        String[] bones = {"right_wing", "left_wing"};
        String boneName = bones[entity.getRandom().nextInt(bones.length)];
        GeoBone bone = this.getGeoModel().getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            float scale = 0.5f; // Match the poseStack.scale(0.5f) we did in render()
            float boneX = (bone.getPivotX() / 16f) * scale;
            float boneY = (bone.getPivotY() / 16f) * scale;
            float boneZ = (bone.getPivotZ() / 16f) * scale;

            double yawRad = Math.toRadians(-entity.yBodyRot);
            double cosYaw = Math.cos(yawRad);
            double sinYaw = Math.sin(yawRad);

            // X is lateral, Z is forward/backward
            double worldX = entity.getX() + (boneX * cosYaw - boneZ * sinYaw);
            double worldY = entity.getY() + boneY;
            double worldZ = entity.getZ() + (boneX * sinYaw + boneZ * cosYaw);

            level.addParticle(net.minecraft.core.particles.ParticleTypes.FIREWORK,
                    worldX, worldY, worldZ,
                    0, -0.02, 0);
        }
    }

    /**
     * Captures the transformed {@code item_bone} matrix during bone traversal without touching
     * {@link MultiBufferSource}, then renders the held item in {@link #render} once the base model pass finishes.
     */
    private static class FairyHeldItemLayer extends GeoRenderLayer<FairyEntity> {
        private PoseStack.Pose savedItemPose;

        public FairyHeldItemLayer(GeoEntityRenderer<FairyEntity> renderer) {
            super(renderer);
        }

        @Override
        public void preRender(PoseStack poseStack, FairyEntity animatable, BakedGeoModel bakedModel,
                              RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                              float partialTick, int packedLight, int packedOverlay) {
            this.savedItemPose = null;
        }

        @Override
        public void renderForBone(PoseStack poseStack, FairyEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                                  int packedLight, int packedOverlay) {
            String name = bone.getName();
            if (!name.equals("item_bone") && !name.equals("right_item")) {
                return;
            }
            ItemStack stack = animatable.getMainHandItem();
            if (stack.isEmpty()) {
                return;
            }

            poseStack.pushPose();
            RenderUtil.translateAndRotateMatrixForBone(poseStack, bone);
            poseStack.translate(0.0, 0.1, -0.1);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));
            this.savedItemPose = poseStack.last().copy();
            poseStack.popPose();
        }

        @Override
        public void render(PoseStack poseStack, FairyEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            if (this.savedItemPose == null) {
                return;
            }
            ItemStack stack = animatable.getMainHandItem();
            if (!stack.isEmpty()) {
                PoseStack itemPoseStack = new PoseStack();
                itemPoseStack.last().pose().set(this.savedItemPose.pose());
                itemPoseStack.last().normal().set(this.savedItemPose.normal());
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        animatable, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false,
                        itemPoseStack, bufferSource, animatable.level(), packedLight, packedOverlay, animatable.getId()
                );
            }
            this.savedItemPose = null;
        }
    }

    private static class FairyTintLayer extends GeoRenderLayer<FairyEntity> {
        private final ResourceLocation templateTexture;
        private final java.util.function.ToIntFunction<FairyEntity> colorProvider;

        public FairyTintLayer(GeoEntityRenderer<FairyEntity> renderer, ResourceLocation texture, java.util.function.ToIntFunction<FairyEntity> colorProvider) {
            super(renderer);
            this.templateTexture = texture;
            this.colorProvider = colorProvider;
        }

        @Override
        public void render(PoseStack poseStack, FairyEntity animatable, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

            int packed = colorProvider.applyAsInt(animatable);
            RenderType tintedRender = RenderType.entityCutoutNoCull(templateTexture);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay,
                    packed | 0xFF000000);
        }
    }
}
