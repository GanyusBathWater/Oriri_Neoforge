package net.ganyusbathwater.oririmod.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class CharmedEffect extends MobEffect {

    public CharmedEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF69B4); // Pink als Charm-Farbe
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // jeden Tick prüfen
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        // hier machen wir nichts aktiv – die eigentliche Blockade passiert über Events
        return true;
    }

    @Override
    public void onEffectAdded(LivingEntity living, int amplifier) {
        // Optional: KI stoppen, Ziel zurücksetzen
        if (living instanceof Mob mob) {
            mob.setTarget(null);
        }
    }
}

