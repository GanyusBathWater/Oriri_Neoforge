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
}
