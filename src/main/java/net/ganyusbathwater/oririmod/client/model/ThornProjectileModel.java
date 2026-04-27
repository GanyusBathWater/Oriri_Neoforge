package net.ganyusbathwater.oririmod.client.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.ThornProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Thorn Projectile.
 * Points GeckoLib at:
 *  - geo/entity/thorn_projectile.geo.json
 *  - textures/entity/thorn_projectile.png
 *  - animations/entity/thorn_projectile.animation.json  (placeholder — no real animations)
 */
public class ThornProjectileModel extends GeoModel<ThornProjectileEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/thorn_projectile.geo.json");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/thorn_projectile.png");

    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/thorn_projectile.animation.json");

    @Override
    public ResourceLocation getModelResource(ThornProjectileEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ThornProjectileEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ThornProjectileEntity animatable) {
        return ANIMATIONS;
    }
}
