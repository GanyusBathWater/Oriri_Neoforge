package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.FairyEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class FairyRenderer extends GeoEntityRenderer<FairyEntity> {
    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy.png");
    private static final ResourceLocation HAIR_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy_hair.png");
    private static final ResourceLocation WING_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/fairy_wings.png");

    public FairyRenderer(EntityRendererProvider.Context context) {
        super(context, new FairyModel());
        
        this.addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected net.minecraft.world.item.ItemStack getStackForBone(GeoBone bone, FairyEntity animatable) {
                if (bone.getName().equals("item_bone") || bone.getName().equals("right_item")) {
                    return animatable.getMainHandItem();
                }
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            @Override
            protected net.minecraft.world.item.ItemDisplayContext getTransformTypeForStack(GeoBone bone, net.minecraft.world.item.ItemStack stack, FairyEntity animatable) {
                return net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, net.minecraft.world.item.ItemStack stack, FairyEntity animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                poseStack.pushPose();
                // Translate slightly to fine-tune the handle grip
                poseStack.translate(0, 0.1, -0.1);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90f));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
                super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });

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
        var level = net.minecraft.client.Minecraft.getInstance().level;
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

    private static class FairyTintLayer extends GeoRenderLayer<FairyEntity> {
        private final ResourceLocation templateTexture;
        private final java.util.function.ToIntFunction<FairyEntity> colorProvider;

        public FairyTintLayer(GeoEntityRenderer<FairyEntity> renderer, ResourceLocation texture, java.util.function.ToIntFunction<FairyEntity> colorProvider) {
            super(renderer);
            this.templateTexture = texture;
            this.colorProvider = colorProvider;
        }

        @Override
        public void render(PoseStack poseStack, FairyEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
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
