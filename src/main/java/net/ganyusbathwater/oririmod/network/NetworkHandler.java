package net.ganyusbathwater.oririmod.network;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPacket;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPayload;
import net.ganyusbathwater.oririmod.network.packet.SelectBossAttackPayload;
import net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPayload;
import net.ganyusbathwater.oririmod.item.custom.magic.BossAttackDebugWandItem;
import net.minecraft.network.chat.Component;
import net.ganyusbathwater.oririmod.network.packet.SyncWorldEventPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    public static final String VERSION = "1";
    public static final ResourceLocation MANA_SYNC = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "mana_sync");
    public static final ResourceLocation SYNC_WORLD_EVENT = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "sync_world_event");
    public static final ResourceLocation SPAWN_AOE_INDICATOR = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "spawn_aoe_indicator");
    public static final ResourceLocation SELECT_BOSS_ATTACK = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "select_boss_attack");
    public static final ResourceLocation SELECT_PARTICLE_EFFECT = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "select_particle_effect");
    public static final ResourceLocation BLIZZA_SPAWN_TITLE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "blizza_spawn_title");
    public static final ResourceLocation DEVIARTRAS_SPAWN_TITLE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "deviartras_spawn_title");
    public static final ResourceLocation HOMEWARD_CONFIRM_REQUEST = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "homeward_confirm_request");
    public static final ResourceLocation HOMEWARD_CONFIRM = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "homeward_confirm");
    public static final ResourceLocation OPEN_DUNGEON_SCREEN = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "open_dungeon_screen");
    public static final ResourceLocation DUNGEON_ACTION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "dungeon_action");
    public static final ResourceLocation PLAY_DUNGEON_MUSIC = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "play_dungeon_music");
    public static final ResourceLocation OPEN_MARKER_SCREEN = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "open_marker_screen");
    public static final ResourceLocation SYNC_MARKER_DATA = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "sync_marker_data");
    public static final ResourceLocation OPEN_CONVERSATION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "open_conversation");
    public static final ResourceLocation CONVERSATION_ACTION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "conversation_action");
    public static final ResourceLocation UPDATE_TELEPORTER_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "update_teleporter_id");
    public static final ResourceLocation OPEN_TELEPORTER_SCREEN = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "open_teleporter_screen");
    public static final ResourceLocation SYNC_DIALOGUES = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "sync_dialogues");
    public static final ResourceLocation OPEN_DUNGEON_DEATH_SCREEN = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "open_dungeon_death_screen");
    public static final ResourceLocation DUNGEON_DEATH_ACTION = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "dungeon_death_action");
    public static final ResourceLocation SYNC_DUNGEON_TIME = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "sync_dungeon_time");

    private NetworkHandler() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(OririMod.MOD_ID).versioned(VERSION);

        registrar.playToClient(
                ManaSyncPayload.TYPE,
                ManaSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    if (player != null) {
                        var pkt = payload.getPacket();
                        int max = pkt.getMaxMana();
                        if (max >= 0)
                            ModManaUtil.setMaxManaClient(player, max);
                        ModManaUtil.setManaClient(player, pkt.getMana());
                    }
                }));

        registrar.playToClient(
                SyncWorldEventPayload.TYPE,
                SyncWorldEventPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    // Direkt auf die Payload-Felder zugreifen
                    WorldEventManager.updateClientEvent(payload.eventType(), payload.ticksRemaining(),
                            payload.eventDuration());
                }));

        registrar.playToClient(
                SpawnAoEIndicatorPayload.TYPE,
                SpawnAoEIndicatorPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var pkt = payload.getPacket();
                    net.ganyusbathwater.oririmod.client.render.AoEIndicatorClientState.addIndicator(
                            pkt.getCenter(), pkt.getRadius(), pkt.getDurationTicks(), pkt.getArgbColor());
                }));

        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.BlizzaSpawnTitlePayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.BlizzaSpawnTitlePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.ganyusbathwater.oririmod.events.ClientEvents.triggerBlizzaTitle();
                }));

        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.DeviartrasSpawnTitlePayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.DeviartrasSpawnTitlePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.ganyusbathwater.oririmod.events.ClientEvents.triggerDeviartrasTitle();
                }));

        // Homeward: server → client open confirm screen
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.HomewardConfirmRequestPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.HomewardConfirmRequestPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.minecraft.client.Minecraft.getInstance()
                            .setScreen(new net.ganyusbathwater.oririmod.client.screen.HomewardConfirmScreen());
                }));

        // Homeward: client → server confirm or cancel
        registrar.playToServer(
                net.ganyusbathwater.oririmod.network.packet.HomewardConfirmPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.HomewardConfirmPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp && payload.confirmed()) {
                        net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(sp);
                    }
                }));

        registrar.playToServer(
                SelectBossAttackPayload.TYPE,
                SelectBossAttackPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    if (player != null) {
                        net.minecraft.world.InteractionHand foundHand = null;
                        if (player.getMainHandItem().getItem() instanceof BossAttackDebugWandItem) {
                            foundHand = net.minecraft.world.InteractionHand.MAIN_HAND;
                        } else if (player.getOffhandItem().getItem() instanceof BossAttackDebugWandItem) {
                            foundHand = net.minecraft.world.InteractionHand.OFF_HAND;
                        }

                        if (foundHand != null) {
                            var stack = player.getItemInHand(foundHand);
                            BossAttackDebugWandItem.setSelected(stack, payload.attackType());
                            player.displayClientMessage(Component.literal("Selected Attack: " + BossAttackDebugWandItem.prettyName(payload.attackType()))
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                        }
                    }
                }));

        registrar.playToServer(
                net.ganyusbathwater.oririmod.network.packet.ElementalChoirSweepPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.ElementalChoirSweepPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    if (player != null && player.getMainHandItem().getItem() instanceof net.ganyusbathwater.oririmod.item.custom.ElementalChoirItem item) {
                        item.executeServerSwing(player, player.getMainHandItem());
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new net.ganyusbathwater.oririmod.network.packet.SyncChoirSwingPayload(player.getId()));
                    }
                }));

        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.SyncChoirSwingPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.SyncChoirSwingPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    if (player != null && player.level().isClientSide) {
                        net.minecraft.world.entity.Entity entity = player.level().getEntity(payload.playerId());
                        if (entity instanceof net.minecraft.world.entity.player.Player swingingPlayer) {
                            net.ganyusbathwater.oririmod.item.custom.ElementalChoirItem.CLIENT_LAST_SWING_TICK.put(swingingPlayer, swingingPlayer.level().getGameTime());
                        }
                    }
                }));

        // Dungeon Keeper: server → client open dungeon screen
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.OpenDungeonScreenPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.OpenDungeonScreenPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        net.minecraft.client.Minecraft.getInstance()
                                .setScreen(new net.ganyusbathwater.oririmod.client.screen.DungeonKeeperScreen(payload))
                ));

        // Dungeon Selection: server → client open selection screen
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.OpenDungeonSelectionPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.OpenDungeonSelectionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        net.minecraft.client.Minecraft.getInstance()
                                .setScreen(new net.ganyusbathwater.oririmod.client.screen.DungeonSelectionScreen(payload))
                ));

        // Dungeon Keeper: client → server party actions
        registrar.playToServer(
                net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    net.ganyusbathwater.oririmod.dungeon.party.DungeonPartyActionHandler.handle(sp, payload);
                }));

        // Dungeon Music: server → client
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.PlayDungeonMusicPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.PlayDungeonMusicPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        net.ganyusbathwater.oririmod.client.DungeonMusicHandler.handle(payload)
                ));

        // Dungeon Run Timer & Objective: server → client
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.SyncDungeonTimePayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.SyncDungeonTimePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.ganyusbathwater.oririmod.client.render.DungeonHUDOverlay.updateTime(payload);
                    net.ganyusbathwater.oririmod.client.render.DungeonHUDOverlay.updateObjective(payload);
                }));

        // Marker GUI: server -> client
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.OpenMarkerScreenPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.OpenMarkerScreenPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        net.minecraft.client.Minecraft.getInstance()
                                .setScreen(new net.ganyusbathwater.oririmod.client.screen.DungeonMarkerScreen(payload))
                ));

        // Marker GUI: client -> server
        registrar.playToServer(
                net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        net.minecraft.world.entity.Entity entity = sp.serverLevel().getEntity(payload.entityId());
                        if (entity instanceof net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity marker) {
                            marker.setStageId(payload.stageId());
                            marker.setStageType(payload.stageType());
                            marker.setRole(payload.role());
                            
                            net.minecraft.nbt.CompoundTag extra = marker.getExtraData();
                            extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_ENEMY_TYPE, payload.enemyType());
                            extra.putInt(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_COUNT, payload.count());
                            extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_SWITCH_ID, payload.switchId());
                            extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_LOOT_TABLE, payload.lootTable());
                            extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_BOSS_ID, payload.bossId());
                            extra.putFloat(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_SPAWN_CHANCE, payload.spawnChance());
                            extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_TRIGGER_BEHAVIOR, payload.triggerBehavior());
                            extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_OBJECTIVE, payload.objectiveText());

                            // Broadcast objective text to ALL markers in the same stage group
                            java.util.List<net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity> sameStageMarkers = sp.serverLevel().getEntitiesOfClass(
                                net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.class, 
                                marker.getBoundingBox().inflate(128.0), 
                                m -> payload.stageId().equals(m.getStageId())
                            );
                            for (var m : sameStageMarkers) {
                                m.getExtraData().putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_OBJECTIVE, payload.objectiveText());
                            }
                            
                            net.ganyusbathwater.oririmod.item.custom.DungeonMarkerItem.LAST_CONFIG.put(sp.getUUID(), payload);
                            
                                sp.displayClientMessage(net.minecraft.network.chat.Component.literal("§aMarker configuration saved!"), true);
                        }
                    }
                }));

        // Sync Dungeon Lives: server -> client
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.ganyusbathwater.oririmod.events.ClientEvents.updateDungeonLives(payload.lives());
                }));

        // Conversation: server → client open dialogue screen
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.OpenConversationPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.OpenConversationPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.ganyusbathwater.oririmod.dialogue.DialogueTree tree =
                            net.ganyusbathwater.oririmod.dialogue.DialogueRegistry.get(payload.treeId());
                    if (tree != null) {
                        net.minecraft.client.Minecraft.getInstance()
                                .setScreen(new net.ganyusbathwater.oririmod.client.screen.ConversationScreen(
                                        tree, payload.entityId()));
                    } else {
                        OririMod.LOGGER.warn("[Dialogue] Client tried to open unknown dialogue tree: {}", payload.treeId());
                    }
                }));
                
        // Sync Dialogues: server -> client
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.SyncDialoguesPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.SyncDialoguesPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.ganyusbathwater.oririmod.dialogue.DialogueRegistry.loadFromJsonSync(payload.dialoguesJson());
                }));

        // Conversation: client → server dialogue action
        registrar.playToServer(
                net.ganyusbathwater.oririmod.network.packet.ConversationActionPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.ConversationActionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    net.minecraft.world.entity.Entity entity = sp.serverLevel().getEntity(payload.entityId());

                    // Validate entity exists, is conversable, and is within 10 blocks
                    if (entity == null || !(entity instanceof net.ganyusbathwater.oririmod.dialogue.ConversableEntity)) {
                        return;
                    }
                    if (sp.distanceTo(entity) > 10.0) {
                        return;
                    }

                    handleConversationAction(sp, entity, payload.action());
                }));

        // Teleporter ID Update
        registrar.playToServer(
                net.ganyusbathwater.oririmod.network.packet.UpdateTeleporterIDPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.UpdateTeleporterIDPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    net.minecraft.world.level.block.entity.BlockEntity be = sp.level().getBlockEntity(payload.pos());
                    if (be instanceof net.ganyusbathwater.oririmod.block.entity.TeleporterBlockEntity teleporter) {
                        // Check if in dungeon
                        boolean inDungeon = sp.level().dimension().location().getPath().startsWith("dungeon_");
                        if (inDungeon && !sp.isCreative()) {
                            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.oririmod.teleporter.no_modify_dungeon").withStyle(net.minecraft.ChatFormatting.RED), true);
                            return;
                        }
                        
                        // Check distance
                        if (sp.blockPosition().distSqr(payload.pos()) > 64) {
                            return;
                        }
                        
                        teleporter.setTeleporterId(payload.id(), sp);
                    }
                }));

        // Open Teleporter Screen (server -> client)
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.OpenTeleporterScreenPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.OpenTeleporterScreenPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.minecraft.client.Minecraft.getInstance()
                            .setScreen(new net.ganyusbathwater.oririmod.client.screen.TeleporterScreen(payload));
                }));

        // Dungeon Death Screen (server -> client)
        registrar.playToClient(
                net.ganyusbathwater.oririmod.network.packet.OpenDungeonDeathScreenPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.OpenDungeonDeathScreenPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    net.minecraft.client.Minecraft.getInstance()
                            .setScreen(new net.ganyusbathwater.oririmod.client.screen.OririDungeonDeathScreen(payload.livesRemaining()));
                }));

        // Dungeon Death Action (client -> server)
        registrar.playToServer(
                net.ganyusbathwater.oririmod.network.packet.DungeonDeathActionPayload.TYPE,
                net.ganyusbathwater.oririmod.network.packet.DungeonDeathActionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                    net.ganyusbathwater.oririmod.dungeon.event.DungeonEventHandler.handleDeathAction(sp, payload.action());
                }));
    }
    

    public static void sendManaToPlayer(ServerPlayer player, int mana, int maxMana) {
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(new ManaSyncPacket(mana, maxMana)));
    }

    public static void sendWorldEventToAll(WorldEventType eventType, int ticksRemaining, int eventDuration) {
        // Die vereinfachte Payload direkt erstellen
        PacketDistributor.sendToAllPlayers(new SyncWorldEventPayload(eventType, ticksRemaining, eventDuration));
    }

    public static void sendWorldEventToDimension(net.minecraft.server.level.ServerLevel level, WorldEventType eventType,
            int ticksRemaining, int eventDuration) {
        PacketDistributor.sendToPlayersInDimension(level,
                new SyncWorldEventPayload(eventType, ticksRemaining, eventDuration));
    }

    public static void sendWorldEventToPlayer(ServerPlayer player, WorldEventType eventType, int ticksRemaining,
            int eventDuration) {
        // Die vereinfachte Payload direkt erstellen
        PacketDistributor.sendToPlayer(player, new SyncWorldEventPayload(eventType, ticksRemaining, eventDuration));
    }

    public static void sendAoEIndicatorToPlayersAround(net.minecraft.server.level.ServerLevel level, BlockPos center,
            float radius, int durationTicks, int argbColor) {
        net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPacket pkt = new net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPacket(
                center, radius, durationTicks, argbColor);
        PacketDistributor.sendToPlayersNear(
                level, null, center.getX(), center.getY(), center.getZ(), 128.0D,
                new SpawnAoEIndicatorPayload(pkt));
    }

    public static void sendBlizzaTitleToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new net.ganyusbathwater.oririmod.network.packet.BlizzaSpawnTitlePayload());
    }

    public static void sendDeviartrasTitle(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new net.ganyusbathwater.oririmod.network.packet.DeviartrasSpawnTitlePayload());
    }

    public static void sendHomewardConfirmRequest(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new net.ganyusbathwater.oririmod.network.packet.HomewardConfirmRequestPayload());
    }

    public static void sendOpenTeleporterScreen(ServerPlayer player, BlockPos pos, String currentId) {
        boolean originObstructed = !net.ganyusbathwater.oririmod.block.custom.TeleporterBlock.isClear(player.level(), pos);
        boolean destObstructed = false;
        if (currentId != null && !currentId.isEmpty()) {
            net.ganyusbathwater.oririmod.world.data.TeleporterSavedData data = net.ganyusbathwater.oririmod.world.data.TeleporterSavedData.get(player.serverLevel());
            BlockPos dest = data.getDestination(currentId, pos);
            if (dest != null) {
                destObstructed = !net.ganyusbathwater.oririmod.block.custom.TeleporterBlock.isClear(player.level(), dest);
            }
        }
        PacketDistributor.sendToPlayer(player,
                new net.ganyusbathwater.oririmod.network.packet.OpenTeleporterScreenPayload(pos, currentId, originObstructed, destObstructed));
    }

    /**
     * Send a conversation dialogue tree to a player.
     */
    public static void sendConversation(ServerPlayer player, int entityId, net.ganyusbathwater.oririmod.dialogue.DialogueTree tree) {
        PacketDistributor.sendToPlayer(player,
                new net.ganyusbathwater.oririmod.network.packet.OpenConversationPayload(entityId, tree.getId()));
    }

    /**
     * Handle a conversation action sent from the client.
     * Dispatches to the appropriate entity-specific handler.
     */
    private static void handleConversationAction(ServerPlayer sp, net.minecraft.world.entity.Entity entity, String actionStr) {
        if ("CLOSE".equals(actionStr)) {
            // Nothing to do server-side, client already closed the screen
            return;
        }
        
        net.ganyusbathwater.oririmod.dialogue.DialogueAction action = net.ganyusbathwater.oririmod.dialogue.DialogueRegistry.getAction(actionStr);
        if (action == null) {
            OririMod.LOGGER.warn("[Dialogue] Unknown action '{}' from player {}", actionStr, sp.getName().getString());
            return;
        }

        if (entity instanceof net.ganyusbathwater.oririmod.dialogue.ConversableEntity conversableEntity) {
            action.execute(sp, conversableEntity);
        }
    }
}