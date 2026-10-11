package net.ganyusbathwater.oririmod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record KitsuneColors(int primary, int secondary) {

    // Default: White Fur (0xFFFFFF), Vanilla Red Dye Tips (0xB02E26)
    public static final KitsuneColors DEFAULT = new KitsuneColors(0xFFFFFF, 0xB02E26);

    public static final Codec<KitsuneColors> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("primary").forGetter(KitsuneColors::primary),
                    Codec.INT.fieldOf("secondary").forGetter(KitsuneColors::secondary)
            ).apply(instance, KitsuneColors::new)
    );

    public static final StreamCodec<ByteBuf, KitsuneColors> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            KitsuneColors::primary,
            ByteBufCodecs.INT,
            KitsuneColors::secondary,
            KitsuneColors::new
    );
}
