package net.ganyusbathwater.oririmod.effect;


import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
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
            // Spielerbewegung stoppen
            player.setDeltaMovement(Vec3.ZERO);
            player.hasImpulse = true; // verhindert Gummiband-Effekte
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Effekt soll JEDE TICK angewendet werden
        return true;
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        if (livingEntity instanceof Mob mob) {
            // AI ausschalten
            mob.setNoAi(true);
        } else if (livingEntity instanceof Player player) {
            // Bewegung komplett unterbinden
            player.getAbilities().setWalkingSpeed(0);
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onMobRemoved(LivingEntity livingEntity, int amplifier, Entity.RemovalReason reason) {
        if (livingEntity instanceof Mob mob) {
            // AI wieder einschalten
            mob.setNoAi(false);
        } else if (livingEntity instanceof Player player) {
            // Bewegung wieder erlauben
            player.getAbilities().setWalkingSpeed(0);
            player.onUpdateAbilities();
        }
    }
}