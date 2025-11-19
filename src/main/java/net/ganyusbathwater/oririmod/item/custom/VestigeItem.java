// language: java
package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VestigeItem extends Item implements ModRarityCarrier {
    public static final String NBT_UNLOCKED = "UnlockedLevel";
    public static final String NBT_DISABLED_MASK = "DisabledMask";

    private final List<List<VestigeEffect>> effectsPerLevel; // Index 0 -> Stufe 1
    private final String translationKeyBase; // z.B. "tooltip.oririmod.vestige.fire_vestige"
    private final ModRarity rarity;

    // Beibehaltung bestehender Signaturen mit Default-Rarity
    public VestigeItem(Properties props, int maxLevel) {
        this(props, maxLevel, "tooltip.oririmod.vestige", ModRarity.UNIQUE);
    }

    public VestigeItem(Properties props, int maxLevel, String translationKeyBase) {
        this(props, maxLevel, translationKeyBase, ModRarity.UNIQUE);
    }

    public VestigeItem(Properties props, int maxLevel, String translationKeyBase, ModRarity rarity) {
        super(props);
        this.effectsPerLevel = new ArrayList<>(Math.max(1, maxLevel));
        for (int i = 0; i < Math.max(1, maxLevel); i++) this.effectsPerLevel.add(List.of());
        this.translationKeyBase = translationKeyBase != null ? translationKeyBase : "tooltip.oririmod.vestige";
        this.rarity = rarity != null ? rarity : ModRarity.UNIQUE;
    }

    public VestigeItem(Properties props, List<List<VestigeEffect>> perLevel) {
        this(props, perLevel, "tooltip.oririmod.vestige", ModRarity.UNIQUE);
    }

    public VestigeItem(Properties props, List<List<VestigeEffect>> perLevel, String translationKeyBase) {
        this(props, perLevel, translationKeyBase, ModRarity.UNIQUE);
    }

    public VestigeItem(Properties props, List<List<VestigeEffect>> perLevel, String translationKeyBase, ModRarity rarity) {
        super(props);
        this.effectsPerLevel = (perLevel == null || perLevel.isEmpty()) ? List.of(List.of()) : perLevel;
        this.translationKeyBase = translationKeyBase != null ? translationKeyBase : "tooltip.oririmod.vestige";
        this.rarity = rarity != null ? rarity : ModRarity.UNIQUE;
    }

    // --- ModRarityCarrier ---
    @Override
    public ModRarity getModRarity() {
        return this.rarity;
    }

    public String getTranslationKeyBase() {
        return this.translationKeyBase;
    }

    // Delegation: keine Logik mehr hier, nur Weitergabe an ModRarityCarrier
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModRarityCarrier.super.appendHoverText(stack, context, tooltip, flag);
    }

    // --- Vestige-Logik ---
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

    public void applyTick(ServerPlayer player, ItemStack stack) {
        int unlocked = getUnlockedLevel(stack);
        for (int lvl = 1; lvl <= unlocked; lvl++) {
            if (!isLevelEnabled(stack, lvl)) continue;
            for (var eff : getEffectsForLevel(lvl)) eff.tick(player, stack, lvl);
        }
    }

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

    public double sumHealthBonus(ServerPlayer player, ItemStack stack) {
        double sum = 0.0D;
        int unlocked = getUnlockedLevel(stack);
        for (int lvl = 1; lvl <= unlocked; lvl++) {
            if (!isLevelEnabled(stack, lvl)) continue;
            for (var eff : getEffectsForLevel(lvl)) {
                sum += eff.healthBonus(player, stack, lvl);
            }
        }
        return sum;
    }

    public float sumStepHeightBonus(ServerPlayer player, ItemStack stack) {
        float sum = 0F;
        int unlocked = getUnlockedLevel(stack);
        for (int lvl = 1; lvl <= unlocked; lvl++) {
            if (!isLevelEnabled(stack, lvl)) continue;
            for (var eff : getEffectsForLevel(lvl)) sum += eff.stepHeightBonus(player, stack, lvl);
        }
        return sum;
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

    // Builder erweitert um Rarity
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final List<List<VestigeEffect>> levels = new ArrayList<>();
        private String translationKeyBase = "tooltip.oririmod.vestige";
        private ModRarity rarity = ModRarity.UNIQUE;

        public Builder translationKeyBase(String base) {
            this.translationKeyBase = base;
            return this;
        }

        public Builder rarity(ModRarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder level(VestigeEffect... effects) {
            levels.add(List.of(effects));
            return this;
        }

        public VestigeItem build(Properties props) {
            return new VestigeItem(
                    props,
                    levels.isEmpty() ? List.of(List.of()) : List.copyOf(levels),
                    translationKeyBase,
                    rarity
            );
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        // Nur auf Server logik ausführen
        if (!level.isClientSide) {
            boolean moved = tryMoveToExtraInventory(player, inHand);
            if (moved) {
                // Wenn Stack komplett verschoben wurde, Handslot leeren
                if (inHand.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
            }
        }
        return super.use(level, player, hand);
    }

    /**
     * Versucht, den gegebenen Stack in das Extra-Inventar zu verschieben.
     * Gibt `true` zurück, wenn sich der Stack im Extra-Inventar verändert hat.
     */
    private boolean tryMoveToExtraInventory(Player player, ItemStack source) {
        if (source.isEmpty()) return false;

        // Extra-Container aus NBT laden
        CompoundTag root = player.getPersistentData();
        HolderLookup.Provider lookup = player.level().registryAccess();
        NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);

        if (root.contains(ExtraInventoryMenu.NBT_KEY)) {
            ContainerHelper.loadAllItems(root.getCompound(ExtraInventoryMenu.NBT_KEY), list, lookup);
        }

        SimpleContainer extra = new SimpleContainer(ExtraInventoryMenu.SIZE);
        for (int i = 0; i < ExtraInventoryMenu.SIZE; i++) {
            extra.setItem(i, list.get(i));
        }

        boolean changed = false;
        ItemStack toMove = source.copy();

        // 1) Zuerst in vorhandene Stacks einlagern
        for (int i = 0; i < extra.getContainerSize() && !toMove.isEmpty(); i++) {
            ItemStack slotStack = extra.getItem(i);
            if (slotStack.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(slotStack, toMove)) continue;

            int maxStack = Math.min(slotStack.getMaxStackSize(), extra.getMaxStackSize());
            int canMove = Math.min(maxStack - slotStack.getCount(), toMove.getCount());
            if (canMove <= 0) continue;

            slotStack.grow(canMove);
            toMove.shrink(canMove);
            extra.setItem(i, slotStack);
            changed = true;
        }

        // 2) Dann in leere Slots legen
        for (int i = 0; i < extra.getContainerSize() && !toMove.isEmpty(); i++) {
            ItemStack slotStack = extra.getItem(i);
            if (!slotStack.isEmpty()) continue;

            int maxStack = Math.min(toMove.getMaxStackSize(), extra.getMaxStackSize());
            ItemStack placed = toMove.split(maxStack);
            extra.setItem(i, placed);
            changed = true;
        }

        if (!changed) return false;

        // Extra-Container zurück in NBT speichern
        NonNullList<ItemStack> saveList = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < ExtraInventoryMenu.SIZE; i++) {
            saveList.set(i, extra.getItem(i));
        }
        CompoundTag invTag = new CompoundTag();
        ContainerHelper.saveAllItems(invTag, saveList, lookup);
        root.put(ExtraInventoryMenu.NBT_KEY, invTag);

        // Ursprünglichen Stack anpassen
        source.setCount(toMove.getCount());
        if (source.getCount() <= 0) {
            source.setCount(0);
        }
        return true;
    }

    public List<VestigeEffect> getEffectsForLevelPublic(int level) {
        return getEffectsForLevel(level);
    }
}