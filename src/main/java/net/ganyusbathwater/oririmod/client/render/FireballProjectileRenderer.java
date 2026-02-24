package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.FireballProjectileEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class FireballProjectileRenderer extends EntityRenderer<FireballProjectileEntity> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                        "textures/entity/fireball_projectile.png");
        private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

        public FireballProjectileRenderer(EntityRendererProvider.Context context) {
                super(context);
        }

        @Override
        public void render(FireballProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
                poseStack.pushPose();

                float scale = entity.getScale() * 1.5F;
                poseStack.scale(scale, scale, scale);

                // Align rendering to velocity vector
                net.minecraft.world.phys.Vec3 velocity = entity.getDeltaMovement();
                // If velocity is 0, it might use the previous tick's rotation, but we'll
                // calculate from velocity directly
                double horizontalDistanceSqr = velocity.x * velocity.x + velocity.z * velocity.z;
                float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * (double) (180F / (float) Math.PI));
                float pitch = (float) (Mth.atan2(velocity.y, Math.sqrt(horizontalDistanceSqr))
                                * (double) (180F / (float) Math.PI));

                // Smoothly interpolate if needed, but for simple projectiles direct velocity
                // alignment often works best
                // Minecraft's entityYaw/Pitch are usually what Mth.lerp(partialTicks,
                // entity.yRotO, entity.getYRot()) uses,
                // but AbstractHurtingProjectile doesn't automatically update them based on
                // velocity.
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

                // Rotate 45 degrees around Z so the planes form an 'X' shape relative to the
                // travel direction
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));

                VertexConsumer vc = buffer.getBuffer(RENDER_TYPE);
                int overlay = OverlayTexture.NO_OVERLAY;
                int light = packedLight; // Or LightTexture.FULL_BRIGHT if it glows

                // Calculate animation frame (16x128 texture gives 8 frames)
                int frame = (entity.tickCount / 2) % 8;
                float v0 = frame * 0.125F;
                float v1 = (frame + 1) * 0.125F;

                float hLength = 0.5F;
                float hWidth = 0.5F;

                // Draw cross plane 1 (Vertical-ish Plane)
                PoseStack.Pose last = poseStack.last();
                Matrix4f poseMat = last.pose();

                // We want the "bottom" of the texture (v=v1) to be at the front (z=hLength)
                // We want the "top" of the texture (v=v0) to be at the back (z=-hLength)
                // USER CLARIFICATION: The texture is appearing backwards. Swap V coordinates.

                drawVertex(poseMat, last, vc, -hWidth, 0.0F, -hLength, 1.0F, v1, overlay, light); // Top right (back)
                drawVertex(poseMat, last, vc, hWidth, 0.0F, -hLength, 0.0F, v1, overlay, light); // Top left (back)
                drawVertex(poseMat, last, vc, hWidth, 0.0F, hLength, 0.0F, v0, overlay, light); // Bottom left (front)
                drawVertex(poseMat, last, vc, -hWidth, 0.0F, hLength, 1.0F, v0, overlay, light); // Bottom right (front)

                // Draw cross plane 2 (Horizontal-ish Plane, rotated 90 degrees around Z)
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                last = poseStack.last();
                poseMat = last.pose();

                drawVertex(poseMat, last, vc, -hWidth, 0.0F, -hLength, 1.0F, v1, overlay, light); // Top right (back)
                drawVertex(poseMat, last, vc, hWidth, 0.0F, -hLength, 0.0F, v1, overlay, light); // Top left (back)
                drawVertex(poseMat, last, vc, hWidth, 0.0F, hLength, 0.0F, v0, overlay, light); // Bottom left (front)
                drawVertex(poseMat, last, vc, -hWidth, 0.0F, hLength, 1.0F, v0, overlay, light); // Bottom right (front)

                poseStack.popPose();
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }

        private void drawVertex(Matrix4f poseMat, PoseStack.Pose last, VertexConsumer vc,
                        float x, float y, float z, float u, float v, int overlay, int light) {
                vc.addVertex(poseMat, x, y, z)
                                .setColor(255, 255, 255, 255)
                                .setUv(u, v)
                                .setOverlay(overlay)
                                .setLight(light)
                                .setNormal(last, 0.0F, 1.0F, 0.0F); // simplified normal
        }

        @Override
        public ResourceLocation getTextureLocation(FireballProjectileEntity entity) {
                return TEXTURE;
        }
}
