package net.ganyusbathwater.oririmod.dungeon.party;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Holds all state for one pending or active dungeon party.
 * Stored in-memory in DungeonPartyManager (per server).
 *
 * A party has one leader (initiator) and up to 3 other members.
 * Members that are in PENDING state must accept before the dungeon can start.
 */
public class DungeonParty {

    public enum MemberStatus { PENDING, ACCEPTED, DECLINED }

    private final UUID partyId;
    private final UUID leaderId;
    private final String dungeonId; // Which dungeon keeper NPC triggered this
    private final Map<UUID, MemberStatus> members = new LinkedHashMap<>(); // excludes leader

    public DungeonParty(UUID leaderId, String dungeonId) {
        this.partyId = UUID.randomUUID();
        this.leaderId = leaderId;
        this.dungeonId = dungeonId;
        // Leader is auto-accepted
    }

    public UUID getPartyId() { return partyId; }
    public UUID getLeaderId() { return leaderId; }
    public String getDungeonId() { return dungeonId; }

    public void invitePlayer(UUID playerId) {
        if (!playerId.equals(leaderId)) {
            members.put(playerId, MemberStatus.PENDING);
        }
    }

    public void respondInvite(UUID playerId, boolean accepted) {
        if (members.containsKey(playerId)) {
            members.put(playerId, accepted ? MemberStatus.ACCEPTED : MemberStatus.DECLINED);
        }
    }

    public void removePlayer(UUID playerId) {
        members.remove(playerId);
    }

    public boolean isLeader(UUID playerId) { return leaderId.equals(playerId); }
    public boolean hasPendingInvites() { return members.containsValue(MemberStatus.PENDING); }

    /** All accepted members + the leader */
    public Set<UUID> getAcceptedMembers() {
        Set<UUID> result = new LinkedHashSet<>();
        result.add(leaderId);
        for (Map.Entry<UUID, MemberStatus> e : members.entrySet()) {
            if (e.getValue() == MemberStatus.ACCEPTED) result.add(e.getKey());
        }
        return result;
    }

    /** All members (invited, accepted, or declined), excluding leader */
    public Map<UUID, MemberStatus> getMemberStatuses() {
        return Collections.unmodifiableMap(members);
    }

    public boolean isFull() { return members.size() >= 3; }

    @Nullable
    public MemberStatus getStatus(UUID playerId) {
        return members.get(playerId);
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("PartyId", partyId);
        tag.putUUID("LeaderId", leaderId);
        tag.putString("DungeonId", dungeonId);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, MemberStatus> e : members.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", e.getKey());
            entry.putString("Status", e.getValue().name());
            list.add(entry);
        }
        tag.put("Members", list);
        return tag;
    }

    public static DungeonParty load(CompoundTag tag) {
        UUID leaderId = tag.getUUID("LeaderId");
        String dungeonId = tag.getString("DungeonId");
        DungeonParty party = new DungeonParty(leaderId, dungeonId);
        ListTag list = tag.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            UUID playerId = entry.getUUID("Player");
            MemberStatus status = MemberStatus.valueOf(entry.getString("Status"));
            party.members.put(playerId, status);
        }
        return party;
    }
}
