package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.events.disaster.NaturalDisasterType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncNaturalDisasterPayload(NaturalDisasterType disasterType, int ticksRemaining, int totalDuration)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncNaturalDisasterPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_natural_disaster"));

    public static final StreamCodec<ByteBuf, SyncNaturalDisasterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> NaturalDisasterType.values()[Math.clamp(i, 0, NaturalDisasterType.values().length - 1)],
                    NaturalDisasterType::ordinal),
            SyncNaturalDisasterPayload::disasterType,
            ByteBufCodecs.VAR_INT,
            SyncNaturalDisasterPayload::ticksRemaining,
            ByteBufCodecs.VAR_INT,
            SyncNaturalDisasterPayload::totalDuration,
            SyncNaturalDisasterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

