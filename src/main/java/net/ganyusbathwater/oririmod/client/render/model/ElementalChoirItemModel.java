package net.ganyusbathwater.oririmod.client.render.model;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.ElementalChoirItem;
import net.minecraft.resources.ResourceLocation;
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
    public void setCustomAnimations(ElementalChoirItem animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<ElementalChoirItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        
        net.minecraft.world.entity.player.Player player = net.ganyusbathwater.oririmod.client.render.item.ElementalChoirItemRenderer.currentRenderEntity;
        if (player != null) {
            long currentTick = player.level().getGameTime();
            long lastSwing = net.ganyusbathwater.oririmod.item.custom.ElementalChoirItem.CLIENT_LAST_SWING_TICK.getOrDefault(player, 0L);
            
            // Only snap the carousel to face the player's crosshair during a left-click swing
            if (currentTick - lastSwing < 10) {
                software.bernie.geckolib.cache.object.GeoBone weaponsBone = getAnimationProcessor().getBone("weapons");
                if (weaponsBone != null) {
                    float partialTick = animationState.getPartialTick();
                    float headRot = net.minecraft.util.Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot());
                    float targetYawRad = (float) Math.toRadians(180.0f - headRot);
                    
                    weaponsBone.setRotY(targetYawRad);
                }
            }
        }
    }
}
