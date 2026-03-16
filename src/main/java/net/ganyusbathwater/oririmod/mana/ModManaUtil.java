package net.ganyusbathwater.oririmod.mana;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.ganyusbathwater.oririmod.enchantment.ModEnchantments;

public class ModManaUtil {
    private static final String NBT_KEY = "OririMana";
    private static final String NBT_MAX_KEY = "OririMaxMana";
    private static final String NBT_TICK_KEY = "OririManaTick";
    private static final String NBT_REGEN_INTERVAL_KEY = "OririRegenIntervalSeconds";
    private static final int DEFAULT_MANA = 100;

    private ModManaUtil() {
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static void syncToClient(ServerPlayer sp) {
        NetworkHandler.sendManaToPlayer(sp, getMana(sp), getMaxMana(sp));
    }

    public static int getMana(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_KEY)) {
            int init = Math.min(DEFAULT_MANA, getMaxMana(player));
            data.putInt(NBT_KEY, init);
            return init;
        }
        return data.getInt(NBT_KEY);
    }

    public static Integer getManaIfPresent(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(NBT_KEY) ? data.getInt(NBT_KEY) : null;
    }

    public static void setMana(Player player, int mana) {
        int clamped = clamp(mana, 0, getMaxMana(player));
        player.getPersistentData().putInt(NBT_KEY, clamped);
        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    public static void resetMana(Player player) {
        int base = Math.min(DEFAULT_MANA, getMaxMana(player));
        player.getPersistentData().putInt(NBT_KEY, base);
        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    public static boolean tryConsumeMana(Player player, int amount) {
        if (amount <= 0)
            return true;
        // Creative-Modus: kein Verbrauch
        if (player.isCreative())
            return true;
        int current = getMana(player);
        if (current < amount)
            return false;
        setMana(player, current - amount);
        return true;
    }

    public static boolean tryConsumeMana(Player player, int amount, ItemStack weaponStack) {
        if (amount <= 0)
            return true;
        if (player.isCreative())
            return true;

        var registryAccess = player.level().registryAccess();
        var manaSavingsOpt = registryAccess.lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(ModEnchantments.MANA_SAVINGS));

        if (manaSavingsOpt.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(manaSavingsOpt.get(), weaponStack);
            if (level > 0) {
                // Reduces cost by 10% per level. Max 50% at level 5.
                float discount = 1.0f - (0.10f * level);
                amount = Math.max(1, Math.round(amount * discount)); // Always costs at least 1 mana unless it's 0 to
                                                                     // begin with
            }
        }

        int current = getMana(player);
        if (current < amount)
            return false;
        setMana(player, current - amount);
        return true;
    }

    public static void addMana(Player player, int amount) {
        if (amount <= 0)
            return;
        setMana(player, getMana(player) + amount);
    }

    public static int getMaxMana(Player player) {
        CompoundTag data = player.getPersistentData();
        int baseMax = data.contains(NBT_MAX_KEY)
                ? data.getInt(NBT_MAX_KEY)
                : OririConfig.COMMON.mana.maxMana.get();

        var registryAccess = player.level().registryAccess();
        var capacityEnchantOpt = registryAccess.lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(ModEnchantments.MANA_CAPACITY));

        int bonus = 0;
        if (capacityEnchantOpt.isPresent()) {
            var capacityEnchant = capacityEnchantOpt.get();
            for (ItemStack armor : player.getArmorSlots()) {
                if (!armor.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(capacityEnchant, armor) > 0) {
                    bonus += 25;
                }
            }
        }

        return baseMax + bonus;
    }

    public static Integer getMaxManaIfPresent(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(NBT_MAX_KEY) ? data.getInt(NBT_MAX_KEY) : null;
    }

    public static void setMaxMana(Player player, int maxMana) {
        int clampedMax = clamp(maxMana, 100, 100000);
        CompoundTag data = player.getPersistentData();
        data.putInt(NBT_MAX_KEY, clampedMax);
        int cur = getMana(player);
        if (cur > clampedMax) {
            data.putInt(NBT_KEY, clampedMax);
        }
        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    public static void resetMaxMana(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(NBT_MAX_KEY);
        int max = getMaxMana(player);
        int mana = getMana(player);
        if (mana > max) {
            data.putInt(NBT_KEY, max);
        }
        if (player instanceof ServerPlayer sp)
            syncToClient(sp);
    }

    public static void incTickCounter(Player player) {
        CompoundTag data = player.getPersistentData();
        int v = data.getInt(NBT_TICK_KEY);
        data.putInt(NBT_TICK_KEY, v + 1);
    }

    public static int getTickCounter(Player player) {
        return player.getPersistentData().getInt(NBT_TICK_KEY);
    }

    public static void setTickCounter(Player player, int value) {
        player.getPersistentData().putInt(NBT_TICK_KEY, Math.max(0, value));
    }

    public static void tick(Player player) {
        if (player.level().isClientSide)
            return;
        CompoundTag data = player.getPersistentData();
        int intervalTicks = getRegenIntervalTicks(player);
        if (intervalTicks <= 0)
            return;
        int tick = data.getInt(NBT_TICK_KEY) + 1;
        if (tick >= intervalTicks) {
            tick = 0;
            int regen = OririConfig.COMMON.mana.regenAmount.get();
            int before = getMana(player);
            if (before < getMaxMana(player)) {
                setMana(player, before + regen);
            }
        }
        data.putInt(NBT_TICK_KEY, tick);
    }

    public static void setRegenIntervalSeconds(Player player, int seconds) {
        int clamped = clamp(seconds, 0, 3600);
        player.getPersistentData().putInt(NBT_REGEN_INTERVAL_KEY, clamped);
    }

    public static int getRegenIntervalSeconds(Player player) {
        return getRegenIntervalTicks(player) / 20;
    }

    public static int getRegenIntervalTicks(Player player) {
        CompoundTag data = player.getPersistentData();
        int baseInterval = data.contains(NBT_REGEN_INTERVAL_KEY)
                ? data.getInt(NBT_REGEN_INTERVAL_KEY)
                : OririConfig.COMMON.mana.regenIntervalSeconds.get();

        int baseTicks = baseInterval * 20;

        var registryAccess = player.level().registryAccess();
        var regenEnchantOpt = registryAccess.lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(ModEnchantments.MANA_REGENERATION));

        int enchantCount = 0;
        if (regenEnchantOpt.isPresent()) {
            var regenEnchant = regenEnchantOpt.get();
            for (ItemStack armor : player.getArmorSlots()) {
                if (!armor.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(regenEnchant, armor) > 0) {
                    enchantCount++;
                }
            }
        }

        if (enchantCount > 0) {
            // Speed up regen by 10% per armor piece with the enchantment:
            float speedMultiplier = Math.max(0.1f, 1.0f - (0.10f * enchantCount));
            return Math.max(1, Math.round(baseTicks * speedMultiplier));
        }

        return baseTicks;
    }

    public static void resetRegenIntervalSeconds(Player player) {
        player.getPersistentData().remove(NBT_REGEN_INTERVAL_KEY);
    }

    public static void setMaxManaClient(Player player, int maxMana) {
        if (!player.level().isClientSide) {
            setMaxMana(player, maxMana);
            return;
        }
        int clampedMax = clamp(maxMana, 100, 100000);
        CompoundTag data = player.getPersistentData();
        data.putInt(NBT_MAX_KEY, clampedMax);
        int cur = data.getInt(NBT_KEY);
        if (cur > clampedMax) {
            data.putInt(NBT_KEY, clampedMax);
        }
    }

    public static void setManaClient(Player player, int mana) {
        if (!player.level().isClientSide) {
            setMana(player, mana);
            return;
        }
        int clamped = clamp(mana, 0, getMaxMana(player));
        player.getPersistentData().putInt(NBT_KEY, clamped);
    }
}