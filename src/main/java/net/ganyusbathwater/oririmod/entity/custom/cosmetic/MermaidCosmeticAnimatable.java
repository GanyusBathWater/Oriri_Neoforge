package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class MermaidCosmeticAnimatable extends AbstractPlayerCosmeticAnimatable {
    
    public static final MermaidCosmeticAnimatable INSTANCE = new MermaidCosmeticAnimatable();

    private MermaidCosmeticAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<MermaidCosmeticAnimatable> event) {
        Entity entity = event.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
        if (entity instanceof Player player) {
            boolean inWater = player.isInWater() || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType));
            if (player.isVisuallySwimming() || inWater) {
                if (event.isMoving()) {
                    event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("mermaid_essence_swimming"));
                } else {
                    event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("mermaid_essence_idle"));
                }
                return PlayState.CONTINUE;
            }
        }
        event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("mermaid_essence_idle"));
        return PlayState.CONTINUE;
    }
}
