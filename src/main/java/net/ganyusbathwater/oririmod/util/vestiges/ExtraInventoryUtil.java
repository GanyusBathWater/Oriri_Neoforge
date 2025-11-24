// java
package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ExtraInventoryUtil {

    private static NonNullList<ItemStack> CLIENT_CACHE =
            NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);

    public static HolderLookup.Provider getClientOrFallbackLookup() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.registryAccess();
        }
        throw new IllegalStateException("RegistryAccess not available on client yet");
    }

    public static void updateClientCache(NonNullList<ItemStack> items) {
        CLIENT_CACHE = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < ExtraInventoryMenu.SIZE && i < items.size(); i++) {
            CLIENT_CACHE.set(i, items.get(i));
        }
    }

    public static NonNullList<ItemStack> readExtraInventory(Player player) {
        NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);

        if (player.level().isClientSide) {
            for (int i = 0; i < ExtraInventoryMenu.SIZE && i < CLIENT_CACHE.size(); i++) {
                list.set(i, CLIENT_CACHE.get(i));
            }
            return list;
        } else {
            CompoundTag root = player.getPersistentData();
            if (!root.contains(ExtraInventoryMenu.NBT_KEY)) {
                return list;
            }
            HolderLookup.Provider lookup = player.level().registryAccess();
            ContainerHelper.loadAllItems(root.getCompound(ExtraInventoryMenu.NBT_KEY), list, lookup);
            return list;
        }
    }
}