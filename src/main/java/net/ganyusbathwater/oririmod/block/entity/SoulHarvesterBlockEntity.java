package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.block.custom.SoulHarvesterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class SoulHarvesterBlockEntity extends BlockEntity {
    private final SimpleContainer inventory = new SimpleContainer(18) {
        @Override
        public void setChanged() {
            super.setChanged();
            SoulHarvesterBlockEntity.this.setChanged();
        }
    };

    private final IItemHandler itemHandler = new InvWrapper(inventory);

    private int progress = 0;
    private int maxProgress = 200; // Base ticks per generation (10 seconds)

    public SoulHarvesterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOUL_HARVESTER_BE.get(), pos, blockState);
    }

    public void drops() {
        SimpleContainer inventoryToDrop = new SimpleContainer(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventoryToDrop.setItem(i, inventory.getItem(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventoryToDrop);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.createTag(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.fromTag(tag.getList("Inventory", 10), registries);
        if (tag.contains("Progress")) {
            progress = tag.getInt("Progress");
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        // Will implement logic later. 
        // Need to check slot 13 (shard), 14-18 (upgrades), 0-12 (output)
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }
}
