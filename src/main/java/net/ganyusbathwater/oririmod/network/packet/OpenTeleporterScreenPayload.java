package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTeleporterScreenPayload(
        BlockPos pos,
        String currentId,
        boolean originObstructed,
        boolean destObstructed
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenTeleporterScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("oririmod", "open_teleporter_screen"));

    public static final StreamCodec<ByteBuf, OpenTeleporterScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.pos().getX());
                buf.writeInt(payload.pos().getY());
                buf.writeInt(payload.pos().getZ());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.currentId());
                buf.writeBoolean(payload.originObstructed());
                buf.writeBoolean(payload.destObstructed());
            },
            buf -> new OpenTeleporterScreenPayload(
                    new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    buf.readBoolean(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
