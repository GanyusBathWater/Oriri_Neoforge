package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration for a dripstone cluster feature.
 * Determines the block types, cluster radius range, and stalactite/stalagmite
 * heights.
 *
 * @param clusterRadius             Horizontal radius of the dripstone cluster
 * @param maxStalactiteHeight       Maximum height of stalactites hanging from
 *                                  ceiling
 * @param maxStalagmiteHeight       Maximum height of stalagmites growing from
 *                                  floor
 * @param floorToCeilingSearchRange How far to search for ceiling from floor
 */
public record ScarletDripstoneClusterConfig(
        IntProvider clusterRadius,
        int maxStalactiteHeight,
        int maxStalagmiteHeight,
        int floorToCeilingSearchRange,
        boolean useScarletBlocks) implements FeatureConfiguration {

    public static final Codec<ScarletDripstoneClusterConfig> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    IntProvider.CODEC.fieldOf("cluster_radius").forGetter(ScarletDripstoneClusterConfig::clusterRadius),
                    Codec.INT.fieldOf("max_stalactite_height")
                            .forGetter(ScarletDripstoneClusterConfig::maxStalactiteHeight),
                    Codec.INT.fieldOf("max_stalagmite_height")
                            .forGetter(ScarletDripstoneClusterConfig::maxStalagmiteHeight),
                    Codec.INT.fieldOf("floor_to_ceiling_search_range")
                            .forGetter(ScarletDripstoneClusterConfig::floorToCeilingSearchRange),
                    Codec.BOOL.fieldOf("use_scarlet_blocks").forGetter(ScarletDripstoneClusterConfig::useScarletBlocks))
            .apply(instance, ScarletDripstoneClusterConfig::new));
}
