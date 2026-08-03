package net.ganyusbathwater.oririmod.dungeon.party;

import net.ganyusbathwater.oririmod.dungeon.DungeonDefinition;
import net.ganyusbathwater.oririmod.dungeon.DungeonManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Server-side singleton (SavedData) that manages all active DungeonParty objects.
 * Attached to the Overworld.
 */
public class DungeonPartyManager extends SavedData {

    private static final String DATA_NAME = "oririmod_dungeon_parties";

    // partyId → party
    private final Map<UUID, DungeonParty> parties = new HashMap<>();
    // playerId → partyId  (for fast lookup)
    private final Map<UUID, UUID> playerToParty = new HashMap<>();

    // -------------------------------------------------------------------------
    //  Lifecycle
    // -------------------------------------------------------------------------

    public static DungeonPartyManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        assert overworld != null;
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DungeonPartyManager::new, DungeonPartyManager::load),
                DATA_NAME
        );
    }

    // -------------------------------------------------------------------------
    //  Party Management
    // -------------------------------------------------------------------------

    public DungeonParty createParty(UUID leaderId, String dungeonId) {
        // Remove any existing party the leader is in
        removePlayerFromParty(leaderId);
        DungeonParty party = new DungeonParty(leaderId, dungeonId);
        parties.put(party.getPartyId(), party);
        playerToParty.put(leaderId, party.getPartyId());
        setDirty();
        return party;
    }

    public void invitePlayer(DungeonParty party, UUID targetId) {
        party.invitePlayer(targetId);
        playerToParty.put(targetId, party.getPartyId());
        setDirty();
    }

    public void respondToInvite(UUID playerId, boolean accepted) {
        DungeonParty party = getPartyForPlayer(playerId);
        if (party == null) return;
        party.respondInvite(playerId, accepted);
        if (!accepted) {
            playerToParty.remove(playerId);
        }
        setDirty();
    }

    public void removePlayerFromParty(UUID playerId) {
        UUID partyId = playerToParty.remove(playerId);
        if (partyId == null) return;
        DungeonParty party = parties.get(partyId);
        if (party == null) return;
        if (party.isLeader(playerId)) {
            // Dissolve the whole party if the leader leaves
            dissolveParty(partyId);
        } else {
            party.removePlayer(playerId);
            setDirty();
        }
    }

    public void dissolveParty(UUID partyId) {
        DungeonParty party = parties.remove(partyId);
        if (party != null) {
            playerToParty.remove(party.getLeaderId());
            for (UUID memberId : party.getMemberStatuses().keySet()) {
                playerToParty.remove(memberId);
            }
        }
        setDirty();
    }

    @Nullable
    public DungeonParty getPartyForPlayer(UUID playerId) {
        UUID partyId = playerToParty.get(playerId);
        return partyId != null ? parties.get(partyId) : null;
    }

    @Nullable
    public DungeonParty getParty(UUID partyId) {
        return parties.get(partyId);
    }

    /**
     * Called by the leader from the NPC screen. Validates readiness and starts the dungeon.
     * Returns null on failure with a reason logged.
     */
    @Nullable
    public net.ganyusbathwater.oririmod.dungeon.DungeonInstance startDungeon(
            ServerLevel overworld,
            DungeonParty party,
            DungeonDefinition definition) {

        if (party.hasPendingInvites()) return null; // Still waiting on responses

        Set<ServerPlayer> players = new HashSet<>();
        for (UUID id : party.getAcceptedMembers()) {
            ServerPlayer sp = overworld.getServer().getPlayerList().getPlayer(id);
            if (sp != null) players.add(sp);
        }

        if (players.isEmpty()) return null;

        // Phase 7: Progression Check
        if (definition.requiredPreviousDungeon() != null && !definition.requiredPreviousDungeon().isBlank()) {
            net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData progressData = net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData.get(overworld);
            for (ServerPlayer sp : players) {
                if (!progressData.hasCompleted(sp.getUUID(), definition.requiredPreviousDungeon())) {
                    // Send message to the leader
                    ServerPlayer leader = overworld.getServer().getPlayerList().getPlayer(party.getLeaderId());
                    if (leader != null) {
                        leader.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("Player " + sp.getName().getString() + " has not completed the required dungeon: " + definition.requiredPreviousDungeon())
                                        .withStyle(net.minecraft.ChatFormatting.RED), false);
                    }
                    return null;
                }
            }
        }

        DungeonManager dungeonManager = DungeonManager.get(overworld);
        var instance = dungeonManager.startDungeon(overworld, definition, players);

        if (instance != null) {
            dissolveParty(party.getPartyId());
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    //  SavedData serialization
    // -------------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (DungeonParty party : parties.values()) {
            list.add(party.save(new CompoundTag()));
        }
        tag.put("Parties", list);
        return tag;
    }

    public static DungeonPartyManager load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        DungeonPartyManager manager = new DungeonPartyManager();
        if (tag.contains("Parties")) {
            ListTag list = tag.getList("Parties", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                DungeonParty party = DungeonParty.load(list.getCompound(i));
                manager.parties.put(party.getPartyId(), party);
                manager.playerToParty.put(party.getLeaderId(), party.getPartyId());
                for (UUID memberId : party.getMemberStatuses().keySet()) {
                    manager.playerToParty.put(memberId, party.getPartyId());
                }
            }
        }
        return manager;
    }
}
