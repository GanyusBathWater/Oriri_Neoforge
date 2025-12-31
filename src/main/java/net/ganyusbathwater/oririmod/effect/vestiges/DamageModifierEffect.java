// file: `src/main/java/net/ganyusbathwater/oririmod/effect/vestiges/DamageModifierEffect.java`
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.combat.ElementalDamageHandler;
import net.ganyusbathwater.oririmod.events.vestiges.VestigeDayNightEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;


public final class DamageModifierEffect implements VestigeEffect {

    private static final int COOLDOWN_TICKS = 20;

    private static final String NBT_KEY = "Oriri_BoundCelestialSisters_Combat";
    private static final String NBT_LAST_STATUS = "LastStatus";
    private static final String NBT_APPLIED = "Applied";

    public static VestigeEffect boundCelestialSistersCombat() {
        return new DamageModifierEffect();
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        if (player.tickCount % COOLDOWN_TICKS != 0) return;

        VestigeDayNightEvents.DayNightStatus status = VestigeDayNightEvents.getStatus(ctx.level());
        if (status == VestigeDayNightEvents.DayNightStatus.UNKNOWN) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);

        int newStatusId = status == VestigeDayNightEvents.DayNightStatus.DAY ? 1 : 2;
        int lastStatusId = tag.getInt(NBT_LAST_STATUS);
        boolean applied = tag.getBoolean(NBT_APPLIED);

        if (applied && lastStatusId == newStatusId) return;

        if (applied) {
            if (lastStatusId == 1) removeDay(player);
            else if (lastStatusId == 2) removeNight(player);
        }

        if (newStatusId == 1) applyDay(player);
        else applyNight(player);

        tag.putInt(NBT_LAST_STATUS, newStatusId);
        tag.putBoolean(NBT_APPLIED, true);
        root.put(NBT_KEY, tag);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        CompoundTag root = player.getPersistentData();
        if (!root.contains(NBT_KEY)) return;

        CompoundTag tag = root.getCompound(NBT_KEY);
        if (!tag.getBoolean(NBT_APPLIED)) {
            root.remove(NBT_KEY);
            return;
        }

        int lastStatusId = tag.getInt(NBT_LAST_STATUS);
        if (lastStatusId == 1) removeDay(player);
        else if (lastStatusId == 2) removeNight(player);

        root.remove(NBT_KEY);
    }

    //---------------------------Methoden für BoundOfTheCelestialSisters-----------------------------------------

    private static void applyDay(Player player) {
        // Tag: +15% Defense => eingehender Schaden * 0.85
        ElementalDamageHandler.setPlayerIncomingMultiplier(player, 0.85f);
        ElementalDamageHandler.setPlayerOutgoingMultiplier(player, 1.0f);
    }

    private static void removeDay(Player player) {
        ElementalDamageHandler.setPlayerIncomingMultiplier(player, 1.0f);
    }

    private static void applyNight(Player player) {
        // Nacht: +15% Damage => ausgehender Schaden * 1.15
        ElementalDamageHandler.setPlayerOutgoingMultiplier(player, 1.15f);
        ElementalDamageHandler.setPlayerIncomingMultiplier(player, 1.0f);
    }

    private static void removeNight(Player player) {
        ElementalDamageHandler.setPlayerOutgoingMultiplier(player, 1.0f);
    }
}