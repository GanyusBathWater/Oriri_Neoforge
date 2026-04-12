package net.ganyusbathwater.oririmod.entity.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Provides the three GeckoLib resource locations for the Eye of Desolation:
 *  - Model  → assets/oririmod/geo/entity/eye_of_desolation.geo.json
 *  - Texture → assets/oririmod/textures/entity/eye_of_desolation.png
 *  - Anim   → assets/oririmod/animations/entity/eye_of_desolation.animation.json
 */
public class EyeOfDesolationModel extends GeoModel<EyeOfDesolationEntity> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/eye_of_desolation.geo.json");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/eye_of_desolation.png");

    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/eye_of_desolation.animation.json");

    @Override
    public ResourceLocation getModelResource(EyeOfDesolationEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(EyeOfDesolationEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(EyeOfDesolationEntity animatable) {
        return ANIMATIONS;
    }

    /**
     * Implement head-tracking so the Eye of Desolation physically looks at players.
     */
    @Override
    public void setCustomAnimations(EyeOfDesolationEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<EyeOfDesolationEntity> animationState) {
        software.bernie.geckolib.cache.object.GeoBone head = getAnimationProcessor().getBone("eye_of_desolation");

        if (head != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            
            // Apply yaw and pitch to the main root bone
            // Note: GeckoLib rotations are in radians.
            head.setRotX(entityData.headPitch() * ((float) Math.PI / 180f));
            head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180f));
        }
    }
}
