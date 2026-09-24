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
import java.util.Set;

/**
 * Server-side handler for all DungeonActionPayload messages from the client.
 */
public class DungeonPartyActionHandler {

    public static void handle(ServerPlayer sender, DungeonActionPayload payload) {
        DungeonPartyManager manager = DungeonPartyManager.get(sender.serverLevel());
        DungeonParty party = manager.getParty(payload.partyId());

        if (party == null && !payload.action().equals("INVITE") && !payload.action().equals("SELECT")) {
            // Party was dissolved; silently ignore stale actions
            return;
        }

        switch (payload.action()) {
            case "INVITE" -> handleInvite(sender, manager, payload.partyId(), payload.targetPlayerName());
            case "LEAVE"  -> handleLeave(sender, manager, party);
            case "ACCEPT" -> handleRespond(sender, manager, party, true);
            case "DECLINE"-> handleRespond(sender, manager, party, false);
            case "START"  -> handleStart(sender, manager, party);
            case "CANCEL_START" -> handleCancelStart(sender, manager, party);
            case "SELECT" -> handleSelect(sender, manager, payload.targetPlayerName());
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
        if (party.isStarting()) return;

        if (party.hasPendingInvites()) {
            leader.displayClientMessage(
                    Component.literal("Not all members have responded yet!").withStyle(ChatFormatting.RED), true);
            return;
        }

        var definition = net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry.get(party.getDungeonId());
        if (definition == null) {
            leader.displayClientMessage(
                    Component.literal("Unknown dungeon: " + party.getDungeonId()).withStyle(ChatFormatting.RED), true);
            return;
        }
        
        net.ganyusbathwater.oririmod.dungeon.DungeonManager dungeonManager = net.ganyusbathwater.oririmod.dungeon.DungeonManager.get(leader.serverLevel());

        // Check if we have a cached generated instance from a previous cancelled start
        if (party.getAssignedInstanceId() == null) {
            Set<ServerPlayer> players = new java.util.HashSet<>();
            for (UUID id : party.getAcceptedMembers()) {
                ServerPlayer sp = leader.getServer().getPlayerList().getPlayer(id);
                if (sp != null) players.add(sp);
            }
            
            // Progression Check
            if (definition.requiredPreviousDungeon() != null && !definition.requiredPreviousDungeon().isBlank()) {
                net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData progressData = net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData.get(leader.serverLevel());
                for (ServerPlayer sp : players) {
                    if (!progressData.hasCompleted(sp.getUUID(), definition.requiredPreviousDungeon())) {
                        leader.displayClientMessage(
                                Component.literal("Player " + sp.getName().getString() + " has not completed the required dungeon: " + definition.requiredPreviousDungeon())
                                        .withStyle(ChatFormatting.RED), false);
                        return;
                    }
                }
            }

            var instance = dungeonManager.allocateDungeon(leader.serverLevel(), definition, players);
            if (instance == null) {
                leader.displayClientMessage(
                        Component.literal("Failed to allocate dungeon grid.").withStyle(ChatFormatting.RED), true);
                return;
            }
            party.setAssignedInstanceId(instance.getInstanceId());
            
            // Queue background generation
            dungeonManager.getActiveTasks().add(new net.ganyusbathwater.oririmod.dungeon.dimension.DungeonGeneratorTask(
                    leader.serverLevel().getServer().getLevel(definition.dimension()),
                    definition, instance
            ));
        }

        // 10-second countdown (200 ticks)
        party.setStarting(true, 200);
        manager.setDirty();
        
        // Refresh screens for all members
        for (UUID memberId : party.getAcceptedMembers()) {
            ServerPlayer sp = leader.getServer().getPlayerList().getPlayer(memberId);
            if (sp != null) refreshScreen(sp, manager, party);
        }
    }

    // ── CANCEL_START ──────────────────────────────────────────────────────────
    
    private static void handleCancelStart(ServerPlayer sender, DungeonPartyManager manager, DungeonParty party) {
        if (party.isStarting()) {
            party.setStarting(false, -200); // 10s cooldown
            manager.setDirty();
            
            sender.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(sender.getName().getString() + " cancelled the dungeon start.").withStyle(ChatFormatting.YELLOW), false);
            
            // Refresh screens for all members
            for (UUID memberId : party.getAcceptedMembers()) {
                ServerPlayer sp = sender.getServer().getPlayerList().getPlayer(memberId);
                if (sp != null) refreshScreen(sp, manager, party);
            }
            
            // Cancel generator task if running
            if (party.getAssignedInstanceId() != null) {
                net.ganyusbathwater.oririmod.dungeon.DungeonManager dungeonManager = net.ganyusbathwater.oririmod.dungeon.DungeonManager.get(sender.serverLevel());
                for (var task : dungeonManager.getActiveTasks()) {
                    if (task.getInstance().getInstanceId().equals(party.getAssignedInstanceId())) {
                        task.cancel();
                        break;
                    }
                }
            }
        }
    }

    // ── SELECT ───────────────────────────────────────────────────────────────

    private static void handleSelect(ServerPlayer sender, DungeonPartyManager manager, String dungeonId) {
        // Create party and open screen
        DungeonParty newParty = manager.createParty(sender.getUUID(), dungeonId);
        refreshScreen(sender, manager, newParty);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Sends a refreshed OpenDungeonScreenPayload to the leader's client. */
    public static void refreshScreen(ServerPlayer leader, DungeonPartyManager manager, DungeonParty party) {
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
                memberIds, memberNames, memberStatuses,
                party.isStarting(), party.getStartTicksRemaining()
        ));
    }
}
