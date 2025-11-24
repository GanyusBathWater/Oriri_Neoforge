// java
package net.ganyusbathwater.oririmod.network;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.DuellantCortexManager;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.packet.DuellantCortexHostileSyncPayload;
import net.ganyusbathwater.oririmod.network.packet.ExtraInventorySyncPayload;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenExtraInventoryPacket;
import net.ganyusbathwater.oririmod.util.vestiges.ExtraInventoryUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    public static final String VERSION = "1";
    public static final ResourceLocation MANA_SYNC =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "mana_sync");

    private NetworkHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(OririMod.MOD_ID).versioned(VERSION);

        registrar.playToServer(
                OpenExtraInventoryPacket.TYPE,
                OpenExtraInventoryPacket.STREAM_CODEC,
                OpenExtraInventoryPacket::handle
        );

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
                ExtraInventorySyncPayload.TYPE,
                ExtraInventorySyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() != null) {
                        NonNullList<ItemStack> items = payload.items();
                        ExtraInventoryUtil.updateClientCache(items);
                    }
                })
        );

        // --- NEU: DuellantCortex HostileCount-Sync ---
        registrar.playToClient(
                DuellantCortexHostileSyncPayload.TYPE,
                DuellantCortexHostileSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    // nur LocalPlayer-Wert setzen
                    DuellantCortexManager.setClientHostileCount(payload.hostileCount());
                })
        );
    }

    public static void sendOpenExtraInventory() {
        PacketDistributor.sendToServer(new OpenExtraInventoryPacket());
    }

    public static void sendManaToPlayer(ServerPlayer player, int mana, int maxMana) {
        player.connection.send(new ClientboundCustomPayloadPacket(
                new ManaSyncPayload(new net.ganyusbathwater.oririmod.network.packet.ManaSyncPacket(mana, maxMana))));
    }

    public static void sendExtraInventoryTo(ServerPlayer player, NonNullList<ItemStack> items) {
        player.connection.send(new ClientboundCustomPayloadPacket(
                new ExtraInventorySyncPayload(items)));
    }

    // --- NEU: Helper für DuellantCortex-Sync ---
    public static void sendDuellantCortexHostileTo(ServerPlayer player, int hostileCount) {
        player.connection.send(new ClientboundCustomPayloadPacket(
                new DuellantCortexHostileSyncPayload(hostileCount)
        ));
    }
}