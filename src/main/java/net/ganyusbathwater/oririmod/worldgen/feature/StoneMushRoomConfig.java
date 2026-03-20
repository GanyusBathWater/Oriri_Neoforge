package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for the Stone Mushroom formation feature.
 * Uses HARDENED_MANASHROOM as the block material.
 */
public class StoneMushRoomConfig implements FeatureConfiguration {

    public static final Codec<StoneMushRoomConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("min_height").forGetter(c -> c.minHeight),
                    Codec.INT.fieldOf("max_height").forGetter(c -> c.maxHeight)
            ).apply(instance, StoneMushRoomConfig::new));

    public final int minHeight;
    public final int maxHeight;

    public StoneMushRoomConfig(int minHeight, int maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }
}
