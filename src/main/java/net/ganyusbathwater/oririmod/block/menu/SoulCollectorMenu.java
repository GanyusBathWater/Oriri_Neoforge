package net.ganyusbathwater.oririmod.block.menu;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.custom.SoulCollectorItem;
import net.ganyusbathwater.oririmod.item.custom.SoulShardItem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SoulCollectorMenu extends AbstractContainerMenu {

    public static final int SHARD_SLOT_X = 48;
    public static final int SHARD_SLOT_Y = 58;
    public static final int INV_X = 30;
    public static final int INV_Y = 148;
    public static final int HOTBAR_Y = 206;

    private final Inventory playerInventory;
    private final int collectorSlotIndex;
    private final SimpleContainer shardContainer;
    private boolean syncing = false;

    public SoulCollectorMenu(int containerId, Inventory playerInv, int collectorSlotIndex) {
        super(ModMenuTypes.SOUL_COLLECTOR_MENU.get(), containerId);
        this.playerInventory = playerInv;
        this.collectorSlotIndex = collectorSlotIndex;

        this.shardContainer = new SimpleContainer(1) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (!syncing) {
                    saveToCollector();
                }
            }
        };

        // Load currently stored shard from the collector item
        ItemStack collector = getCollectorStack();
        if (collector.is(ModItems.SOUL_COLLECTOR.get())) {
            this.syncing = true;
            this.shardContainer.setItem(0, SoulCollectorItem.getStoredShard(collector).copy());
            this.syncing = false;
        }

        // Slot 0: Single Soul Shard slot (bound Soul Shards only — rejects blank shards)
        this.addSlot(new Slot(this.shardContainer, 0, SHARD_SLOT_X, SHARD_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SoulShardItem.hasBoundEntity(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player Inventory (3 rows x 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = col + row * 9 + 9;
                this.addSlot(createPlayerSlot(playerInv, slotIdx, INV_X + col * 18, INV_Y + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(createPlayerSlot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    private Slot createPlayerSlot(Inventory inv, int index, int x, int y) {
        boolean isLockedCollector = (index == this.collectorSlotIndex);
        return new Slot(inv, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return !isLockedCollector;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !isLockedCollector;
            }
        };
    }

    public ItemStack getCollectorStack() {
        if (this.collectorSlotIndex >= 0 && this.collectorSlotIndex < this.playerInventory.getContainerSize()) {
            return this.playerInventory.getItem(this.collectorSlotIndex);
        }
        return ItemStack.EMPTY;
    }

    public int getCollectorSlotIndex() {
        return this.collectorSlotIndex;
    }

    public ItemStack getStoredShard() {
        return this.shardContainer.getItem(0);
    }

    /**
     * Refreshes the open slot from the underlying collector stack (e.g., if a mob dies while the book is open).
     */
    public void reloadFromCollector() {
        ItemStack collector = getCollectorStack();
        if (collector.is(ModItems.SOUL_COLLECTOR.get())) {
            this.syncing = true;
            this.shardContainer.setItem(0, SoulCollectorItem.getStoredShard(collector).copy());
            this.syncing = false;
            broadcastChanges();
        }
    }

    private void saveToCollector() {
        ItemStack collector = getCollectorStack();
        if (collector.is(ModItems.SOUL_COLLECTOR.get())) {
            SoulCollectorItem.setStoredShard(collector, this.shardContainer.getItem(0));
            this.playerInventory.setChanged();
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Prevent hotbar number-key swap or offhand swap from moving the open Soul Collector
        if (clickType == ClickType.SWAP) {
            if (button == this.collectorSlotIndex || (button == 40 && this.collectorSlotIndex == Inventory.SLOT_OFFHAND)) {
                return;
            }
        }
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot.container == this.playerInventory && slot.getContainerSlot() == this.collectorSlotIndex) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveToCollector();
    }

    @Override
    public boolean stillValid(Player player) {
        return getCollectorStack().is(ModItems.SOUL_COLLECTOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();

            if (index == 0) {
                // Move from book shard slot -> player inventory
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move bound Soul Shard from player inventory -> book slot 0
                if (SoulShardItem.hasBoundEntity(stack)) {
                    if (!this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 28) {
                    if (!this.moveItemStackTo(stack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(stack, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }
        return copy;
    }
}
