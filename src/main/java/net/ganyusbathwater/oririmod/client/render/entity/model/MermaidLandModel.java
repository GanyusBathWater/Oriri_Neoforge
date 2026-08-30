package net.ganyusbathwater.oririmod.client.render.entity.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable;
import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel;
import net.minecraft.resources.ResourceLocation;

public class MermaidLandModel extends AbstractPlayerCosmeticModel<MermaidCosmeticAnimatable> {

    @Override
    public void setCustomAnimations(MermaidCosmeticAnimatable animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<MermaidCosmeticAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        net.minecraft.world.entity.Entity entity = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
        if (entity instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                net.minecraft.client.model.PlayerModel<?> playerModel = playerRenderer.getModel();
                software.bernie.geckolib.cache.object.GeoBone leftLeg = getAnimationProcessor().getBone("left_leg");
                if (leftLeg != null) {
                    leftLeg.setRotX(playerModel.leftLeg.xRot);
                    leftLeg.setRotY(playerModel.leftLeg.yRot);
                    leftLeg.setRotZ(playerModel.leftLeg.zRot);
                }
                software.bernie.geckolib.cache.object.GeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
                if (rightLeg != null) {
                    rightLeg.setRotX(playerModel.rightLeg.xRot);
                    rightLeg.setRotY(playerModel.rightLeg.yRot);
                    rightLeg.setRotZ(playerModel.rightLeg.zRot);
                }
            }
        }
    }

    @Override
    public ResourceLocation getModelResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "geo/entity/mermaid_essence_land.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/mermaid_fin_template.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MermaidCosmeticAnimatable object) {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "animations/entity/mermaid_essence.animation.json");
    }
}
