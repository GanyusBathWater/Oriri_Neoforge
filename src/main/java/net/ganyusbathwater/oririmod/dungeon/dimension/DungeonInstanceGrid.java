package net.ganyusbathwater.oririmod.dungeon.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages the allocation of 2048x2048 grid slots for dungeon instances in a specific dimension.
 * Searches for free slots in an outward spiral from (0,0).
 */
public class DungeonInstanceGrid {
    public static final int SLOT_SIZE = 2048; // Minimum spacing between dungeons

    // Stores occupied slot coordinates as "x,z" to easily check availability
    private final Set<String> occupiedSlots = new HashSet<>();

    /**
     * Finds the nearest available slot (outward spiral) and marks it as occupied.
     * @return The exact BlockPos origin (Y=64) where the structure should be placed.
     */
    public BlockPos allocateSlot() {
        int radius = 0;
        while (true) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        String key = x + "," + z;
                        if (!occupiedSlots.contains(key)) {
                            occupiedSlots.add(key);
                            return new BlockPos(x * SLOT_SIZE, 64, z * SLOT_SIZE);
                        }
                    }
                }
            }
            radius++;
        }
    }

    /**
     * Frees a slot previously allocated by allocateSlot().
     */
    public void freeSlot(BlockPos origin) {
        int x = origin.getX() / SLOT_SIZE;
        int z = origin.getZ() / SLOT_SIZE;
        occupiedSlots.remove(x + "," + z);
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String slot : occupiedSlots) {
            list.add(StringTag.valueOf(slot));
        }
        tag.put("OccupiedSlots", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        occupiedSlots.clear();
        if (tag.contains("OccupiedSlots")) {
            ListTag list = tag.getList("OccupiedSlots", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                occupiedSlots.add(list.getString(i));
            }
        }
    }
}
