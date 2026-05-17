package net.ganyusbathwater.oririmod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component payload that marks an item as a "Cosmic" item, enabling
 * the animated Calamity-style tooltip.
 *
 * @param style  A numeric style index (0=LEGENDARY, 1=GODLY, 2=MYTHIC...).
 *               Passed to the shader as a uniform so one shader handles all
 *               colour palette variants.
 * @param animated Whether the gradient animation should play (set false to
 *                 freeze the colours, e.g. for disabled/locked items).
 */
public record CosmicTooltipData(int style, boolean animated) {

    // --- Codec for disk persistence (item data components are persisted in NBT) ---
    public static final Codec<CosmicTooltipData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("style", 0).forGetter(CosmicTooltipData::style),
            Codec.BOOL.optionalFieldOf("animated", true).forGetter(CosmicTooltipData::animated)
    ).apply(inst, CosmicTooltipData::new));

    // --- Network stream codec for syncing to clients (required by DataComponentType) ---
    public static final StreamCodec<FriendlyByteBuf, CosmicTooltipData> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.INT,
                    CosmicTooltipData::style,
                    net.minecraft.network.codec.ByteBufCodecs.BOOL,
                    CosmicTooltipData::animated,
                    CosmicTooltipData::new
            );
}
