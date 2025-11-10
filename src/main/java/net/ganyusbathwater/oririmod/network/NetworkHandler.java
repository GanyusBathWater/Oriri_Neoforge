package net.ganyusbathwater.oririmod.network;

import io.netty.buffer.Unpooled;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPacket;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenExtraInventoryPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkChannel;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public final class NetworkHandler {
    public static final String VERSION = "1";
    public static final ResourceLocation MANA_SYNC =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "mana_sync");

    private NetworkHandler() {}

    // Wird vom MOD-Eventbus via OririMod registriert
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
                        ManaSyncPacket pkt = payload.getPacket();
                        int max = pkt.getMaxMana();
                        if (max >= 0) ModManaUtil.setMaxManaClient(player, max);
                        ModManaUtil.setManaClient(player, pkt.getMana());
                    }
                })
        );
    }

    public static void sendOpenExtraInventory() {
        PacketDistributor.sendToServer(new OpenExtraInventoryPacket());
    }

    public static void sendManaToPlayer(ServerPlayer player, int mana, int maxMana) {
        player.connection.send(new ClientboundCustomPayloadPacket(
                new ManaSyncPayload(new ManaSyncPacket(mana, maxMana))));
    }
}