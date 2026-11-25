package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;

public class LizardTailCosmeticAnimatable extends AbstractPlayerCosmeticAnimatable {

    public static final LizardTailCosmeticAnimatable INSTANCE = new LizardTailCosmeticAnimatable();

    private LizardTailCosmeticAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Driven procedurally by LizardTailPhysicsController in LizardTailCosmeticModel#setCustomAnimations.
    }
}
