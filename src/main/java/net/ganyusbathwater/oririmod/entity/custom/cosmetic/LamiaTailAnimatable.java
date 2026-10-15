package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

public class LamiaTailAnimatable extends AbstractPlayerCosmeticAnimatable {

    public static final LamiaTailAnimatable INSTANCE = new LamiaTailAnimatable();

    private LamiaTailAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<LamiaTailAnimatable> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("lamia_walking"));
        } else {
            event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("lamia_walking"));
        }
        return PlayState.CONTINUE;
    }
}
