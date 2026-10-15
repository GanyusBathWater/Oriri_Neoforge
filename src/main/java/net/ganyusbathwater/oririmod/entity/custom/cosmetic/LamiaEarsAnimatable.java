package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

public class LamiaEarsAnimatable extends AbstractPlayerCosmeticAnimatable {

    public static final LamiaEarsAnimatable INSTANCE = new LamiaEarsAnimatable();

    private LamiaEarsAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<LamiaEarsAnimatable> event) {
        event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("lamia_idle_ears"));
        return PlayState.CONTINUE;
    }
}
