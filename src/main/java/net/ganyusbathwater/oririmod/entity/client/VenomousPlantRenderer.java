package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.ganyusbathwater.oririmod.entity.custom.VenomousPlantEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib entity renderer for the Venomous Plant.
 *
 * Key override — {@link #getDeathMaxRotation} returns 0 so Minecraft's vanilla
 * death-rotation transform is suppressed entirely.  The plant dies by playing
 * only the custom GeckoLib "plant_turret_death" animation without any tipping
 * or falling-over effect on top.
 */
public class VenomousPlantRenderer extends GeoEntityRenderer<VenomousPlantEntity> {

    public VenomousPlantRenderer(EntityRendererProvider.Context context) {
        super(context, new VenomousPlantModel());
    }

    /**
     * Return 0 to completely disable the vanilla "entity tips over when it dies"
     * rotation transform — we only want the GeckoLib death animation to play.
     */
    @Override
    protected float getDeathMaxRotation(VenomousPlantEntity animatable) {
        return 0f;
    }

    /**
     * Prevent the split-second pop-up by hiding the entity on its very first
     * render ticks before the spawning animation fully applies its position offset.
     */
    @Override
    public void render(VenomousPlantEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.tickCount < 2) {
            poseStack.pushPose();
            poseStack.scale(0, 0, 0);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        } else {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }
}
