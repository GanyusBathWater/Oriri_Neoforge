package net.ganyusbathwater.oririmod.entity.custom.cosmetic;

import net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticAnimatable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class KitsuneCosmeticAnimatable extends AbstractPlayerCosmeticAnimatable {

    public static final KitsuneCosmeticAnimatable INSTANCE = new KitsuneCosmeticAnimatable();

    private final Set<Integer> sneakingPlayers = Collections.synchronizedSet(new HashSet<>());

    private KitsuneCosmeticAnimatable() {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Controller 1: Ears (idle twitch vs sneak fold back)
        controllers.add(new AnimationController<>(this, "ears_controller", 0, state -> {
            Entity entity = state.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
            if (entity instanceof Player player && player.isCrouching()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("kitsune_ears_sneak"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("kitsune_ears_idle"));
            }
            return PlayState.CONTINUE;
        }));

        // Controller 2: Tails Continuous Idle Sway
        controllers.add(new AnimationController<>(this, "tails_idle_controller", 0, state -> {
            state.getController().setAnimation(RawAnimation.begin().thenLoop("kitsune_tails_idle"));
            return PlayState.CONTINUE;
        }));

        // Controller 3: Tails Sneak Combine & Unfold
        controllers.add(new AnimationController<>(this, "tails_sneak_controller", 0, state -> {
            Entity entity = state.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
            if (entity instanceof Player player) {
                int id = player.getId();
                if (player.isCrouching()) {
                    sneakingPlayers.add(id);
                    state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("kitsune_tails_sneak"));
                } else {
                    if (sneakingPlayers.remove(id)) {
                        state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold("kitsune_tails_sneak_unfold"));
                    }
                }
            }
            return PlayState.CONTINUE;
        }));
    }
}
