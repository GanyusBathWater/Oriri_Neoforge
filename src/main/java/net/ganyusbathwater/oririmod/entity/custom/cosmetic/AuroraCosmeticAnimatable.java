package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

public class AuroraCosmeticAnimatable extends AbstractPlayerCosmeticAnimatable {
    
    public static final AuroraCosmeticAnimatable INSTANCE = new AuroraCosmeticAnimatable();

    private AuroraCosmeticAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base_controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<AuroraCosmeticAnimatable> event) {
        net.minecraft.world.entity.Entity entity = event.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isFallFlying()) {
                event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenPlayAndHold("animation.wings.glide"));
                return PlayState.CONTINUE;
            } else if (player.getAbilities().flying) {
                event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.player.fly"));
                return PlayState.CONTINUE;
            }
        }
        event.getController().setAnimation(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.player.idle"));
        return PlayState.CONTINUE;
    }
}
