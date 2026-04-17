package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent server → client when Blizza spawns.
 * The client uses this to start the spawn-title overlay timer.
 */
public class BlizzaSpawnTitlePayload implements CustomPacketPayload {

    public static final Type<BlizzaSpawnTitlePayload> TYPE =
            new Type<>(NetworkHandler.BLIZZA_SPAWN_TITLE);

    public static final StreamCodec<FriendlyByteBuf, BlizzaSpawnTitlePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> { /* no data */ },
                    buf -> new BlizzaSpawnTitlePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
