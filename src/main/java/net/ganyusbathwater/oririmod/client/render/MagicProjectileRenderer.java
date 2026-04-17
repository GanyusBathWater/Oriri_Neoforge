package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.entity.MagicProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public class MagicProjectileRenderer extends EntityRenderer<MagicProjectileEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/entity/magic_projectile.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    private static final Map<Element, Color> ELEMENT_COLORS = new EnumMap<>(Element.class);

    static {
        ELEMENT_COLORS.put(Element.FIRE, new Color(255, 60, 60));
        ELEMENT_COLORS.put(Element.NATURE, new Color(60, 255, 60));
        ELEMENT_COLORS.put(Element.EARTH, new Color(180, 120, 60));
        ELEMENT_COLORS.put(Element.WATER, new Color(60, 120, 255));
        ELEMENT_COLORS.put(Element.LIGHT, new Color(255, 255, 180));
        ELEMENT_COLORS.put(Element.DARKNESS, new Color(160, 60, 255));
        ELEMENT_COLORS.put(Element.TRUE_DAMAGE, new Color(255, 60, 220));
        ELEMENT_COLORS.put(Element.PHYSICAL, Color.WHITE);
    }

    public MagicProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MagicProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float scale = 1.5f; // Increased scale
        poseStack.scale(scale, scale, scale);

        // Align rendering to velocity vector
        net.minecraft.world.phys.Vec3 velocity = entity.getDeltaMovement();
        double horizontalDistanceSqr = velocity.x * velocity.x + velocity.z * velocity.z;
        float yaw = (float) (Mth.atan2(velocity.x, velocity.z) * (double) (180F / (float) Math.PI));
        float pitch = (float) (Mth.atan2(velocity.y, Math.sqrt(horizontalDistanceSqr))
                * (double) (180F / (float) Math.PI));

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Rotate 45 degrees around Z so the planes form an 'X' shape relative to the travel direction
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));

        VertexConsumer vc = buffer.getBuffer(RENDER_TYPE);
        int overlay = OverlayTexture.NO_OVERLAY;
        int light = 15728880; // Full bright for magic projectile

        // Calculate animation frame (4 frames, 0.5s each = 10 ticks per frame)
        int frame = (entity.tickCount / 10) % 4;
        float v0 = frame * 0.25F;
        float v1 = (frame + 1) * 0.25F;

        float hLength = 0.5F;
        float hWidth = 0.5F;

        Color color = ELEMENT_COLORS.getOrDefault(entity.getElement(), Color.WHITE);

        // Draw cross plane 1
        PoseStack.Pose last = poseStack.last();
        Matrix4f poseMat = last.pose();

        drawVertex(poseMat, last, vc, -hWidth, 0.0F, -hLength, 1.0F, v1, overlay, light, color);
        drawVertex(poseMat, last, vc, hWidth, 0.0F, -hLength, 0.0F, v1, overlay, light, color);
        drawVertex(poseMat, last, vc, hWidth, 0.0F, hLength, 0.0F, v0, overlay, light, color);
        drawVertex(poseMat, last, vc, -hWidth, 0.0F, hLength, 1.0F, v0, overlay, light, color);

        // Draw cross plane 2
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        last = poseStack.last();
        poseMat = last.pose();

        drawVertex(poseMat, last, vc, -hWidth, 0.0F, -hLength, 1.0F, v1, overlay, light, color);
        drawVertex(poseMat, last, vc, hWidth, 0.0F, -hLength, 0.0F, v1, overlay, light, color);
        drawVertex(poseMat, last, vc, hWidth, 0.0F, hLength, 0.0F, v0, overlay, light, color);
        drawVertex(poseMat, last, vc, -hWidth, 0.0F, hLength, 1.0F, v0, overlay, light, color);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void drawVertex(Matrix4f poseMat, PoseStack.Pose last, VertexConsumer vc,
                            float x, float y, float z, float u, float v, int overlay, int light, Color color) {
        vc.addVertex(poseMat, x, y, z)
                .setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(last, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicProjectileEntity entity) {
        return TEXTURE;
    }
}
