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
            // Spielerbewegung stoppen
            player.setDeltaMovement(Vec3.ZERO);
            player.hasImpulse = true; // verhindert Gummiband-Effekte
        }

        for (MobEffectInstance instance : livingEntity.getActiveEffects()) {
            // instance.getEffect() liefert ein Holder<MobEffect>; holder.value() ist das tatsächliche MobEffect
            if (instance.getEffect().value() == this) {
                // wenn nur noch <= 1 Tick übrig ist, Cleanup durchführen
                if (instance.getDuration() <= 1) {
                    // Cleanup - AI & Spielerbewegung zurücksetzen
                    if (livingEntity instanceof Mob mob) {
                        mob.setNoAi(false);
                    } else if (livingEntity instanceof Player player) {
                        // Standard-WalkingSpeed in Minecraft ist 0.1f (Server-seitig anpassen)
                        player.getAbilities().setWalkingSpeed(0.1f);
                        player.onUpdateAbilities();
                    }
                }
                break; // gefunden — raus aus der Schleife
            }
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
            player.getAbilities().setWalkingSpeed(-0.1f);
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onMobRemoved(LivingEntity livingEntity, int amplifier, Entity.RemovalReason reason) {
        System.out.println("onMobRemoved");
        if (livingEntity instanceof Mob mob) {
            // AI wieder einschalten
            mob.setNoAi(false);
        } else if (livingEntity instanceof Player player) {
            // Bewegung wieder erlauben
            player.getAbilities().setWalkingSpeed(0.1f);
            player.onUpdateAbilities();
        }
    }
}