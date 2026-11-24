package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> Client: opens the Wish Upon a Star attribute selection screen
 * for a specific dungeon's Wish item.
 */
public record OpenWishScreenPayload(
        String dungeonId,
        String dungeonDisplayName,
        String currentAttributeId,
        boolean offhand
) implements CustomPacketPayload {

    public static final Type<OpenWishScreenPayload> TYPE = new Type<>(NetworkHandler.OPEN_WISH_SCREEN);

    public static final StreamCodec<ByteBuf, OpenWishScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenWishScreenPayload::dungeonId,
            ByteBufCodecs.STRING_UTF8, OpenWishScreenPayload::dungeonDisplayName,
            ByteBufCodecs.STRING_UTF8, OpenWishScreenPayload::currentAttributeId,
            ByteBufCodecs.BOOL, OpenWishScreenPayload::offhand,
            OpenWishScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
