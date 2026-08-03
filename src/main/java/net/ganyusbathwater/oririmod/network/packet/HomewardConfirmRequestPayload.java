package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent server → client when the player finishes charging the Homeward item
 * inside a dungeon dimension. The client opens a small yes/no dialog.
 */
public class HomewardConfirmRequestPayload implements CustomPacketPayload {

    public static final Type<HomewardConfirmRequestPayload> TYPE =
            new Type<>(NetworkHandler.HOMEWARD_CONFIRM_REQUEST);

    public static final StreamCodec<FriendlyByteBuf, HomewardConfirmRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> { /* no data */ },
                    buf -> new HomewardConfirmRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
