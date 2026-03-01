package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.DoomClockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class DoomClockRenderer extends EntityRenderer<DoomClockEntity> {

    private static final ResourceLocation RING_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/entity/doom_clock/clock_ring.png");
    private static final ResourceLocation LONG_HAND_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/entity/doom_clock/clock_hand_long.png");
    private static final ResourceLocation SHORT_HAND_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/entity/doom_clock/clock_hand_short.png");

    private static final RenderType RING_RENDER_TYPE = RenderType.entityTranslucent(RING_TEXTURE);
    private static final RenderType LONG_HAND_RENDER_TYPE = RenderType.entityTranslucent(LONG_HAND_TEXTURE);
    private static final RenderType SHORT_HAND_RENDER_TYPE = RenderType.entityTranslucent(SHORT_HAND_TEXTURE);

    public DoomClockRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DoomClockEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Face the player
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Scale it up
        float scale = 3.0f;
        poseStack.scale(scale, scale, scale);

        int light = LightTexture.FULL_BRIGHT; // Glowing clock!
        int overlay = OverlayTexture.NO_OVERLAY;

        // 1. Draw Ring
        poseStack.pushPose();
        drawQuad(poseStack, buffer.getBuffer(RING_RENDER_TYPE), light, overlay);
        poseStack.popPose();

        // Variables for rotation
        float ticks = entity.getClientTickCount() + partialTicks;
        // The clock ticks from 0 to 240.
        // Let's make the long hand complete 1 full rotation over 12 seconds
        // 1 rotation = 360 degrees. 360 / 240 = 1.5 degrees per tick.
        float longHandAngle = ticks * 1.5f;

        // Let's make the short hand complete 12 full rotations (1 per second)
        // 1 second (20 ticks) = 360 degrees. 360 / 20 = 18 degrees per tick.
        float shortHandAngle = ticks * 18.0f;

        // Note: Clock hands rotate clockwise, which means a negative rotation around
        // the Z axis
        // but since we flipped 180 on Y, positive Z rotation might be clockwise. We
        // assume positive Z is clockwise from looking at it.

        // 2. Draw Short Hand
        poseStack.pushPose();
        poseStack.translate(0, 0, -0.01f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(shortHandAngle));
        // Short hand is 6x18. Pivot at 3.5, 3.5 from bottom-left.
        drawRect(poseStack, buffer.getBuffer(SHORT_HAND_RENDER_TYPE), 6f / 64f, 18f / 64f, 3.5f / 64f, 3.5f / 64f,
                light, overlay);
        poseStack.popPose();

        // 3. Draw Long Hand
        poseStack.pushPose();
        poseStack.translate(0, 0, -0.02f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(longHandAngle));
        // Long hand is 8x24. Pivot at 4.5, 3.5 from bottom-left.
        drawRect(poseStack, buffer.getBuffer(LONG_HAND_RENDER_TYPE), 8f / 64f, 24f / 64f, 4.5f / 64f, 3.5f / 64f, light,
                overlay);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void drawQuad(PoseStack poseStack, VertexConsumer vc, int light, int overlay) {
        drawRect(poseStack, vc, 1.0f, 1.0f, 0.5f, 0.5f, light, overlay);
    }

    private void drawRect(PoseStack poseStack, VertexConsumer vc, float width, float height, float pivotX, float pivotY,
            int light, int overlay) {
        PoseStack.Pose last = poseStack.last();
        Matrix4f poseMat = last.pose();

        float x0 = -pivotX;
        float x1 = width - pivotX;
        float y0 = -pivotY;
        float y1 = height - pivotY;

        // Top left
        drawVertex(poseMat, last, vc, x0, y1, 0.0f, 0.0f, 0.0f, overlay, light);
        // Bottom left
        drawVertex(poseMat, last, vc, x0, y0, 0.0f, 0.0f, 1.0f, overlay, light);
        // Bottom right
        drawVertex(poseMat, last, vc, x1, y0, 0.0f, 1.0f, 1.0f, overlay, light);
        // Top right
        drawVertex(poseMat, last, vc, x1, y1, 0.0f, 1.0f, 0.0f, overlay, light);
    }

    private void drawVertex(Matrix4f poseMat, PoseStack.Pose last, VertexConsumer vc, float x, float y, float z,
            float u, float v, int overlay, int light) {
        vc.addVertex(poseMat, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(last, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(DoomClockEntity entity) {
        return RING_TEXTURE;
    }
}
