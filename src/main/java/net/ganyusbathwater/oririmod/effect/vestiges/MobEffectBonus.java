package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

public class MobEffectBonus implements VestigeEffect {
    private final Holder<MobEffect> effect;
    private final int amplifier;
    private final int durationTicks;

    public MobEffectBonus(Holder<MobEffect> effect, int amplifier, int durationTicks) {
        this.effect = effect;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
    }

    @Override
    public void tick(ServerPlayer player, ItemStack stack, int level) {
        var existing = player.getEffect(effect);
        if (existing == null || existing.getAmplifier() < amplifier || existing.getDuration() <= durationTicks / 2) {
            player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, true, false, true));
        }
    }
}