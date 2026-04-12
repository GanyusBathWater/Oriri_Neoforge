package net.ganyusbathwater.oririmod.entity.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity;

/**
 * GeckoLib entity renderer for the Eye of Desolation.
 * Registers the model and adds the guardian-style beam render layer.
 */
public class EyeOfDesolationRenderer extends GeoEntityRenderer<EyeOfDesolationEntity> {

    public EyeOfDesolationRenderer(EntityRendererProvider.Context context) {
        super(context, new EyeOfDesolationModel());
        // Attach the beam layer — it renders the laser from entity centre to target
        this.addRenderLayer(new EyeOfDesolationBeamLayer(this));
    }

    /** Flying entity — no ground shadow. */
    @Override
    protected float getDeathMaxRotation(EyeOfDesolationEntity animatable) {
        return 0f;
    }
}
