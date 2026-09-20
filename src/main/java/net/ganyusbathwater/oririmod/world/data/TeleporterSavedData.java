package net.ganyusbathwater.oririmod.world.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleporterSavedData extends SavedData {
    private final Map<String, List<BlockPos>> teleporters = new HashMap<>();

    public boolean addTeleporter(String id, BlockPos pos) {
        if (id == null || id.isEmpty()) return false;
        
        teleporters.putIfAbsent(id, new ArrayList<>());
        List<BlockPos> list = teleporters.get(id);
        
        if (list.size() >= 2 && !list.contains(pos)) {
            return false; // Frequency full
        }
        
        if (!list.contains(pos)) {
            list.add(pos);
            this.setDirty();
        }
        return true;
    }

    public void removeTeleporter(String id, BlockPos pos) {
        if (id == null || id.isEmpty()) return;
        List<BlockPos> list = teleporters.get(id);
        if (list != null) {
            if (list.remove(pos)) {
                if (list.isEmpty()) {
                    teleporters.remove(id);
                }
                this.setDirty();
            }
        }
    }

    public void removeTeleporter(BlockPos pos) {
        boolean changed = false;
        var iter = teleporters.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (entry.getValue().remove(pos)) {
                changed = true;
                if (entry.getValue().isEmpty()) {
                    iter.remove();
                }
            }
        }
        if (changed) {
            this.setDirty();
        }
    }

    public BlockPos getDestination(String id, BlockPos fromPos) {
        List<BlockPos> list = teleporters.get(id);
        if (list == null || list.size() < 2) return null;
        for (BlockPos p : list) {
            if (!p.equals(fromPos)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<String, List<BlockPos>> entry : teleporters.entrySet()) {
            ListTag listTag = new ListTag();
            for (BlockPos p : entry.getValue()) {
                listTag.add(new IntArrayTag(new int[]{p.getX(), p.getY(), p.getZ()}));
            }
            mapTag.put(entry.getKey(), listTag);
        }
        tag.put("Teleporters", mapTag);
        return tag;
    }

    public static TeleporterSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TeleporterSavedData data = new TeleporterSavedData();
        CompoundTag mapTag = tag.getCompound("Teleporters");
        for (String key : mapTag.getAllKeys()) {
            ListTag listTag = mapTag.getList(key, Tag.TAG_INT_ARRAY);
            List<BlockPos> posList = new ArrayList<>();
            for (int i = 0; i < listTag.size(); i++) {
                int[] arr = listTag.getIntArray(i);
                if (arr.length == 3) {
                    posList.add(new BlockPos(arr[0], arr[1], arr[2]));
                }
            }
            if (!posList.isEmpty()) {
                data.teleporters.put(key, posList);
            }
        }
        return data;
    }

    public static TeleporterSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TeleporterSavedData::new, TeleporterSavedData::load),
                "oririmod_teleporters_" + level.dimension().location().getPath()
        );
    }
}
