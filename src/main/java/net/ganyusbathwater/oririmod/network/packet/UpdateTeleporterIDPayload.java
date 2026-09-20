package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateTeleporterIDPayload(
        BlockPos pos,
        String id
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpdateTeleporterIDPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("oririmod", "update_teleporter_id"));

    public static final StreamCodec<ByteBuf, UpdateTeleporterIDPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.pos().getX());
                buf.writeInt(payload.pos().getY());
                buf.writeInt(payload.pos().getZ());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.id());
            },
            buf -> new UpdateTeleporterIDPayload(
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                    ByteBufCodecs.STRING_UTF8.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
