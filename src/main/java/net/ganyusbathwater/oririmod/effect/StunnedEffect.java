package net.ganyusbathwater.oririmod.effect;


import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class StunnedEffect extends MobEffect {

    public StunnedEffect(MobEffectCategory category, int color) {
        super(MobEffectCategory.HARMFUL, 0x00000);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof Player player) {
            // Stop player movement
            player.setDeltaMovement(Vec3.ZERO);
            player.hasImpulse = true; // prevents rubber band effects
        }

        for (MobEffectInstance instance : livingEntity.getActiveEffects()) {
            if (livingEntity instanceof Mob mob) {
                mob.setNoAi(true);
            } else if (livingEntity instanceof Player player) {
                player.getAbilities().setWalkingSpeed(-0.1f);
                player.onUpdateAbilities();
            }

            // instance.getEffect() returns a Holder<MobEffect>; holder.value() is the actual MobEffect
            if (instance.getEffect().value() == this) {
                // if only <= 1 tick remains, perform cleanup
                if (instance.getDuration() <= 1) {
                    // Cleanup - AI & Reset player movement
                    if (livingEntity instanceof Mob mob) {
                        mob.setNoAi(false);
                    } else if (livingEntity instanceof Player player) {
                        // Default WalkingSpeed in Minecraft is 0.1f (adjust server side)
                        player.getAbilities().setWalkingSpeed(0.1f);
                        player.onUpdateAbilities();
                    }
                }
                break;
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // EVERY TICK should be applied to the effect
        return true;
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
    }

    @Override
    public void onMobRemoved(LivingEntity livingEntity, int amplifier, Entity.RemovalReason reason) {
        if (livingEntity instanceof Mob mob) {
            // AI turn back on
            mob.setNoAi(false);
        } else if (livingEntity instanceof Player player) {
            // Allow movement again
            player.getAbilities().setWalkingSpeed(0.1f);
            player.onUpdateAbilities();
        }
    }
}