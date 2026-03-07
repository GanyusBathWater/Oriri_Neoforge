package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.block.menu.EquinoxTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class EquinoxTableBlockEntity extends BlockEntity implements MenuProvider {
    // Slot indices:
    // 0 = Top, 1 = Left, 2 = Center, 3 = Right, 4 = Bottom (cross pattern)
    // 5 = Template
    // 6 = Output
    public static final int SLOT_COUNT = 7;
    public static final int SLOT_TOP = 0;
    public static final int SLOT_LEFT = 1;
    public static final int SLOT_CENTER = 2;
    public static final int SLOT_RIGHT = 3;
    public static final int SLOT_BOTTOM = 4;
    public static final int SLOT_TEMPLATE = 5;
    public static final int SLOT_OUTPUT = 6;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Output slot: no insertion allowed
            if (slot == SLOT_OUTPUT)
                return false;
            return super.isItemValid(slot, stack);
        }
    };

    public EquinoxTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EQUINOX_TABLE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.oririmod.equinox_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EquinoxTableMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
}
