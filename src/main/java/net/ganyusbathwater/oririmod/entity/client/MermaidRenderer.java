package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.MermaidEntity;
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
 * Renderer for the MermaidEntity.
 */
public class MermaidRenderer extends GeoEntityRenderer<MermaidEntity> {

    private static final ResourceLocation SKIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid.png");
    private static final ResourceLocation HAIR_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid_hair_template.png");
    private static final ResourceLocation FIN_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid_fin_template.png");

    private static final String[] DRIP_BONES = {
        "left_arm_fin", "right_arm_fin", "left_leg_fin", "right_leg_fin"
    };

    private int particleTick = 0;

    public MermaidRenderer(EntityRendererProvider.Context context) {
        super(context, new MermaidModel());
        this.addRenderLayer(new MermaidTintLayer(this, HAIR_TEMPLATE, MermaidEntity::getHairColor));
        this.addRenderLayer(new MermaidTintLayer(this, FIN_TEMPLATE, MermaidEntity::getFinColor));
    }

    @Override
    public void render(MermaidEntity entity, float entityYaw, float partialTick,
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
            if (particleTick >= 35) { // every ~35 render frames (decreased amount)
                particleTick = 0;
                spawnDripParticles(entity, poseStack);
            }
        } else {
            particleTick = 0;
        }
    }

    @Override
    protected float getDeathMaxRotation(MermaidEntity animatable) {
        return 0f;
    }

    private void spawnDripParticles(MermaidEntity entity, PoseStack poseStack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // Pick one random bone instead of looping all four
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

    private static class MermaidTintLayer extends GeoRenderLayer<MermaidEntity> {

        private final ResourceLocation templateTexture;
        private final java.util.function.ToIntFunction<MermaidEntity> colorProvider;

        public MermaidTintLayer(GeoEntityRenderer<MermaidEntity> renderer, ResourceLocation texture, java.util.function.ToIntFunction<MermaidEntity> colorProvider) {
            super(renderer);
            this.templateTexture = texture;
            this.colorProvider = colorProvider;
        }

        @Override
        public void render(PoseStack poseStack, MermaidEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel bakedModel,
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
}
