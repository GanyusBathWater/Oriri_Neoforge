package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenClockerScreenPayload(BlockPos pos, int mode, int delay, int timeOn, int timeOff, boolean useSeconds) implements CustomPacketPayload {
    public static final Type<OpenClockerScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "open_clocker_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenClockerScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeInt(payload.mode());
                buf.writeInt(payload.delay());
                buf.writeInt(payload.timeOn());
                buf.writeInt(payload.timeOff());
                buf.writeBoolean(payload.useSeconds());
            },
            buf -> new OpenClockerScreenPayload(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
