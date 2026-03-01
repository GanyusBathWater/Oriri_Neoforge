package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.RootVisualEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RootVisualRenderer extends EntityRenderer<RootVisualEntity> {
        private static final ResourceLocation VINE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft",
                        "textures/block/vine.png");

        public RootVisualRenderer(EntityRendererProvider.Context context) {
                super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(RootVisualEntity entity) {
                return VINE_LOCATION;
        }

        @Override
        public boolean shouldRender(RootVisualEntity livingEntity, Frustum camera, double camX, double camY,
                        double camZ) {
                return super.shouldRender(livingEntity, camera, camX, camY, camZ);
        }

        @Override
        public void render(RootVisualEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
                poseStack.pushPose();

                int age = entity.getAgeTicks();
                int lifespan = entity.getLifespan();

                // Animation: Spring up, hold, scale down
                // Animation: Spring up faster (5 ticks instead of 10), hold, scale down
                float scale = 1.0f;
                if (age < 5) {
                        scale = age / 5.0f;
                } else if (age > lifespan - 10) {
                        scale = (lifespan - age) / 10.0f;
                }

                // Apply scale, and stretch slightly taller
                poseStack.scale(scale * 1.5f, scale * 2.5f, scale * 1.5f);

                // Render double intersecting quads (like grass/vines)
                VertexConsumer vertexConsumer = buffer
                                .getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));

                drawQuad(poseStack, vertexConsumer, packedLight);

                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                drawQuad(poseStack, vertexConsumer, packedLight);

                poseStack.popPose();
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }

        private void drawQuad(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight) {
                PoseStack.Pose pose = poseStack.last();

                float minX = -0.5f;
                float maxX = 0.5f;
                float minY = 0.0f;
                float maxY = 1.0f;

                // Use a nice visible green color (R=50, G=180, B=50) to offset the grey texture
                int r = 50;
                int g = 180;
                int b = 50;

                // Front face
                vertexConsumer.addVertex(pose, minX, minY, 0.0F).setColor(r, g, b, 255).setUv(1.0F, 1.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);
                vertexConsumer.addVertex(pose, maxX, minY, 0.0F).setColor(r, g, b, 255).setUv(0.0F, 1.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);
                vertexConsumer.addVertex(pose, maxX, maxY, 0.0F).setColor(r, g, b, 255).setUv(0.0F, 0.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);
                vertexConsumer.addVertex(pose, minX, maxY, 0.0F).setColor(r, g, b, 255).setUv(1.0F, 0.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);

                // Back face
                vertexConsumer.addVertex(pose, minX, maxY, 0.0F).setColor(r, g, b, 255).setUv(1.0F, 0.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);
                vertexConsumer.addVertex(pose, maxX, maxY, 0.0F).setColor(r, g, b, 255).setUv(0.0F, 0.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);
                vertexConsumer.addVertex(pose, maxX, minY, 0.0F).setColor(r, g, b, 255).setUv(0.0F, 1.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);
                vertexConsumer.addVertex(pose, minX, minY, 0.0F).setColor(r, g, b, 255).setUv(1.0F, 1.0F)
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
                                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
}
