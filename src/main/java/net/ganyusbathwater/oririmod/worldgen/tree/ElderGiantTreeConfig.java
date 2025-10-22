package net.ganyusbathwater.oririmod.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record ElderGiantTreeConfig(
        IntProvider trunkHeight,
        IntProvider trunkRadius,                 // neu: 1-5
        IntProvider canopyRadius,
        IntProvider branchLength,
        BlockStateProvider logProvider,
        BlockStateProvider leavesProvider,
        BlockStateProvider floweringLeavesProvider,
        float floweringChance
) implements FeatureConfiguration {

    public static final Codec<ElderGiantTreeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.CODEC.fieldOf("trunk_height").forGetter(ElderGiantTreeConfig::trunkHeight),
            IntProvider.CODEC.fieldOf("trunk_radius").forGetter(ElderGiantTreeConfig::trunkRadius),
            IntProvider.CODEC.fieldOf("canopy_radius").forGetter(ElderGiantTreeConfig::canopyRadius),
            IntProvider.CODEC.fieldOf("branch_length").forGetter(ElderGiantTreeConfig::branchLength),
            BlockStateProvider.CODEC.fieldOf("log_provider").forGetter(ElderGiantTreeConfig::logProvider),
            BlockStateProvider.CODEC.fieldOf("leaves_provider").forGetter(ElderGiantTreeConfig::leavesProvider),
            BlockStateProvider.CODEC.fieldOf("flowering_leaves_provider").forGetter(ElderGiantTreeConfig::floweringLeavesProvider),
            Codec.FLOAT.fieldOf("flowering_chance").orElse(0.2f).forGetter(ElderGiantTreeConfig::floweringChance)
    ).apply(instance, ElderGiantTreeConfig::new));
}