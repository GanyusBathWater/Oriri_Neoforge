package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.item.custom.vestiges.MirrorOfTheVoid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MirrorOfTheVoidEffect implements VestigeEffect {

    private static final String NBT_VOID_MIRROR_KEY = "Oriri_VoidMirror";
    private static final String NBT_DODGE_ENABLED = "DodgeEnabled";
    private static final String NBT_DODGE_BASE_COOLDOWN_SECONDS = "DodgeBaseCooldownSeconds";

    private static final String NBT_DODGE_ACTIVE_KEY = "Oriri_VoidMirror_Dodge";
    private static final String NBT_DODGE_ACTIVE_COOLDOWN_SECONDS = "CooldownSeconds";

    private static final String NBT_DODGE_ACTIVE_NEXT_DECREMENT_TICK = "NextDecrementTick";

    @OnlyIn(Dist.CLIENT)
    private static final Map<UUID, Integer> CLIENT_COOLDOWN_SECONDS = new ConcurrentHashMap<>();

    public MirrorOfTheVoidEffect(int ignoredCooldownTime) {}

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        ItemStack stack = ctx.stack();
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof MirrorOfTheVoid)) return;

        int level = Math.max(0, VestigeItem.getUnlockedLevel(stack));
        boolean enabled = level > 0;

        setDodgeEnabled(player, enabled);

        tickActiveCooldown(player, enabled);

        if (!enabled) return;

        int baseCooldown = cooldownSecondsForLevel(level);
        setBaseCooldownSecondsIfChanged(player, baseCooldown);
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;
        tick(ctx);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        setDodgeEnabled(player, false);

        CompoundTag root = player.getPersistentData();

        CompoundTag tag = root.getCompound(NBT_VOID_MIRROR_KEY);
        tag.remove(NBT_DODGE_BASE_COOLDOWN_SECONDS);
        root.put(NBT_VOID_MIRROR_KEY, tag);

        CompoundTag active = root.getCompound(NBT_DODGE_ACTIVE_KEY);
        active.putInt(NBT_DODGE_ACTIVE_COOLDOWN_SECONDS, 0);
        active.remove(NBT_DODGE_ACTIVE_NEXT_DECREMENT_TICK);
        root.put(NBT_DODGE_ACTIVE_KEY, active);

        syncCooldownToClientCache(player, 0);
    }

    private static void tickActiveCooldown(Player player, boolean enabled) {
        if (player == null || player.level().isClientSide) return;

        long now = player.level().getGameTime();

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_DODGE_ACTIVE_KEY);

        int cd = tag.getInt(NBT_DODGE_ACTIVE_COOLDOWN_SECONDS);

        if (!enabled) {
            if (cd != 0) {
                tag.putInt(NBT_DODGE_ACTIVE_COOLDOWN_SECONDS, 0);
                root.put(NBT_DODGE_ACTIVE_KEY, tag);
                syncCooldownToClientCache(player, 0);
            }
            tag.remove(NBT_DODGE_ACTIVE_NEXT_DECREMENT_TICK);
            root.put(NBT_DODGE_ACTIVE_KEY, tag);
            return;
        }

        if (cd <= 0) {
            tag.remove(NBT_DODGE_ACTIVE_NEXT_DECREMENT_TICK);
            root.put(NBT_DODGE_ACTIVE_KEY, tag);
            if (cd != 0) syncCooldownToClientCache(player, 0);
            return;
        }

        long next = tag.getLong(NBT_DODGE_ACTIVE_NEXT_DECREMENT_TICK);

        if (next <= 0L) {
            tag.putLong(NBT_DODGE_ACTIVE_NEXT_DECREMENT_TICK, now + 20L);
            root.put(NBT_DODGE_ACTIVE_KEY, tag);
            syncCooldownToClientCache(player, cd);
            return;
        }

        if (now < next) {
            syncCooldownToClientCache(player, cd);
            return;
        }

        int newCd = cd - 1;
        tag.putInt(NBT_DODGE_ACTIVE_COOLDOWN_SECONDS, newCd);
        tag.putLong(NBT_DODGE_ACTIVE_NEXT_DECREMENT_TICK, now + 20L);
        root.put(NBT_DODGE_ACTIVE_KEY, tag);

        syncCooldownToClientCache(player, newCd);
    }

    private static int cooldownSecondsForLevel(int level) {
        return switch (level) {
            case 1 -> 60;
            case 2 -> 45;
            case 3 -> 30;
            default -> 60;
        };
    }

    public static boolean isDodgeEnabled(Player player) {
        if (player == null) return false;
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_VOID_MIRROR_KEY);
        return tag.getBoolean(NBT_DODGE_ENABLED);
    }

    public static void setDodgeEnabled(Player player, boolean enabled) {
        if (player == null || player.level().isClientSide) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_VOID_MIRROR_KEY);

        boolean before = tag.getBoolean(NBT_DODGE_ENABLED);
        if (before == enabled) return;

        tag.putBoolean(NBT_DODGE_ENABLED, enabled);
        root.put(NBT_VOID_MIRROR_KEY, tag);
    }

    public static int getBaseCooldownSeconds(Player player) {
        if (player == null) return 0;
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_VOID_MIRROR_KEY);
        return Math.max(0, tag.getInt(NBT_DODGE_BASE_COOLDOWN_SECONDS));
    }

    private static void setBaseCooldownSecondsIfChanged(Player player, int seconds) {
        if (player == null || player.level().isClientSide) return;

        int clamped = Math.max(0, seconds);

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_VOID_MIRROR_KEY);

        int before = tag.getInt(NBT_DODGE_BASE_COOLDOWN_SECONDS);
        if (before == clamped) return;

        tag.putInt(NBT_DODGE_BASE_COOLDOWN_SECONDS, clamped);
        root.put(NBT_VOID_MIRROR_KEY, tag);
    }

    public static int getActiveCooldownSecondsForHud(Player player) {
        if (player == null) return 0;
        if (!player.level().isClientSide) {
            CompoundTag root = player.getPersistentData();
            CompoundTag tag = root.getCompound(NBT_DODGE_ACTIVE_KEY);
            return Math.max(0, tag.getInt(NBT_DODGE_ACTIVE_COOLDOWN_SECONDS));
        }
        return Math.max(0, getClientCooldownSeconds(player.getUUID()));
    }

    @OnlyIn(Dist.CLIENT)
    private static int getClientCooldownSeconds(UUID playerId) {
        return CLIENT_COOLDOWN_SECONDS.getOrDefault(playerId, 0);
    }

    private static void syncCooldownToClientCache(Player player, int seconds) {
        if (player == null) return;
        if (player.level().isClientSide) return;
        player.getServer().execute(() -> {
            player.getServer().getPlayerList().getPlayers().forEach(p -> {
                if (p.getUUID().equals(player.getUUID())) {
                    MirrorOfTheVoidEffect.setClientCooldownSeconds(p.getUUID(), seconds);
                }
            });
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void setClientCooldownSeconds(UUID playerId, int seconds) {
        CLIENT_COOLDOWN_SECONDS.put(playerId, Math.max(0, seconds));
    }
}