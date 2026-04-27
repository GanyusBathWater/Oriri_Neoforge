package net.ganyusbathwater.oririmod.client.render;

import net.ganyusbathwater.oririmod.client.model.ThornProjectileModel;
import net.ganyusbathwater.oririmod.entity.ThornProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib entity renderer for the Thorn Projectile.
 * Renders the 3-D model defined in ThornProjectileModel.
 */
public class ThornProjectileRenderer extends GeoEntityRenderer<ThornProjectileEntity> {

    public ThornProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, new ThornProjectileModel());
    }
}
