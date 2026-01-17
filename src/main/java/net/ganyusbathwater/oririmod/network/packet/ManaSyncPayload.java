package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ManaSyncPayload implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE = new Type<>(NetworkHandler.MANA_SYNC);

    // Codec für (De)Serialisierung in 1.21.1
    public static final StreamCodec<FriendlyByteBuf, ManaSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> ManaSyncPacket.encode(payload.pkt, buf),
                    ManaSyncPayload::read
            );

    private final ManaSyncPacket pkt;

    public ManaSyncPayload(ManaSyncPacket pkt) {
        this.pkt = pkt;
    }

    public static ManaSyncPayload read(FriendlyByteBuf buf) {
        return new ManaSyncPayload(ManaSyncPacket.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ManaSyncPacket getPacket() {
        return pkt;
    }
}