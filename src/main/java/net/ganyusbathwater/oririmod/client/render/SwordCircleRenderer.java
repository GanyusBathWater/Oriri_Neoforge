package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ganyusbathwater.oririmod.entity.SwordCircleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SwordCircleRenderer extends EntityRenderer<SwordCircleEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack swordStack;

    public SwordCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.swordStack = new ItemStack(Items.NETHERITE_SWORD);
    }

    @Override
    public void render(SwordCircleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        int numSwords = 8;
        
        float radius = 3.0f;
        float rotationSpeed = 10.0f; // degrees per tick
        
        // Calculate dynamic radius and rotation based on phase
        float currentRotation = age * rotationSpeed;
        
        if (age > 40.0f) {
            // The Tell Phase: Expand radius and slow down rotation
            float progress = Math.min((age - 40.0f) / 10.0f, 1.0f); // 0.0 to 1.0 over 10 ticks
            
            // Easing function for smooth expansion (Out Cubic)
            float eased = 1.0f - (float)Math.pow(1.0f - progress, 3);
            
            radius = 3.0f + (eased * 1.0f); // Expands from 3.0 to 4.0
            
            // Lock rotation smoothly
            currentRotation = 40.0f * rotationSpeed + (eased * (rotationSpeed * 2.0f)); 
        }

        for (int i = 0; i < numSwords; i++) {
            poseStack.pushPose();
            
            // Base offset angle for this sword
            float angleDeg = currentRotation + (i * (360.0f / numSwords));
            float angleRad = angleDeg * ((float)Math.PI / 180F);
            
            // Position relative to center
            float xOffset = radius * (float)Math.cos(angleRad);
            float zOffset = radius * (float)Math.sin(angleRad);
            
            poseStack.translate(xOffset, 0, zOffset);
            
            // Match original SwordProjectileEntity hover state exactly: Pointing perfectly straight down.
            float pitch = -90.0F; 
            float yaw = 0.0F;
            float roll = i * 45.0f; // Give them a nice evenly distributed roll

            // Apply standard projectile rotations
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

            // Add roll around the flight axis
            poseStack.mulPose(Axis.XP.rotationDegrees(roll));
            
            // Item renderer specific adjustments (pointing tip forward)
            poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
            
            // Center pivot
            poseStack.translate(-0.5F, -0.5F, -0.5F);

            this.itemRenderer.renderStatic(
                    this.swordStack,
                    ItemDisplayContext.NONE,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    entity.level(),
                    entity.getId() + i // Pseudo-unique ID for rendering
            );
            
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordCircleEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
