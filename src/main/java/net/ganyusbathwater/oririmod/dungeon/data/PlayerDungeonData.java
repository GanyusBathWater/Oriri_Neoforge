package net.ganyusbathwater.oririmod.dungeon.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks per-player persistent dungeon progression (completed dungeons and
 * per-dungeon "Wish Upon a Star" permanent attribute blessings).
 * Stored globally in the Overworld.
 */
public class PlayerDungeonData extends SavedData {

    private static final String DATA_NAME = "oririmod_player_dungeon_data";

    // Mapping from Player UUID to a set of completed dungeon IDs.
    private final Map<UUID, Set<String>> completedDungeons = new HashMap<>();

    // Mapping from Player UUID -> (Dungeon ID -> chosen StarWishAttribute ID).
    private final Map<UUID, Map<String, String>> starWishes = new HashMap<>();

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

    public String getStarWish(UUID playerId, String dungeonId) {
        Map<String, String> wishes = starWishes.get(playerId);
        if (wishes == null) {
            return "";
        }
        return wishes.getOrDefault(dungeonId, "");
    }

    public Map<String, String> getAllStarWishes(UUID playerId) {
        Map<String, String> wishes = starWishes.get(playerId);
        return wishes == null ? Collections.emptyMap() : Collections.unmodifiableMap(wishes);
    }

    public void setStarWish(UUID playerId, String dungeonId, String attributeId) {
        starWishes.computeIfAbsent(playerId, k -> new HashMap<>()).put(dungeonId, attributeId);
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

        ListTag wishPlayerList = new ListTag();
        for (Map.Entry<UUID, Map<String, String>> entry : starWishes.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("PlayerId", entry.getKey());
            CompoundTag wishesTag = new CompoundTag();
            for (Map.Entry<String, String> wish : entry.getValue().entrySet()) {
                wishesTag.putString(wish.getKey(), wish.getValue());
            }
            playerTag.put("Wishes", wishesTag);
            wishPlayerList.add(playerTag);
        }
        tag.put("StarWishes", wishPlayerList);
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
        if (tag.contains("StarWishes")) {
            ListTag wishPlayerList = tag.getList("StarWishes", Tag.TAG_COMPOUND);
            for (int i = 0; i < wishPlayerList.size(); i++) {
                CompoundTag playerTag = wishPlayerList.getCompound(i);
                UUID playerId = playerTag.getUUID("PlayerId");
                CompoundTag wishesTag = playerTag.getCompound("Wishes");
                Map<String, String> wishes = new HashMap<>();
                for (String dungeonId : wishesTag.getAllKeys()) {
                    String attrId = wishesTag.getString(dungeonId);
                    if (!attrId.isBlank()) {
                        wishes.put(dungeonId, attrId);
                    }
                }
                if (!wishes.isEmpty()) {
                    data.starWishes.put(playerId, wishes);
                }
            }
        }
        return data;
    }
}
