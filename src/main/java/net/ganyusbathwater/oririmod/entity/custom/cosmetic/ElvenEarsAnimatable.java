package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class ElvenEarsAnimatable extends AbstractPlayerCosmeticAnimatable {

    public static final ElvenEarsAnimatable INSTANCE = new ElvenEarsAnimatable();

    private ElvenEarsAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<ElvenEarsAnimatable> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("elven_idle_ears"));
        return PlayState.CONTINUE;
    }
}
