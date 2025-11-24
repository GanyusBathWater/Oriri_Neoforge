package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VestigeManager {

    private static final Map<UUID, NonNullList<ItemStack>> EXTRA_CACHE = new HashMap<>();

    private VestigeManager() {}

    public static void tick(ServerPlayer player) {
        NonNullList<ItemStack> extra = loadExtraInventory(player);
        // \- hier ggf. Effekte aller Vestiges ticken lassen
        for (ItemStack stack : extra) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof VestigeItem vestige) {
                vestige.applyTick(player, stack);
            }
        }
    }

    public static NonNullList<ItemStack> getExtraInventory(ServerPlayer player) {
        return loadExtraInventory(player);
    }

    public static boolean hasKeepInventory(ServerPlayer player) {
        for (ItemStack stack : loadExtraInventory(player)) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof VestigeItem vestige) {
                if (vestige.grantsKeepInventory(player, stack)) return true;
            }
        }
        return false;
    }

    private static NonNullList<ItemStack> loadExtraInventory(ServerPlayer player) {
        UUID id = player.getUUID();
        NonNullList<ItemStack> cached = EXTRA_CACHE.get(id);
        if (cached != null) return cached;

        var root = player.getPersistentData();
        var lookup = player.level().registryAccess();
        NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);

        if (root.contains(ExtraInventoryMenu.NBT_KEY)) {
            ContainerHelper.loadAllItems(root.getCompound(ExtraInventoryMenu.NBT_KEY), list, lookup);
        }

        EXTRA_CACHE.put(id, list);
        return list;
    }

    public static void invalidate(ServerPlayer player) {
        EXTRA_CACHE.remove(player.getUUID());
    }
}