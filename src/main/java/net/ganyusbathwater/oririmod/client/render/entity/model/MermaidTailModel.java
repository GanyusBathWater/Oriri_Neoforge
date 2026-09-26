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
        
        // The tail model is now rendered at the root, which fixes the arm fins.
        // However, the 'tail_fin' bone needs to follow the vanilla body so it bends when sneaking.
        software.bernie.geckolib.cache.object.GeoBone tailFin = getAnimationProcessor().getBone("tail_fin");
        if (tailFin != null) {
            net.minecraft.world.entity.Entity entity = animationState.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
            if (entity instanceof net.minecraft.client.player.AbstractClientPlayer player) {
                net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
                if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                    net.minecraft.client.model.PlayerModel<?> playerModel = playerRenderer.getModel();
                    tailFin.setRotX(-playerModel.body.xRot);
                    tailFin.setRotY(-playerModel.body.yRot);
                    tailFin.setRotZ(-playerModel.body.zRot);
                    
                    tailFin.setPosX(playerModel.body.x);
                    tailFin.setPosY(-playerModel.body.y);
                    tailFin.setPosZ(playerModel.body.z);
                }
            }
        }
    }
}
