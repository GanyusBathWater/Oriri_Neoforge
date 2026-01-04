package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.RelicOfThePastEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = "oririmod")
public final class RelicOfThePastProcEvents {

    private RelicOfThePastProcEvents() {}

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        if (!RelicOfThePastEffect.isEnabled(player)) return;

        float dealt = event.getNewDamage();
        if (dealt <= 0.0f) return;

        if (RelicOfThePastEffect.getActiveCooldownSecondsRaw(player) > 0) return;

        int level = getRelicLevel(player);
        if (level <= 0) return;

        applyEffectsForLevel(player, level);

        RelicOfThePastEffect.setNewProcCooldown(player);
    }

    private static int getRelicLevel(Player player) {
        if (player == null) return 0;

        final int maxLevel = 3;
        int level = 0;

        // defensiv: Level wird bereits in RelicOfThePastEffect gepflegt, hier nur best effort
        // (wenn du den Level exakt brauchst, solltest du ihn dort aus dem Stack ableiten \- wie beim Mirror)
        if (RelicOfThePastEffect.isEnabled(player)) {
            level = maxLevel;
        }

        return Math.max(0, Math.min(maxLevel, level));
    }

    private static void applyEffectsForLevel(Player player, int level) {
        if (player == null) return;

        int dur = RelicOfThePastEffect.EFFECT_DURATION_TICKS;

        if (level >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, 0, true, false));
        }
        if (level >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 0, true, false));
        }
        if (level >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, 0, true, false));
        }
    }
}