package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent server → client when Deviartras spawns.
 * The client uses this to start the spawn-title overlay timer.
 */
public class DeviartrasSpawnTitlePayload implements CustomPacketPayload {

    public static final Type<DeviartrasSpawnTitlePayload> TYPE =
            new Type<>(NetworkHandler.DEVIARTRAS_SPAWN_TITLE);

    public static final StreamCodec<FriendlyByteBuf, DeviartrasSpawnTitlePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> { /* no data */ },
                    buf -> new DeviartrasSpawnTitlePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
