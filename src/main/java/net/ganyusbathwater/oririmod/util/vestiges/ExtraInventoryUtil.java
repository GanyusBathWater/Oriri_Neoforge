package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ExtraInventoryUtil {
    public static NonNullList<ItemStack> readExtraInventory(Player player) {
        NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);
        CompoundTag root = player.getPersistentData();
        if (!root.contains(ExtraInventoryMenu.NBT_KEY)) return list;

        HolderLookup.Provider lookup = player.level().registryAccess();
        ContainerHelper.loadAllItems(root.getCompound(ExtraInventoryMenu.NBT_KEY), list, lookup);
        return list;
    }
}
