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
    public static final ResourceLocation BLIZZA_SPAWN_TITLE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "blizza_spawn_title");

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
}