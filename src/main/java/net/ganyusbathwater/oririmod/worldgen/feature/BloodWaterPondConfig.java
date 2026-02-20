package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record BloodWaterPondConfig(IntProvider radius, IntProvider depth) implements FeatureConfiguration {
    public static final Codec<BloodWaterPondConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.CODEC.fieldOf("radius").forGetter(BloodWaterPondConfig::radius),
            IntProvider.CODEC.fieldOf("depth").forGetter(BloodWaterPondConfig::depth))
            .apply(instance, BloodWaterPondConfig::new));
}
