package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ImmunityEffect implements VestigeEffect {

    // optional: nicht jeden Tick prüfen (Performance); 1s = 20 Ticks
    private static final int COOLDOWN_TICKS = 20;

    private final Set<Holder<MobEffect>> blocked;

    public ImmunityEffect(Set<Holder<MobEffect>> blockedEffects) {
        if (blockedEffects == null || blockedEffects.isEmpty()) {
            this.blocked = Collections.emptySet();
        } else {
            Set<Holder<MobEffect>> tmp = new HashSet<>();
            for (Holder<MobEffect> h : blockedEffects) {
                if (h != null) tmp.add(h);
            }
            this.blocked = Collections.unmodifiableSet(tmp);
        }
    }

    @SafeVarargs
    public ImmunityEffect(Holder<MobEffect>... blockedEffects) {
        Set<Holder<MobEffect>> tmp = new HashSet<>();
        if (blockedEffects != null) {
            for (Holder<MobEffect> h : blockedEffects) {
                if (h != null) tmp.add(h);
            }
        }
        this.blocked = tmp.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(tmp);
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null) return;

        Player player = ctx.player();
        if (player == null) return;

        if (ctx.isClient()) return;
        if (blocked.isEmpty()) return;

        // optionaler Cooldown, damit es nicht pro Tick läuft
        if (player.tickCount % COOLDOWN_TICKS != 0) return;

        // Entfernt die Effekte, falls sie aktiv sind
        for (Holder<MobEffect> holder : blocked) {
            if (holder == null) continue;
            Holder<MobEffect> effect = holder.getDelegate();
            if (effect == null) continue;

            if (player.hasEffect(effect)) {
                player.removeEffect(effect);
            }
        }
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        // optional: direkt beim Equip einmal entfernen
        tick(ctx);
    }
}