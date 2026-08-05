package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncMarkerDataPayload(
        int entityId,
        String stageId,
        String stageType,
        String role,
        String enemyType,
        int count,
        String switchId,
        String lootTable,
        String bossId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMarkerDataPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("oririmod", "sync_marker_data"));

    public static final StreamCodec<ByteBuf, SyncMarkerDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entityId());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.stageId());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.stageType());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.role());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.enemyType());
                buf.writeInt(payload.count());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.switchId());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.lootTable());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.bossId());
            },
            buf -> new SyncMarkerDataPayload(
                    buf.readInt(),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    buf.readInt(),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
