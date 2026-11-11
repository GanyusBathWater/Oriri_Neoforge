package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.SilentNereidEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renderer for the SilentNereidEntity.
 *
 * Renders:
 * - Base skin tinted with the mixed color (mixture between hair and scales).
 * - Scale 2 layer tinted with the mixed color.
 * - Scale 1 layer tinted with the scale color.
 * - Hair layer tinted with the hair color.
 * - Emissive glow layer at full brightness.
 */
public class SilentNereidRenderer extends GeoEntityRenderer<SilentNereidEntity> {

    private static final ResourceLocation HAIR_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_hair.png");
    private static final ResourceLocation SCALE_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_scale.png");
    private static final ResourceLocation SCALE2_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_scale2.png");
    private static final ResourceLocation EMISSIVE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/silent_nereids_emissive.png");

    private static final String[] DRIP_BONES = {
            "left_arm_fin", "right_arm_fin", "left_leg_fin", "right_leg_fin"
    };

    private int particleTick = 0;

    public SilentNereidRenderer(EntityRendererProvider.Context context) {
        super(context, new SilentNereidModel());
        // Layer 1: Scale 2 (color mixture of scales and hair)
        this.addRenderLayer(new SilentNereidTintLayer(this, SCALE2_TEMPLATE, SilentNereidEntity::getMixedColor));
        // Layer 2: Scale 1 (primary scale color)
        this.addRenderLayer(new SilentNereidTintLayer(this, SCALE_TEMPLATE, SilentNereidEntity::getFinColor));
        // Layer 3: Hair (hair color)
        this.addRenderLayer(new SilentNereidTintLayer(this, HAIR_TEMPLATE, SilentNereidEntity::getHairColor));
        // Layer 4: Emissive glow
        this.addRenderLayer(new SilentNereidEmissiveLayer(this, EMISSIVE_TEXTURE));
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(SilentNereidEntity animatable, float partialTick, int packedLight) {
        // Base skin texture gets the same mixed color as the scale2 texture
        return software.bernie.geckolib.util.Color.ofOpaque(animatable.getMixedColor());
    }

    @Override
    public void render(SilentNereidEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        if (entity.tickCount < 2) {
            poseStack.pushPose();
            poseStack.scale(0f, 0f, 0f);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
            return;
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (!entity.isInWaterState()) {
            particleTick++;
            if (particleTick >= 35) {
                particleTick = 0;
                spawnDripParticles(entity, poseStack);
            }
        } else {
            particleTick = 0;
        }
    }

    @Override
    protected float getDeathMaxRotation(SilentNereidEntity animatable) {
        return 0f;
    }

    private void spawnDripParticles(SilentNereidEntity entity, PoseStack poseStack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        String boneName = DRIP_BONES[entity.getRandom().nextInt(DRIP_BONES.length)];
        GeoBone bone = this.getGeoModel().getAnimationProcessor().getBone(boneName);
        if (bone != null) {
            float boneX = bone.getPivotX() / 16f;
            float boneY = bone.getPivotY() / 16f;
            float boneZ = bone.getPivotZ() / 16f;

            double yawRad = Math.toRadians(entity.getYRot());
            double cosYaw = Math.cos(yawRad);
            double sinYaw = Math.sin(yawRad);
            double worldX = entity.getX() + (boneX * cosYaw - boneZ * sinYaw);
            double worldY = entity.getY() + boneY;
            double worldZ = entity.getZ() + (boneX * sinYaw + boneZ * cosYaw);

            level.addParticle(ParticleTypes.FALLING_WATER,
                    worldX, worldY, worldZ,
                    0, -0.05, 0);
        }
    }

    private static class SilentNereidTintLayer extends GeoRenderLayer<SilentNereidEntity> {

        private final ResourceLocation templateTexture;
        private final java.util.function.ToIntFunction<SilentNereidEntity> colorProvider;

        public SilentNereidTintLayer(GeoEntityRenderer<SilentNereidEntity> renderer,
                                     ResourceLocation texture,
                                     java.util.function.ToIntFunction<SilentNereidEntity> colorProvider) {
            super(renderer);
            this.templateTexture = texture;
            this.colorProvider = colorProvider;
        }

        @Override
        public void render(PoseStack poseStack, SilentNereidEntity animatable,
                           software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

            int packed = colorProvider.applyAsInt(animatable);
            float r = ((packed >> 16) & 0xFF) / 255f;
            float g = ((packed >> 8)  & 0xFF) / 255f;
            float b = ( packed        & 0xFF) / 255f;

            RenderType tintedRender = RenderType.entityCutoutNoCull(templateTexture);
            VertexConsumer tintedBuffer = bufferSource.getBuffer(tintedRender);

            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    tintedRender, tintedBuffer, partialTick, packedLight, packedOverlay,
                    (int)(r * 255) << 16 | (int)(g * 255) << 8 | (int)(b * 255) | 0xFF000000);
        }
    }

    private static class SilentNereidEmissiveLayer extends GeoRenderLayer<SilentNereidEntity> {

        private final ResourceLocation emissiveTexture;

        public SilentNereidEmissiveLayer(GeoEntityRenderer<SilentNereidEntity> renderer, ResourceLocation emissiveTexture) {
            super(renderer);
            this.emissiveTexture = emissiveTexture;
        }

        @Override
        public void render(PoseStack poseStack, SilentNereidEntity animatable,
                           software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

            RenderType emissiveRenderType = RenderType.entityTranslucentEmissive(emissiveTexture, false);
            VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissiveRenderType);

            // Light level 15728880 = LightTexture.FULL_BRIGHT (0xF000F0)
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                    emissiveRenderType, emissiveBuffer, partialTick, 15728880, packedOverlay, 0xFFFFFFFF);
        }
    }
}
