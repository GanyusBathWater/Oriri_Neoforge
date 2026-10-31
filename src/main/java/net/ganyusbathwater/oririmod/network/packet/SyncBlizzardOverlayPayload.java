package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncBlizzardOverlayPayload(boolean exposed, boolean sheltered) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncBlizzardOverlayPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_blizzard_overlay"));

    public static final StreamCodec<ByteBuf, SyncBlizzardOverlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SyncBlizzardOverlayPayload::exposed,
            ByteBufCodecs.BOOL,
            SyncBlizzardOverlayPayload::sheltered,
            SyncBlizzardOverlayPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

