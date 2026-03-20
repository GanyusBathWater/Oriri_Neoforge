package net.ganyusbathwater.oririmod.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record AbyssCrownTreeConfig(
        IntProvider trunkHeight,
        IntProvider trunkRadius,
        IntProvider canopyRadius,
        BlockStateProvider logProvider,
        BlockStateProvider stemProvider,
        BlockStateProvider leavesProvider) implements FeatureConfiguration {

    public static final Codec<AbyssCrownTreeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.CODEC.fieldOf("trunk_height").forGetter(AbyssCrownTreeConfig::trunkHeight),
            IntProvider.CODEC.fieldOf("trunk_radius").forGetter(AbyssCrownTreeConfig::trunkRadius),
            IntProvider.CODEC.fieldOf("canopy_radius").forGetter(AbyssCrownTreeConfig::canopyRadius),
            BlockStateProvider.CODEC.fieldOf("log_provider").forGetter(AbyssCrownTreeConfig::logProvider),
            BlockStateProvider.CODEC.fieldOf("stem_provider").forGetter(AbyssCrownTreeConfig::stemProvider),
            BlockStateProvider.CODEC.fieldOf("leaves_provider").forGetter(AbyssCrownTreeConfig::leavesProvider)
    ).apply(instance, AbyssCrownTreeConfig::new));
}
