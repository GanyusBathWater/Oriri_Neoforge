package net.ganyusbathwater.oririmod.network.packet;

import io.netty.buffer.ByteBuf;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.world.fog.FogAnchor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> Client payload syncing all chunk-attached structure {@link FogAnchor} emitters
 * for a single chunk (sent on chunk watch and whenever an anchor is added or demolished).
 */
public record SyncChunkFogAnchorsPayload(
        int chunkX,
        int chunkZ,
        List<FogAnchor.ClientView> anchors) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sync_chunk_fog_anchors");

    public static final Type<SyncChunkFogAnchorsPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, SyncChunkFogAnchorsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncChunkFogAnchorsPayload::chunkX,
            ByteBufCodecs.VAR_INT, SyncChunkFogAnchorsPayload::chunkZ,
            FogAnchor.ClientView.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncChunkFogAnchorsPayload::anchors,
            SyncChunkFogAnchorsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
