package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DungeonDeathActionPayload(String action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DungeonDeathActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("oririmod", "dungeon_death_action"));

    public static final StreamCodec<ByteBuf, DungeonDeathActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> ByteBufCodecs.STRING_UTF8.encode(buf, payload.action()),
            buf -> new DungeonDeathActionPayload(ByteBufCodecs.STRING_UTF8.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
