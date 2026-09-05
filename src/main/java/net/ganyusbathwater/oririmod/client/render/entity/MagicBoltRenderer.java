package net.ganyusbathwater.oririmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.MagicBoltEntity;
import net.ganyusbathwater.oririmod.util.MagicBoltAbility;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MagicBoltRenderer extends EntityRenderer<MagicBoltEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/entity/magic_projectile.png");
    private final ItemRenderer itemRenderer;

    public MagicBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(MagicBoltEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        MagicBoltAbility ability = entity.getAbility();
        
        switch (ability) {
            case SONIC -> {
                // Invisible
                return;
            }
            case BLAZE, EXPLOSIVE -> {
                renderItem(entity, new ItemStack(Items.FIRE_CHARGE), poseStack, buffer, packedLight);
                return;
            }
            case ENDER -> {
                renderItem(entity, new ItemStack(Items.ENDER_PEARL), poseStack, buffer, packedLight);
                return;
            }
            case NORMAL -> {
                renderCross(entity, partialTicks, poseStack, buffer, packedLight);
                return;
            }
            default -> {
            }
        }
    }

    private void renderItem(MagicBoltEntity entity, ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(0.8F, 0.8F, 0.8F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }

    private void renderCross(MagicBoltEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        // Align with motion
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        // Rotate X to form an X shape instead of +
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(0.1666667F, 0.1666667F, 0.1666667F);
        
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        
        // Manual animation (4 frames, 10 ticks per frame)
        int frame = (entity.tickCount / 10) % 4;
        float vOffset = frame * 0.25F;

        PoseStack.Pose posestack$pose = poseStack.last();
        drawPlane(posestack$pose, vertexconsumer, packedLight, vOffset);

        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        posestack$pose = poseStack.last();
        drawPlane(posestack$pose, vertexconsumer, packedLight, vOffset);

        poseStack.popPose();
    }

    private void drawPlane(PoseStack.Pose pose, VertexConsumer vertexConsumer, int packedLight, float vOffset) {
        float v0 = 0.0F + vOffset;
        float v1 = 0.25F + vOffset;

        // Front face (normal 0, 0, 1)
        vertex(pose, vertexConsumer, -8,  8, 0, 0.0F, v0, 0, 0, 1, packedLight);
        vertex(pose, vertexConsumer, -8, -8, 0, 0.0F, v1, 0, 0, 1, packedLight);
        vertex(pose, vertexConsumer,  8, -8, 0, 1.0F, v1, 0, 0, 1, packedLight);
        vertex(pose, vertexConsumer,  8,  8, 0, 1.0F, v0, 0, 0, 1, packedLight);
        
        // Back face (normal 0, 0, -1)
        vertex(pose, vertexConsumer,  8,  8, 0, 1.0F, v0, 0, 0, -1, packedLight);
        vertex(pose, vertexConsumer,  8, -8, 0, 1.0F, v1, 0, 0, -1, packedLight);
        vertex(pose, vertexConsumer, -8, -8, 0, 0.0F, v1, 0, 0, -1, packedLight);
        vertex(pose, vertexConsumer, -8,  8, 0, 0.0F, v0, 0, 0, -1, packedLight);
    }

    public void vertex(PoseStack.Pose pose, VertexConsumer consumer, int x, int y, int z, float u, float v, int nx, int ny, int nz, int packedLight) {
        consumer.addVertex(pose.pose(), (float)x, (float)y, (float)z)
                .setColor(50, 150, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float)nx, (float)ny, (float)nz);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicBoltEntity entity) {
        return TEXTURE;
    }
}
