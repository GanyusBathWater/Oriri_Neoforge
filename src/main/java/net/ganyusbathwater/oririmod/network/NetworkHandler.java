// java
package net.ganyusbathwater.oririmod.network;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.packet.ManaSyncPayload;
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

    }

    public static void sendManaToPlayer(ServerPlayer player, int mana, int maxMana) {
        player.connection.send(new ClientboundCustomPayloadPacket(
                new ManaSyncPayload(new net.ganyusbathwater.oririmod.network.packet.ManaSyncPacket(mana, maxMana))));
    }
}