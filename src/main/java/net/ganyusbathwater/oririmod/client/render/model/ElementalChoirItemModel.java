package net.ganyusbathwater.oririmod.client.render.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.physics.ElementalChoirAnimationController;
import net.ganyusbathwater.oririmod.client.render.item.ElementalChoirItemRenderer;
import net.ganyusbathwater.oririmod.item.custom.ElementalChoirItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class ElementalChoirItemModel extends DefaultedItemGeoModel<ElementalChoirItem> {
    public ElementalChoirItemModel() {
        super(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elemental_choir"));
    }

    @Override
    protected String subtype() {
        return "item";
    }

    @Override
    public void setCustomAnimations(ElementalChoirItem animatable, long instanceId, AnimationState<ElementalChoirItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Player player = ElementalChoirItemRenderer.currentRenderEntity;
        ElementalChoirAnimationController.applyAnimation(
                player,
                animatable,
                name -> getAnimationProcessor().getBone(name),
                animationState.getPartialTick()
        );
    }
}
