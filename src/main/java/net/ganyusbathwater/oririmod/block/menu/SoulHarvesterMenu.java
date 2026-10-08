package net.ganyusbathwater.oririmod.block.menu;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.entity.SoulHarvesterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.item.Item;

public class SoulHarvesterMenu extends AbstractContainerMenu {
    private final SoulHarvesterBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    // Client Constructor
    public SoulHarvesterMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, null, new SimpleContainerData(2));
    }

    // Server Constructor
    public SoulHarvesterMenu(int containerId, Inventory playerInv, SoulHarvesterBlockEntity entity) {
        this(containerId, playerInv, entity, entity.getContainerData());
    }

    public SoulHarvesterMenu(int containerId, Inventory playerInv, SoulHarvesterBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SOUL_HARVESTER_MENU.get(), containerId);
        checkContainerSize(playerInv, 36);
        this.blockEntity = entity;
        this.levelAccess = (entity == null) ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos());
        this.data = data;

        net.neoforged.neoforge.items.IItemHandler shardHandler = entity == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(new net.minecraft.world.SimpleContainer(1)) : new net.neoforged.neoforge.items.wrapper.InvWrapper(this.blockEntity.getShardInv());
        net.neoforged.neoforge.items.IItemHandler upgradesHandler = entity == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(new net.minecraft.world.SimpleContainer(5)) : new net.neoforged.neoforge.items.wrapper.InvWrapper(this.blockEntity.getUpgradesInv());
        net.neoforged.neoforge.items.IItemHandler storageHandler = entity == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(new net.minecraft.world.SimpleContainer(27)) : new net.neoforged.neoforge.items.wrapper.InvWrapper(this.blockEntity.getStorageInv());

        // Shard slot (0)
        this.addSlot(new SlotItemHandler(shardHandler, 0, 80, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.SOUL_SHARD.get());
            }
        });

        // Upgrade slots (1 to 5)
        // 0 = Looting, 1 = XP, 2 = Speed, 3 = Fire Aspect, 4 = Player Kill
        Item[] expectedUpgrades = {
            ModItems.SOUL_HARVESTER_LOOTING_UPGRADE.get(),
            ModItems.SOUL_HARVESTER_XP_UPGRADE.get(),
            ModItems.SOUL_HARVESTER_SPEED_UPGRADE.get(),
            ModItems.SOUL_HARVESTER_FIRE_ASPECT_UPGRADE.get(),
            ModItems.SOUL_HARVESTER_PLAYER_KILL_UPGRADE.get()
        };

        for (int i = 0; i < 5; i++) {
            final int upgradeIndex = i;
            this.addSlot(new SlotItemHandler(upgradesHandler, i, 182, 20 + (i * 18)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(expectedUpgrades[upgradeIndex]);
                }
            });
        }

        // Storage slots (6 to 32)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new SlotItemHandler(storageHandler, (row * 9) + col, 8 + col * 18, 72 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Output only
                    }
                });
            }
        }

        // Player Inventory
        addPlayerInventory(playerInv);
        addPlayerHotbar(playerInv);

        addDataSlots(data);
    }

    public SoulHarvesterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int progressArrowSize = 63; // Scaled to our custom progress bar width (165 - 102)
        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    private void addPlayerInventory(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInv) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, ModBlocks.SOUL_HARVESTER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            // Total custom slots = 1 (shard) + 5 (upgrades) + 27 (storage) = 33
            if (index < 33) {
                // From container to player inventory
                if (!this.moveItemStackTo(slotStack, 33, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to container
                if (!this.moveItemStackTo(slotStack, 0, 33, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return itemStack;
    }
}
