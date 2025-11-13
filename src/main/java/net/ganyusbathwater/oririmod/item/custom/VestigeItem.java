package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VestigeItem extends Item {
    public static final String NBT_UNLOCKED = "UnlockedLevel";
    public static final String NBT_DISABLED_MASK = "DisabledMask";

    private final List<List<VestigeEffect>> effectsPerLevel; // Index 0 -> Stufe 1

    public VestigeItem(Properties props, int maxLevel) {
        super(props);
        this.effectsPerLevel = new ArrayList<>(Math.max(1, maxLevel));
        for (int i = 0; i < Math.max(1, maxLevel); i++) this.effectsPerLevel.add(List.of());
    }

    public VestigeItem(Properties props, List<List<VestigeEffect>> perLevel) {
        super(props);
        this.effectsPerLevel = (perLevel == null || perLevel.isEmpty()) ? List.of(List.of()) : perLevel;
    }

    public int getMaxLevel() {
        return effectsPerLevel.size();
    }

    public int getUnlockedLevel(ItemStack stack) {
        int lvl = getInt(stack, NBT_UNLOCKED, 1);
        if (lvl <= 0) lvl = 1;
        return Math.min(lvl, getMaxLevel());
    }

    public void setUnlockedLevel(ItemStack stack, int level) {
        int clamped = Math.max(1, Math.min(level, getMaxLevel()));
        putInt(stack, NBT_UNLOCKED, clamped);
    }

    public boolean isLevelEnabled(ItemStack stack, int level) {
        int mask = getInt(stack, NBT_DISABLED_MASK, 0);
        int bit = 1 << (level - 1);
        return (mask & bit) == 0;
    }

    public void setLevelEnabled(ItemStack stack, int level, boolean enabled) {
        int bit = 1 << (level - 1);
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            int mask = tag.getInt(NBT_DISABLED_MASK);
            mask = enabled ? (mask & ~bit) : (mask | bit);
            tag.putInt(NBT_DISABLED_MASK, mask);
            return CustomData.of(tag);
        });
    }

    protected List<VestigeEffect> getEffectsForLevel(int level) {
        if (level <= 0 || level > effectsPerLevel.size()) return Collections.emptyList();
        return effectsPerLevel.get(level - 1);
    }

    // Wird pro Tick aufgerufen
    public void applyTick(ServerPlayer player, ItemStack stack) {
        int unlocked = getUnlockedLevel(stack);
        for (int lvl = 1; lvl <= unlocked; lvl++) {
            if (!isLevelEnabled(stack, lvl)) continue;
            for (var eff : getEffectsForLevel(lvl)) eff.tick(player, stack, lvl);
        }
    }

    // Wird von Events genutzt
    public boolean grantsKeepInventory(ServerPlayer player, ItemStack stack) {
        int unlocked = getUnlockedLevel(stack);
        for (int lvl = 1; lvl <= unlocked; lvl++) {
            if (!isLevelEnabled(stack, lvl)) continue;
            for (var eff : getEffectsForLevel(lvl)) {
                if (eff.keepInventoryOnDeath(player, stack, lvl)) return true;
            }
        }
        return false;
    }

    // Summiert z.B. Step-Height-Bonus
    public float sumStepHeightBonus(ServerPlayer player, ItemStack stack) {
        float sum = 0F;
        int unlocked = getUnlockedLevel(stack);
        for (int lvl = 1; lvl <= unlocked; lvl++) {
            if (!isLevelEnabled(stack, lvl)) continue;
            for (var eff : getEffectsForLevel(lvl)) sum += eff.stepHeightBonus(player, stack, lvl);
        }
        return sum;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int max = getMaxLevel();
        int unlocked = getUnlockedLevel(stack);
        tooltip.add(Component.translatable("tooltip.oririmod.vestige.level", unlocked, max).withStyle(ChatFormatting.AQUA));

        int mask = getInt(stack, NBT_DISABLED_MASK, 0);
        for (int lvl = 1; lvl <= unlocked; lvl++) {
            boolean enabled = (mask & (1 << (lvl - 1))) == 0;
            tooltip.add(Component.translatable(
                    enabled ? "tooltip.oririmod.vestige.level.enabled" : "tooltip.oririmod.vestige.level.disabled", lvl
            ).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }

    // Kleiner Builder für definierte Stufen
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final List<List<VestigeEffect>> levels = new ArrayList<>();
        public Builder level(VestigeEffect... effects) {
            levels.add(List.of(effects));
            return this;
        }
        public VestigeItem build(Properties props) {
            return new VestigeItem(props, levels.isEmpty() ? List.of(List.of()) : List.copyOf(levels));
        }
    }

    // --- Helpers für CustomData (Data Components) ---

    private static int getInt(ItemStack stack, String key, int def) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return def;
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getInt(key) : def;
    }

    private static void putInt(ItemStack stack, String key, int value) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.putInt(key, value);
            return CustomData.of(tag);
        });
    }
}