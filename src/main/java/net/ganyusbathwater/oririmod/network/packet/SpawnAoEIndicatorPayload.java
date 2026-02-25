package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class SpawnAoEIndicatorPayload implements CustomPacketPayload {

    public static final Type<SpawnAoEIndicatorPayload> TYPE = new Type<>(NetworkHandler.SPAWN_AOE_INDICATOR);

    public static final StreamCodec<FriendlyByteBuf, SpawnAoEIndicatorPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> SpawnAoEIndicatorPacket.encode(payload.pkt, buf),
            SpawnAoEIndicatorPayload::read);

    private final SpawnAoEIndicatorPacket pkt;

    public SpawnAoEIndicatorPayload(SpawnAoEIndicatorPacket pkt) {
        this.pkt = pkt;
    }

    public static SpawnAoEIndicatorPayload read(FriendlyByteBuf buf) {
        return new SpawnAoEIndicatorPayload(SpawnAoEIndicatorPacket.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public SpawnAoEIndicatorPacket getPacket() {
        return pkt;
    }
}
