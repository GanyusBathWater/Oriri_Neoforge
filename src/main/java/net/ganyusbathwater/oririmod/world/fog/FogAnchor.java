package net.ganyusbathwater.oririmod.world.fog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.ganyusbathwater.oririmod.block.custom.FogSourceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * Persistent chunk-attached fog emitter converted from a {@link FogSourceBlock} or
 * {@link net.ganyusbathwater.oririmod.block.custom.VolumeFogSourceBlock} during structure
 * placement. Decoupled from blocks so players can freely place torches, chests, or blocks on
 * {@code origin}, while tracking {@code supportBlocks} from the surrounding structure so the fog
 * automatically cleans itself up if the structure is demolished.
 */
public record FogAnchor(
        BlockPos origin,
        int density,
        FogSourceBlock.FogColor color,
        boolean isVolume,
        List<BlockPos> supportBlocks) {

    public static final Codec<FogSourceBlock.FogColor> COLOR_CODEC =
            StringRepresentable.fromEnum(FogSourceBlock.FogColor::values);

    public static final Codec<FogAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("origin").forGetter(FogAnchor::origin),
            Codec.intRange(FogSourceBlock.MIN_DENSITY, FogSourceBlock.MAX_DENSITY)
                    .fieldOf("density").forGetter(FogAnchor::density),
            COLOR_CODEC.fieldOf("color").forGetter(FogAnchor::color),
            Codec.BOOL.optionalFieldOf("volume", false).forGetter(FogAnchor::isVolume),
            BlockPos.CODEC.listOf().optionalFieldOf("supports", List.of()).forGetter(FogAnchor::supportBlocks)
    ).apply(instance, FogAnchor::new));

    public ClientView toClientView() {
        return new ClientView(origin, density, color, isVolume);
    }

    /**
     * Compact client-facing projection synced over the network (support blocks stay server-only).
     */
    public record ClientView(BlockPos origin, int density, FogSourceBlock.FogColor color, boolean isVolume) {

        private static final StreamCodec<ByteBuf, FogSourceBlock.FogColor> COLOR_STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(
                        idx -> {
                            FogSourceBlock.FogColor[] vals = FogSourceBlock.FogColor.values();
                            return (idx >= 0 && idx < vals.length) ? vals[idx] : FogSourceBlock.FogColor.MIST;
                        },
                        Enum::ordinal
                );

        public static final StreamCodec<ByteBuf, ClientView> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ClientView::origin,
                ByteBufCodecs.VAR_INT, ClientView::density,
                COLOR_STREAM_CODEC, ClientView::color,
                ByteBufCodecs.BOOL, ClientView::isVolume,
                ClientView::new
        );
    }
}
