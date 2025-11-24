// java
package net.ganyusbathwater.oririmod.menu;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ExtraInventoryMenu extends AbstractContainerMenu {
    public static final String NBT_KEY = "OririExtraInventory";
    public static final int SIZE = 4;

    private static final int ROWS = 6;

    private final Player player;
    private final Inventory playerInv;
    private final SimpleContainer extra;

    public ExtraInventoryMenu(int windowId, Inventory inv) {
        super(ModMenus.EXTRA_INVENTORY.value(), windowId);
        this.playerInv = inv;
        this.player = inv.player;
        this.extra = loadOrCreate(player);

        int xStart = 8;
        int yStart = 18;

        int[] CHEST_SLOTS_1_BASED = {12, 16, 39, 43};
        for (int i = 0; i < SIZE; i++) {
            int n0 = CHEST_SLOTS_1_BASED[i] - 1;
            int row = n0 / 9;
            int col = n0 % 9;
            int x = xStart + col * 18;
            int y = yStart + row * 18;
            this.addSlot(new Slot(extra, i, x, y));
        }

        int playerInvY = yStart + ROWS * 18 + 14;
        addPlayerInventory(inv, xStart, playerInvY);
        addPlayerHotbar(inv, xStart, playerInvY + 58);
    }

    private void addPlayerInventory(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv, int x, int y) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, x + i * 18, y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveBack(player, extra);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack in = slot.getItem();
            stack = in.copy();

            int extraEnd = SIZE;
            int invStart = SIZE;
            int hotbarEnd = invStart + 27 + 9;

            if (index < extraEnd) {
                if (!this.moveItemStackTo(in, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(in, 0, extraEnd, false)) return ItemStack.EMPTY;
            }

            if (in.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (in.getCount() == stack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, in);
        }
        return stack;
    }

    private static SimpleContainer loadOrCreate(Player player) {
        SimpleContainer cont = new SimpleContainer(SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                saveBack(player, this);
            }
        };
        CompoundTag root = player.getPersistentData();
        if (root.contains(NBT_KEY)) {
            NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            HolderLookup.Provider lookup = player.level().registryAccess();
            ContainerHelper.loadAllItems(root.getCompound(NBT_KEY), list, lookup);
            for (int i = 0; i < SIZE; i++) cont.setItem(i, list.get(i));
        }
        return cont;
    }

    private static void saveBack(Player player, SimpleContainer cont) {
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            list.set(i, cont.getItem(i));
        }

        CompoundTag invTag = new CompoundTag();
        HolderLookup.Provider lookup = player.level().registryAccess();
        ContainerHelper.saveAllItems(invTag, list, lookup);
        player.getPersistentData().put(NBT_KEY, invTag);

        if (player instanceof ServerPlayer sp) {
            NetworkHandler.sendExtraInventoryTo(sp, list);
        }
    }
}