package net.ganyusbathwater.oririmod.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib entity renderer for Deviartras.
 *
 * Key overrides:
 *  - {@link #getDeathMaxRotation} returns 0 to suppress vanilla "tip-over"
 *    rotation. All death animation is handled exclusively by the GeckoLib
 *    deviartras_defeat / deviartras_defeat_idle sequence.
 *  - On the first two ticks, the entity is scaled to 0 to hide the
 *    one-frame "pop-in" before the spawning pose is established.
 */
public class DeviartrasRenderer extends GeoEntityRenderer<DeviartrasEntity> {

    public DeviartrasRenderer(EntityRendererProvider.Context context) {
        super(context, new DeviartrasModel());
    }

    /**
     * Return 0 so vanilla never rotates Deviartras sideways on death.
     * The GeckoLib defeat animation handles the visual death pose entirely.
     */
    @Override
    protected float getDeathMaxRotation(DeviartrasEntity animatable) {
        return 0f;
    }

    /**
     * Suppress the first two render ticks to avoid a one-frame position-pop
     * before the model settles into its initial pose.
     */
    @Override
    public void render(DeviartrasEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.tickCount < 2) {
            poseStack.pushPose();
            poseStack.scale(0f, 0f, 0f);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        } else {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }
}
