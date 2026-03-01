package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.SwordProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public class SwordProjectileRenderer extends EntityRenderer<SwordProjectileEntity> {
    private final ItemRenderer itemRenderer;

    public SwordProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SwordProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float currentYaw;
        float currentPitch;

        if (entity.isDeadState()) {
            currentYaw = entity.getYRot();
            currentPitch = entity.getXRot();
        } else {
            currentYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            currentPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        }

        // Force pointing downwards initially before locking since we can't reliably
        // override
        // Entity rotation variables on initialization in 1.21 Mojmap
        if (entity.tickCount < 40) {
            currentPitch = -90.0F;
            currentYaw = 0.0F;
        }

        float roll = entity.getRandomRoll();

        // 1. If it's dead, snap it EXACTLY to its pitch. If hovering, it's straight
        // down (-90).
        // Standard projectile rotation logic
        poseStack.mulPose(Axis.YP.rotationDegrees(currentYaw - 90.0F));

        // Pitch is natively correct for projectiles in Minecraft, do not invert
        poseStack.mulPose(Axis.ZP.rotationDegrees(currentPitch));

        float scale = 1.0F; // Vanilla Sword Sized
        poseStack.scale(scale, scale, scale);

        // Add random roll around the flight axis
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));

        // Point the texture's tip towards +X (forward)
        // With ItemDisplayContext.NONE, the texture renders flat on the XY plane.
        // We're spinning -45 on Z to point the top-right corner (the blade tip)
        // directly at the +X axis (forward vector).
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));

        // Center the pivot
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordProjectileEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
