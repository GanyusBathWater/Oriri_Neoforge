package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent client → server when the player clicks "Leave" or "Cancel"
 * on the Homeward confirmation screen.
 *
 * @param confirmed {@code true} = player confirmed they want to leave the dungeon.
 */
public record HomewardConfirmPayload(boolean confirmed) implements CustomPacketPayload {

    public static final Type<HomewardConfirmPayload> TYPE =
            new Type<>(NetworkHandler.HOMEWARD_CONFIRM);

    public static final StreamCodec<FriendlyByteBuf, HomewardConfirmPayload> STREAM_CODEC =
            StreamCodec.of(
                    HomewardConfirmPayload::encode,
                    HomewardConfirmPayload::decode);

    private static void encode(FriendlyByteBuf buf, HomewardConfirmPayload payload) {
        buf.writeBoolean(payload.confirmed());
    }

    private static HomewardConfirmPayload decode(FriendlyByteBuf buf) {
        return new HomewardConfirmPayload(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
