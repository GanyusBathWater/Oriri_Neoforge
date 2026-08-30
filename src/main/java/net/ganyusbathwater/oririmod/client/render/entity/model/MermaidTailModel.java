package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.minecraft.resources.ResourceLocation;

public class MermaidTailModel extends AbstractPlayerCosmeticModel<MermaidCosmeticAnimatable> {

    @Override
    public ResourceLocation getModelResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid_essence.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid_fin_template.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_essence.animation.json");
    }

    @Override
    public void setCustomAnimations(MermaidCosmeticAnimatable animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<MermaidCosmeticAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        
        // The tail model is physically glued to the Vanilla body bone in the RenderLayer.
        // It inherits all Vanilla body rotations and movements directly from the PoseStack!
        // We MUST reset the 'body' and 'torso' bones to 0, otherwise they would be double-rotated
        // (once by the PoseStack, and once by AbstractPlayerCosmeticModel), causing perpendicular drifting!
        software.bernie.geckolib.cache.object.GeoBone torso = getAnimationProcessor().getBone("torso");
        software.bernie.geckolib.cache.object.GeoBone body = getAnimationProcessor().getBone("body");
        if (torso != null) {
            torso.setRotX(0);
            torso.setRotY(0);
            torso.setRotZ(0);
        }
        if (body != null) {
            body.setRotX(0);
            body.setRotY(0);
            body.setRotZ(0);
        }
    }
}
