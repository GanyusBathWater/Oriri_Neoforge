package net.ganyusbathwater.oririmod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SilentNereidColors(int primary, int secondary) {

    // Default: Deep-Sea Indigo (0x1A237E) primary, Bioluminescent Blue (0x00E5FF) secondary/emissive
    public static final SilentNereidColors DEFAULT = new SilentNereidColors(0x1A237E, 0x00E5FF);

    public static final Codec<SilentNereidColors> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("primary").forGetter(SilentNereidColors::primary),
                    Codec.INT.fieldOf("secondary").forGetter(SilentNereidColors::secondary)
            ).apply(instance, SilentNereidColors::new)
    );

    public static final StreamCodec<ByteBuf, SilentNereidColors> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SilentNereidColors::primary,
            ByteBufCodecs.INT,
            SilentNereidColors::secondary,
            SilentNereidColors::new
    );
}
