package net.ganyusbathwater.oririmod.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LizardTailColors(int primary, int secondary) {

    // Default: Dark Green (0x1F5E29) primary, Green (0x63A832) secondary
    public static final LizardTailColors DEFAULT = new LizardTailColors(0x1F5E29, 0x63A832);

    public static final Codec<LizardTailColors> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("primary").forGetter(LizardTailColors::primary),
                    Codec.INT.fieldOf("secondary").forGetter(LizardTailColors::secondary)
            ).apply(instance, LizardTailColors::new)
    );

    public static final StreamCodec<ByteBuf, LizardTailColors> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            LizardTailColors::primary,
            ByteBufCodecs.INT,
            LizardTailColors::secondary,
            LizardTailColors::new
    );
}
