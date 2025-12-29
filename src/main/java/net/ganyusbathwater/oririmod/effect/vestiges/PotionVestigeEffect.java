package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class PotionVestigeEffect implements VestigeEffect {

    //damit es nicht jede Sekunde ausgeführt wird und für Effekte wie Nachtsicht nicht "flackert"
    private static final int COOLDOWN_TICKS = 5 * 20;
    private static final int EFFECT_DURATION_TICKS = COOLDOWN_TICKS + 260;

    private final Holder<MobEffect> effect;
    private final int amplifier;

    public PotionVestigeEffect(Holder<MobEffect> effect, int amplifier) {
        this.effect = effect;
        this.amplifier = amplifier;
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null) return;

        Player player = ctx.player();
        if (player == null) return;

        if (ctx.isClient()) return;
        if (effect == null) return;

        // nur alle 5 Sekunden anwenden
        if (player.tickCount % COOLDOWN_TICKS != 0) return;

        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, amplifier, true, false));
    }
}