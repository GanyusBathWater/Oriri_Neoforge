package net.ganyusbathwater.oririmod.dungeon.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which dungeons each player has completed.
 * Stored globally in the Overworld.
 */
public class PlayerDungeonData extends SavedData {

    private static final String DATA_NAME = "oririmod_player_dungeon_data";

    // Mapping from Player UUID to a set of completed dungeon IDs.
    private final Map<UUID, Set<String>> completedDungeons = new HashMap<>();

    public static PlayerDungeonData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        assert overworld != null;
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlayerDungeonData::new, PlayerDungeonData::load),
                DATA_NAME
        );
    }

    public boolean hasCompleted(UUID playerId, String dungeonId) {
        return completedDungeons.getOrDefault(playerId, Set.of()).contains(dungeonId);
    }

    public void markCompleted(UUID playerId, String dungeonId) {
        completedDungeons.computeIfAbsent(playerId, k -> new HashSet<>()).add(dungeonId);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : completedDungeons.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("PlayerId", entry.getKey());
            
            ListTag dungeonsTag = new ListTag();
            for (String dungeonId : entry.getValue()) {
                dungeonsTag.add(StringTag.valueOf(dungeonId));
            }
            playerTag.put("Dungeons", dungeonsTag);
            playerList.add(playerTag);
        }
        tag.put("Players", playerList);
        return tag;
    }

    public static PlayerDungeonData load(CompoundTag tag, HolderLookup.Provider provider) {
        PlayerDungeonData data = new PlayerDungeonData();
        if (tag.contains("Players")) {
            ListTag playerList = tag.getList("Players", Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                UUID playerId = playerTag.getUUID("PlayerId");
                
                Set<String> dungeons = new HashSet<>();
                ListTag dungeonsTag = playerTag.getList("Dungeons", Tag.TAG_STRING);
                for (int j = 0; j < dungeonsTag.size(); j++) {
                    dungeons.add(dungeonsTag.getString(j));
                }
                data.completedDungeons.put(playerId, dungeons);
            }
        }
        return data;
    }
}
