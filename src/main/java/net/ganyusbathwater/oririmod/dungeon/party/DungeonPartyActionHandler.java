package net.ganyusbathwater.oririmod.dungeon.party;

import net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry;
import net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonScreenPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side handler for all DungeonActionPayload messages from the client.
 */
public class DungeonPartyActionHandler {

    public static void handle(ServerPlayer sender, DungeonActionPayload payload) {
        DungeonPartyManager manager = DungeonPartyManager.get(sender.serverLevel());
        DungeonParty party = manager.getParty(payload.partyId());

        if (party == null && !payload.action().equals("INVITE")) {
            // Party was dissolved; silently ignore stale actions
            return;
        }

        switch (payload.action()) {
            case "INVITE" -> handleInvite(sender, manager, payload.partyId(), payload.targetPlayerName());
            case "LEAVE"  -> handleLeave(sender, manager, party);
            case "ACCEPT" -> handleRespond(sender, manager, party, true);
            case "DECLINE"-> handleRespond(sender, manager, party, false);
            case "START"  -> handleStart(sender, manager, party);
        }
    }

    // ── INVITE ───────────────────────────────────────────────────────────────

    private static void handleInvite(ServerPlayer leader, DungeonPartyManager manager,
                                     UUID partyId, String targetName) {
        DungeonParty party = manager.getParty(partyId);
        if (party == null || !party.isLeader(leader.getUUID())) return;
        if (party.isFull()) {
            leader.displayClientMessage(
                    Component.literal("Party is full (max 4 players)!").withStyle(ChatFormatting.RED), true);
            return;
        }

        ServerPlayer target = leader.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            leader.displayClientMessage(
                    Component.literal("Player '" + targetName + "' is not online!").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (target.getUUID().equals(leader.getUUID())) return;

        manager.invitePlayer(party, target.getUUID());

        // Notify the invited player
        target.displayClientMessage(
                Component.literal(leader.getName().getString() + " invited you to a dungeon! Right-click the Dungeon Keeper NPC to accept or decline.")
                        .withStyle(ChatFormatting.YELLOW), false);

        // Refresh the leader's screen
        refreshScreen(leader, manager, party);
    }

    // ── LEAVE ────────────────────────────────────────────────────────────────

    private static void handleLeave(ServerPlayer sender, DungeonPartyManager manager, DungeonParty party) {
        boolean wasLeader = party.isLeader(sender.getUUID());
        if (wasLeader) {
            // Notify all members that the party was dissolved
            for (UUID memberId : party.getMemberStatuses().keySet()) {
                ServerPlayer member = sender.getServer().getPlayerList().getPlayer(memberId);
                if (member != null) {
                    member.displayClientMessage(
                            Component.literal("The party leader left. Party dissolved.").withStyle(ChatFormatting.RED), false);
                }
            }
            manager.dissolveParty(party.getPartyId());
        } else {
            manager.removePlayerFromParty(sender.getUUID());
            // Refresh leader's screen
            ServerPlayer leader = sender.getServer().getPlayerList().getPlayer(party.getLeaderId());
            if (leader != null) refreshScreen(leader, manager, party);
        }
    }

    // ── ACCEPT / DECLINE ─────────────────────────────────────────────────────

    private static void handleRespond(ServerPlayer sender, DungeonPartyManager manager,
                                      DungeonParty party, boolean accepted) {
        manager.respondToInvite(sender.getUUID(), accepted);

        // Notify leader
        ServerPlayer leader = sender.getServer().getPlayerList().getPlayer(party.getLeaderId());
        if (leader != null) {
            String msg = sender.getName().getString() + (accepted ? " accepted your invite!" : " declined your invite.");
            leader.displayClientMessage(
                    Component.literal(msg).withStyle(accepted ? ChatFormatting.GREEN : ChatFormatting.RED), false);
            refreshScreen(leader, manager, manager.getParty(party.getPartyId()));
        }
    }

    // ── START ────────────────────────────────────────────────────────────────

    private static void handleStart(ServerPlayer leader, DungeonPartyManager manager, DungeonParty party) {
        if (!party.isLeader(leader.getUUID())) return;

        if (party.hasPendingInvites()) {
            leader.displayClientMessage(
                    Component.literal("Not all members have responded yet!").withStyle(ChatFormatting.RED), true);
            return;
        }

        var definition = DungeonDefinitionRegistry.get(party.getDungeonId());
        if (definition == null) {
            leader.displayClientMessage(
                    Component.literal("Unknown dungeon: " + party.getDungeonId()).withStyle(ChatFormatting.RED), true);
            return;
        }

        var instance = manager.startDungeon(leader.serverLevel(), party, definition);
        if (instance == null) {
            leader.displayClientMessage(
                    Component.literal("Failed to start dungeon. Is the dimension loaded?").withStyle(ChatFormatting.RED), true);
        }
        // Instance started — players are already teleported by DungeonManager.startDungeon
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Sends a refreshed OpenDungeonScreenPayload to the leader's client. */
    private static void refreshScreen(ServerPlayer leader, DungeonPartyManager manager, DungeonParty party) {
        if (party == null) return;
        var def = DungeonDefinitionRegistry.get(party.getDungeonId());
        String displayName = def != null ? def.displayName() : party.getDungeonId();
        String description = def != null ? def.description() : "";

        List<UUID> memberIds = new ArrayList<>();
        List<String> memberNames = new ArrayList<>();
        List<String> memberStatuses = new ArrayList<>();
        for (var entry : party.getMemberStatuses().entrySet()) {
            memberIds.add(entry.getKey());
            var mp = leader.getServer().getPlayerList().getPlayer(entry.getKey());
            memberNames.add(mp != null ? mp.getName().getString() : "Unknown");
            memberStatuses.add(entry.getValue().name());
        }

        PacketDistributor.sendToPlayer(leader, new OpenDungeonScreenPayload(
                -1, party.getDungeonId(), displayName, description,
                party.getPartyId(), party.getLeaderId(),
                memberIds, memberNames, memberStatuses
        ));
    }
}
