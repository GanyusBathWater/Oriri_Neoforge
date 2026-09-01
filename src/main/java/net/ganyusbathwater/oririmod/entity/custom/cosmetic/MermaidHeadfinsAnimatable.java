package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

public class MermaidHeadfinsAnimatable extends AbstractPlayerCosmeticAnimatable {
    
    public static final MermaidHeadfinsAnimatable INSTANCE = new MermaidHeadfinsAnimatable();

    private MermaidHeadfinsAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<MermaidHeadfinsAnimatable> event) {
        event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("mermaid_essence_idle_headfins"));
        return PlayState.CONTINUE;
    }
}
