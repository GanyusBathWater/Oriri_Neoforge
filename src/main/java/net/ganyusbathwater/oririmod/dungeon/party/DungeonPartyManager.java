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



    public void tickParties(ServerLevel overworld) {
        boolean dirty = false;
        for (DungeonParty party : parties.values()) {
            if (party.getStartTicksRemaining() < 0) {
                party.setStarting(false, party.getStartTicksRemaining() + 1);
                dirty = true;
            } else if (party.isStarting()) {
                party.decrementStartTicks();
                dirty = true;

                // Sync time to clients every 20 ticks
                if (party.getStartTicksRemaining() % 20 == 0) {
                    for (UUID memberId : party.getAcceptedMembers()) {
                        ServerPlayer sp = overworld.getServer().getPlayerList().getPlayer(memberId);
                        if (sp != null) {
                            net.ganyusbathwater.oririmod.dungeon.party.DungeonPartyActionHandler.refreshScreen(sp, this, party);
                        }
                    }
                }

                if (party.getStartTicksRemaining() <= 0) {
                    party.setStarting(false, 0);
                    DungeonDefinition def = net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry.get(party.getDungeonId());
                    
                    // Finalize generation: Build stages and teleport players
                    DungeonManager dungeonManager = DungeonManager.get(overworld);
                    net.ganyusbathwater.oririmod.dungeon.DungeonInstance instance = dungeonManager.getInstance(party.getAssignedInstanceId());
                    
                    if (def != null && instance != null) {
                        ServerLevel dimensionLevel = overworld.getServer().getLevel(def.dimension());
                        instance.setStarted(true);
                        
                        for (UUID pId : party.getAcceptedMembers()) {
                            ServerPlayer p = overworld.getServer().getPlayerList().getPlayer(pId);
                            if (p != null) {
                                net.ganyusbathwater.oririmod.dungeon.dimension.DungeonDimensionManager.teleportPlayerToDungeon(p, dimensionLevel, instance);
                            }
                        }
                        dissolveParty(party.getPartyId());
                    }
                }
            }
        }
        if (dirty) setDirty();
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
