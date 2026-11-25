package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.physics.LizardTailPhysicsController;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.LizardTailCosmeticAnimatable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public class LizardTailCosmeticModel extends AbstractPlayerCosmeticModel<LizardTailCosmeticAnimatable> {

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/lizard_tail_cosmetic.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/lizard_tail_primary.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/lizard_tail_cosmetic.animation.json");

    @Override
    public ResourceLocation getModelResource(LizardTailCosmeticAnimatable object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LizardTailCosmeticAnimatable object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(LizardTailCosmeticAnimatable object) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(LizardTailCosmeticAnimatable animatable, long instanceId,
                                    AnimationState<LizardTailCosmeticAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Entity entity = animationState.getData(DataTickets.ENTITY);
        if (entity instanceof LivingEntity livingEntity) {
            LizardTailPhysicsController.applyTailAnimation(
                    livingEntity,
                    name -> getAnimationProcessor().getBone(name),
                    animationState.getPartialTick()
            );
        }
    }
}
