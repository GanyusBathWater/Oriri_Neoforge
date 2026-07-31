package net.ganyusbathwater.oririmod.client.render;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.NoxusHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NoxusHelmetModel extends GeoModel<NoxusHelmetItem> {
    @Override
    public ResourceLocation getModelResource(NoxusHelmetItem object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/armor/noxus_helmet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NoxusHelmetItem object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/models/noxus_helmet/" + object.getVariant() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(NoxusHelmetItem animatable) {
        // Return null or a valid empty animation file if not animated
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/armor_animation.json"); // Provide fallback or dummy
    }
}
