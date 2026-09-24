package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenDungeonDeathScreenPayload(int livesRemaining) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenDungeonDeathScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("oririmod", "open_dungeon_death_screen"));

    public static final StreamCodec<ByteBuf, OpenDungeonDeathScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeInt(payload.livesRemaining()),
            buf -> new OpenDungeonDeathScreenPayload(buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
