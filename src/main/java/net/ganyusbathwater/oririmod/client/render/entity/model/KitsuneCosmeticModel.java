package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.ganyusbathwater.oririmod.client.render.layer.CosmeticVisibilityHelper;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.KitsuneCosmeticAnimatable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;

public class KitsuneCosmeticModel extends AbstractPlayerCosmeticModel<KitsuneCosmeticAnimatable> {

    @Override
    public ResourceLocation getModelResource(KitsuneCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/kitsune_cosmetic.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KitsuneCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/kitsune_cosmetic_primary.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KitsuneCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/kitsune_cosmetic.animation.json");
    }

    @Override
    public void setCustomAnimations(KitsuneCosmeticAnimatable animatable, long instanceId, AnimationState<KitsuneCosmeticAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Entity entity = animationState.getData(DataTickets.ENTITY);
        boolean hideEars = (entity instanceof Player player) && CosmeticVisibilityHelper.shouldHideCosmeticHead(player);

        GeoBone leftEar = getAnimationProcessor().getBone("left_ear");
        if (leftEar != null) {
            leftEar.setHidden(hideEars);
        }
        GeoBone rightEar = getAnimationProcessor().getBone("right_ear");
        if (rightEar != null) {
            rightEar.setHidden(hideEars);
        }
    }
}
