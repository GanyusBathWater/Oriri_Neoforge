package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.PatientiaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PatientiaModel extends GeoModel<PatientiaEntity> {
    
    private static final ResourceLocation MODEL_LOCATION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/patientia.geo.json");
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/patientia.png");
    private static final ResourceLocation ANIMATION_LOCATION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/patientia.animation.json");

    @Override
    public ResourceLocation getModelResource(PatientiaEntity object) {
        return MODEL_LOCATION;
    }

    @Override
    public ResourceLocation getTextureResource(PatientiaEntity object) {
        return TEXTURE_LOCATION;
    }

    @Override
    public ResourceLocation getAnimationResource(PatientiaEntity animatable) {
        return ANIMATION_LOCATION;
    }

    @Override
    public void setCustomAnimations(PatientiaEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<PatientiaEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        software.bernie.geckolib.cache.object.GeoBone head = getAnimationProcessor().getBone("head");
        float netHeadYaw = 0.0F;
        float headPitch = 0.0F;

        if (head != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);
            if (entityData != null) {
                netHeadYaw = entityData.netHeadYaw();
                headPitch = entityData.headPitch();
                head.setRotX(headPitch * net.minecraft.util.Mth.DEG_TO_RAD);
                head.setRotY(netHeadYaw * net.minecraft.util.Mth.DEG_TO_RAD);
            }
        }

        float partialTick = animationState.getPartialTick();
        net.ganyusbathwater.oririmod.client.physics.HairPhysicsController.applyHairPhysics(
                animatable,
                name -> getAnimationProcessor().getBone(name),
                partialTick,
                netHeadYaw,
                headPitch
        );
    }
}
