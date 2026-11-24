package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> Server: confirms the player's chosen permanent attribute from
 * the Wish Upon a Star screen.
 */
public record SelectWishAttributePayload(
        String dungeonId,
        String chosenAttributeId,
        boolean offhand
) implements CustomPacketPayload {

    public static final Type<SelectWishAttributePayload> TYPE = new Type<>(NetworkHandler.SELECT_WISH_ATTRIBUTE);

    public static final StreamCodec<ByteBuf, SelectWishAttributePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectWishAttributePayload::dungeonId,
            ByteBufCodecs.STRING_UTF8, SelectWishAttributePayload::chosenAttributeId,
            ByteBufCodecs.BOOL, SelectWishAttributePayload::offhand,
            SelectWishAttributePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
