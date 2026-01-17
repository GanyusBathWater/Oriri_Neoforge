package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.ElementalDamageHandler;
import net.ganyusbathwater.oririmod.events.vestiges.VestigeDayNightEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class BoundOfTheCelestialSistersEffect implements VestigeEffect {

    private static final int COOLDOWN_TICKS = 20;

    private static final String NBT_KEY = "Oriri_BoundCelestialSisters_Element";
    private static final String NBT_LAST_STATUS = "LastStatus";
    private static final String NBT_APPLIED = "Applied";

    private static final float RESIST_PERCENT = 0.25f;

    public static VestigeEffect elementalResistance() {
        return new BoundOfTheCelestialSistersEffect();
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
            if (lastStatusId == 1) {
                removeDay(player);
            } else if (lastStatusId == 2) {
                removeNight(player);
            }
        }

        if (newStatusId == 1) {
            applyDay(player);
        } else {
            applyNight(player);
        }

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
        if (lastStatusId == 1) {
            removeDay(player);
        } else if (lastStatusId == 2) {
            removeNight(player);
        }

        root.remove(NBT_KEY);
    }

    private static void applyDay(Player player) {
        ElementalDamageHandler.setPlayerResistance(player, Element.DARKNESS, RESIST_PERCENT);
        ElementalDamageHandler.setPlayerResistance(player, Element.LIGHT, 0.0f);
    }

    private static void removeDay(Player player) {
        ElementalDamageHandler.setPlayerResistance(player, Element.DARKNESS, 0.0f);
    }

    private static void applyNight(Player player) {
        ElementalDamageHandler.setPlayerResistance(player, Element.LIGHT, RESIST_PERCENT);
        ElementalDamageHandler.setPlayerResistance(player, Element.DARKNESS, 0.0f);
    }

    private static void removeNight(Player player) {
        ElementalDamageHandler.setPlayerResistance(player, Element.LIGHT, 0.0f);
    }
}