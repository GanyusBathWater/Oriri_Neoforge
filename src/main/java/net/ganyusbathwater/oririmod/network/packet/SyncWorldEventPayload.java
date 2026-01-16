package net.ganyusbathwater.oririmod.network.packet;

import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncWorldEventPayload(WorldEventType eventType, int ticksRemaining, int eventDuration) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncWorldEventPayload> TYPE = new CustomPacketPayload.Type<>(NetworkHandler.SYNC_WORLD_EVENT);

    public static final StreamCodec<FriendlyByteBuf, SyncWorldEventPayload> STREAM_CODEC = StreamCodec.of(
            SyncWorldEventPayload::encode,
            SyncWorldEventPayload::decode
    );

    private static void encode(FriendlyByteBuf buf, SyncWorldEventPayload payload) {
        buf.writeEnum(payload.eventType());
        buf.writeVarInt(payload.ticksRemaining());
        buf.writeVarInt(payload.eventDuration());
    }

    private static SyncWorldEventPayload decode(FriendlyByteBuf buf) {
        WorldEventType eventType = buf.readEnum(WorldEventType.class);
        int ticksRemaining = buf.readVarInt();
        int eventDuration = buf.readVarInt();
        return new SyncWorldEventPayload(eventType, ticksRemaining, eventDuration);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}