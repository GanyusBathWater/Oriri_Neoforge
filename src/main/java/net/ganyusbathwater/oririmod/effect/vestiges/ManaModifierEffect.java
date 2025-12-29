package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.events.vestiges.VestigeDayNightEvents;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class ManaModifierEffect implements VestigeEffect {

    private static final int COOLDOWN_TICKS = 20;

    private static final String NBT_KEY = "Oriri_BoundCelestialSisters_Mana";
    private static final String NBT_LAST_STATUS = "LastStatus";
    private static final String NBT_APPLIED = "Applied";

    private static final String NBT_APPLIED_MAX_MANA_BONUS = "AppliedMaxManaBonus";
    private static final String NBT_DAY_APPLIED = "DayApplied";

    private static final int DAY_BONUS_MAX_MANA = 50;
    private static final int MIN_REGEN_INTERVAL_SECONDS = 1;

    private final int bonusMaxMana;
    private final Integer regenIntervalSeconds;

    public ManaModifierEffect(int bonusMaxMana, Integer regenIntervalSeconds) {
        this.bonusMaxMana = bonusMaxMana;
        this.regenIntervalSeconds = regenIntervalSeconds;
    }

    public static ManaModifierEffect bonusMaxMana(int bonusMaxMana) {
        return new ManaModifierEffect(bonusMaxMana, null);
    }

    public static ManaModifierEffect regenIntervalSeconds(int seconds) {
        return new ManaModifierEffect(0, seconds);
    }

    public static ManaModifierEffect bonusMaxManaAndRegenInterval(int bonusMaxMana, int seconds) {
        return new ManaModifierEffect(bonusMaxMana, seconds);
    }

    public static VestigeEffect boundCelestialSistersMana() {
        return new ManaModifierEffect(0, null);
    }

    @Override
    public void tick(VestigeContext ctx) {
        // Standard-Modus (nicht Tag/Nacht)
        if (bonusMaxMana != 0 || regenIntervalSeconds != null) return;

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
            if (lastStatusId == 1) removeDay(player, tag);
            else if (lastStatusId == 2) removeNight(player);
        }

        if (newStatusId == 1) applyDay(player, tag);
        else applyNight(player, tag);

        tag.putInt(NBT_LAST_STATUS, newStatusId);
        tag.putBoolean(NBT_APPLIED, true);
        root.put(NBT_KEY, tag);
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        // Standard-Modus (nicht Tag/Nacht)
        if (bonusMaxMana == 0 && regenIntervalSeconds == null) return;

        Player player = ctx.player();
        if (player == null) return;

        if (bonusMaxMana != 0) {
            int currentMax = ModManaUtil.getMaxMana(player);
            ModManaUtil.setMaxMana(player, currentMax + bonusMaxMana);
        }

        if (regenIntervalSeconds != null) {
            ModManaUtil.setRegenIntervalSeconds(player, regenIntervalSeconds);
        }
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        // Tag/Nacht-Modus cleanup: nur entfernen, was tatsächlich aktiv war
        if (bonusMaxMana == 0 && regenIntervalSeconds == null) {
            CompoundTag root = player.getPersistentData();
            if (!root.contains(NBT_KEY)) return;

            CompoundTag tag = root.getCompound(NBT_KEY);
            if (!tag.getBoolean(NBT_APPLIED)) {
                root.remove(NBT_KEY);
                return;
            }

            removeDay(player, tag);
            removeNight(player);

            root.remove(NBT_KEY);
            return;
        }

        // Standard-Modus cleanup
        if (bonusMaxMana != 0) {
            int currentMax = ModManaUtil.getMaxMana(player);
            ModManaUtil.setMaxMana(player, currentMax - bonusMaxMana);
        }

        if (regenIntervalSeconds != null) {
            ModManaUtil.resetRegenIntervalSeconds(player);
        }
    }

    private static void applyDay(Player player, CompoundTag tag) {
        // idempotent: nicht erneut addieren, falls schon aktiv (z.B. nach Relog)
        if (tag.getBoolean(NBT_DAY_APPLIED)) {
            ModManaUtil.resetRegenIntervalSeconds(player);
            return;
        }

        int before = ModManaUtil.getMaxMana(player);
        ModManaUtil.setMaxMana(player, before + DAY_BONUS_MAX_MANA);
        int after = ModManaUtil.getMaxMana(player);

        int applied = Math.max(0, after - before);
        tag.putInt(NBT_APPLIED_MAX_MANA_BONUS, applied);
        tag.putBoolean(NBT_DAY_APPLIED, applied > 0);

        ModManaUtil.resetRegenIntervalSeconds(player);
    }

    private static void removeDay(Player player, CompoundTag tag) {
        if (!tag.getBoolean(NBT_DAY_APPLIED)) {
            tag.putInt(NBT_APPLIED_MAX_MANA_BONUS, 0);
            return;
        }

        int applied = tag.contains(NBT_APPLIED_MAX_MANA_BONUS) ? tag.getInt(NBT_APPLIED_MAX_MANA_BONUS) : 0;
        if (applied > 0) {
            ModManaUtil.setMaxMana(player, ModManaUtil.getMaxMana(player) - applied);
        }

        tag.putInt(NBT_APPLIED_MAX_MANA_BONUS, 0);
        tag.putBoolean(NBT_DAY_APPLIED, false);
    }

    private static void applyNight(Player player, CompoundTag tag) {
        // Nacht überschreibt den Day-Status: Day gilt als nicht aktiv
        tag.putBoolean(NBT_DAY_APPLIED, false);
        tag.putInt(NBT_APPLIED_MAX_MANA_BONUS, 0);

        int cur = ModManaUtil.getRegenIntervalSeconds(player);
        int halved = Math.max(MIN_REGEN_INTERVAL_SECONDS, cur / 2);
        ModManaUtil.setRegenIntervalSeconds(player, halved);
    }

    private static void removeNight(Player player) {
        ModManaUtil.resetRegenIntervalSeconds(player);
    }
}