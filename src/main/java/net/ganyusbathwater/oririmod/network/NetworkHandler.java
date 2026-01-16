package net.ganyusbathwater.oririmod.network;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPacket;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncWorldEventPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    public static final String VERSION = "1";
    public static final ResourceLocation MANA_SYNC =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "mana_sync");
    public static final ResourceLocation SYNC_WORLD_EVENT =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_world_event");

    private NetworkHandler() {}

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
                        if (max >= 0) ModManaUtil.setMaxManaClient(player, max);
                        ModManaUtil.setManaClient(player, pkt.getMana());
                    }
                })
        );

        registrar.playToClient(
                SyncWorldEventPayload.TYPE,
                SyncWorldEventPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    // Direkt auf die Payload-Felder zugreifen
                    WorldEventManager.updateEvent(payload.eventType(), payload.ticksRemaining(), payload.eventDuration());
                })
        );
    }

    public static void sendManaToPlayer(ServerPlayer player, int mana, int maxMana) {
        PacketDistributor.sendToPlayer(player, new ManaSyncPayload(new ManaSyncPacket(mana, maxMana)));
    }

    public static void sendWorldEventToAll(WorldEventType eventType, int ticksRemaining, int eventDuration) {
        // Die vereinfachte Payload direkt erstellen
        PacketDistributor.sendToAllPlayers(new SyncWorldEventPayload(eventType, ticksRemaining, eventDuration));
    }

    public static void sendWorldEventToPlayer(ServerPlayer player, WorldEventType eventType, int ticksRemaining, int eventDuration) {
        // Die vereinfachte Payload direkt erstellen
        PacketDistributor.sendToPlayer(player, new SyncWorldEventPayload(eventType, ticksRemaining, eventDuration));
    }
}