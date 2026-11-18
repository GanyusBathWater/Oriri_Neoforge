package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.custom.CattailBlock;
import net.ganyusbathwater.oririmod.worldgen.ElderValleyTerrainHelper;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.ganyusbathwater.oririmod.worldgen.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Populates {@code oririmod:elder_valley} corridors with varied riparian ecosystems:
 * <ul>
 *     <li>Dense waterlogged and bankside {@link ModBlocks#CATTAIL} reedbeds along calm shallows and mud banks.</li>
 *     <li>Calm-pool lily pads, seagrass meadows, and mossy shoreline boulders near rapids/weirs.</li>
 *     <li>Variable canopy stretches alternating between sunlit wildflower/cattail floodplains and
 *     wooded Elder Tree groves seated strictly on stable flat terraces clear of river channels.</li>
 * </ul>
 */
public class ElderValleyFeature extends Feature<NoneFeatureConfiguration> {

    public ElderValleyFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!(context.chunkGenerator().getBiomeSource() instanceof ElderwoodsBiomeSource ebs)) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ElderValleyTerrainHelper.ValleyColumnInfo valley = new ElderValleyTerrainHelper.ValleyColumnInfo();
        ebs.sampleValleyColumn(origin.getX(), origin.getZ(), valley);
        if (!valley.hasValley || valley.biomeMask < 0.20) {
            return false;
        }

        boolean placedAny = false;

        // 1. Variable Riparian Canopy: Wooded Grove stretches place spaced Elder Trees on stable dry terraces;
        //    Open Meadow stretches stay sunlit for scenic boating & wildflower/cattail vistas.
        if (valley.canopyDensity > 0.52
                && !valley.inWaterChannel
                && valley.distToWaterEdge >= 4.5
                && random.nextFloat() < (valley.canopyDensity - 0.45f) * 0.65f) {
            placedAny |= tryPlaceBankElderTree(level, context, random, ebs, valley, origin);
        }

        // 2. Cattail marshes, bank grasses/wildflowers, moss boulders, lily pads & aquatic flora
        placedAny |= decorateRiparianPatch(level, random, ebs, origin);
        return placedAny;
    }

    private boolean tryPlaceBankElderTree(
            WorldGenLevel level,
            FeaturePlaceContext<NoneFeatureConfiguration> context,
            RandomSource random,
            ElderwoodsBiomeSource ebs,
            ElderValleyTerrainHelper.ValleyColumnInfo centerValley,
            BlockPos origin) {

        int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ()) - 1;
        if (floorY < centerValley.waterY + 2) {
            return false;
        }

        BlockPos floorPos = new BlockPos(origin.getX(), floorY, origin.getZ());
        BlockState floorState = level.getBlockState(floorPos);
        if (!floorState.is(BlockTags.DIRT)) {
            return false;
        }

        // Ensure trunk sits on a dry flat/gentle terrace (never hovering over cliff walls or channel water)
        int minH = floorY;
        int maxH = floorY;
        ElderValleyTerrainHelper.ValleyColumnInfo probe = new ElderValleyTerrainHelper.ValleyColumnInfo();
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dz = -2; dz <= 2; dz += 2) {
                int px = origin.getX() + dx;
                int pz = origin.getZ() + dz;
                ebs.sampleValleyColumn(px, pz, probe);
                if (probe.inWaterChannel || probe.distToWaterEdge < 2.5) {
                    return false;
                }
                int ph = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, px, pz) - 1;
                if (ph < probe.waterY + 1) {
                    return false;
                }
                if (ph < minH) minH = ph;
                if (ph > maxH) maxH = ph;
            }
        }
        if (maxH - minH > 2 || hasNearbyTreeLogs(level, floorPos.above(), 7)) {
            return false;
        }

        Holder<ConfiguredFeature<?, ?>> treeHolder = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(ModConfiguredFeatures.ELDER_TREE_KEY)
                .orElse(null);
        return treeHolder != null
                && treeHolder.value().place(level, context.chunkGenerator(), random, floorPos.above());
    }

    private static boolean hasNearbyTreeLogs(WorldGenLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = -2; dy <= 6; dy += 2) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(BlockTags.LOGS) || state.is(ModBlocks.ELDER_LOG_BLOCK.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean decorateRiparianPatch(
            WorldGenLevel level, RandomSource random, ElderwoodsBiomeSource ebs, BlockPos origin) {

        boolean placedAny = false;
        int radius = 5 + random.nextInt(3);
        ElderValleyTerrainHelper.ValleyColumnInfo info = new ElderValleyTerrainHelper.ValleyColumnInfo();
        BlockPos.MutableBlockPos floorPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos surfacePos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextFloat() < 0.30f) {
                    continue;
                }
                int tx = origin.getX() + dx;
                int tz = origin.getZ() + dz;

                ebs.sampleValleyColumn(tx, tz, info);
                if (!info.hasValley || info.biomeMask < 0.15) {
                    continue;
                }

                int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, tx, tz) - 1;
                if (floorY <= level.getMinBuildHeight() + 5) {
                    continue;
                }

                floorPos.set(tx, floorY, tz);
                BlockState floorState = level.getBlockState(floorPos);
                if (!floorState.isSolid()) {
                    continue;
                }

                surfacePos.set(tx, floorY + 1, tz);

                if (info.inWaterChannel) {
                    // Never obstruct active waterfall chutes or rocky weir spillways
                    if (info.isWeirCrest || info.isWaterfallStep) {
                        continue;
                    }
                    int waterDepth = info.waterY - floorY;
                    if (waterDepth <= 0) {
                        continue;
                    }

                    // Shallow shoreline water margins: cattails & lily pads
                    if (waterDepth <= 2 && info.distToWaterEdge >= -3.8) {
                        float cattailChance = info.canopyDensity <= 0.52 ? 0.24f : 0.14f;
                        if (random.nextFloat() < cattailChance && tryPlaceCattail(level, random, surfacePos, waterDepth)) {
                            placedAny = true;
                        } else if (waterDepth >= 1 && random.nextFloat() < 0.12f && tryPlaceLilyPad(level, tx, info.waterY, tz)) {
                            placedAny = true;
                        }
                    } else if (waterDepth >= 2) {
                        // Calm deeper pool beds: seagrass & rare bankside lily pads
                        if (info.distToWaterEdge >= -3.2 && random.nextFloat() < 0.08f && tryPlaceLilyPad(level, tx, info.waterY, tz)) {
                            placedAny = true;
                        } else if (random.nextFloat() < 0.22f && tryPlaceSeagrass(level, random, surfacePos, waterDepth)) {
                            placedAny = true;
                        }
                    }
                } else {
                    // Dry riparian banks, gravel/mud shores & valley meadows
                    if (!level.isEmptyBlock(surfacePos)) {
                        continue;
                    }
                    int slopeN = Math.abs(floorY - (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, tx + 1, tz) - 1));
                    int slopeE = Math.abs(floorY - (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, tx, tz + 1) - 1));
                    if (Math.max(slopeN, slopeE) >= 2) {
                        continue; // Keep steep canyon cliffs cleanly rocky
                    }

                    if (info.distToWaterEdge <= 3.5) {
                        float roll = random.nextFloat();
                        if (roll < 0.18f && tryPlaceCattail(level, random, surfacePos, 0)) {
                            placedAny = true;
                        } else if (roll < 0.21f && floorY >= info.waterY + 1 && tryPlaceMossyBoulder(level, random, floorPos, surfacePos)) {
                            placedAny = true;
                        } else if ((floorState.is(Blocks.MUD) || floorState.is(Blocks.MOSS_BLOCK)) && roll < 0.38f) {
                            level.setBlock(surfacePos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                            placedAny = true;
                        } else if (floorState.is(BlockTags.DIRT) && roll < 0.60f) {
                            placedAny |= placeValleyGroundFlora(level, random, surfacePos, info.canopyDensity);
                        }
                    } else if (floorState.is(BlockTags.DIRT) && random.nextFloat() < 0.42f) {
                        placedAny |= placeValleyGroundFlora(level, random, surfacePos, info.canopyDensity);
                    }
                }
            }
        }
        return placedAny;
    }

    private boolean placeValleyGroundFlora(
            WorldGenLevel level, RandomSource random, BlockPos pos, double canopyDensity) {

        float roll = random.nextFloat();
        if (roll < 0.045f) {
            level.setBlock(pos, ModBlocks.STAR_HERB.get().defaultBlockState(), 2);
            return true;
        }
        if (canopyDensity <= 0.52 && roll < 0.16f) {
            BlockState flower = switch (random.nextInt(4)) {
                case 0 -> Blocks.BLUE_ORCHID.defaultBlockState();
                case 1 -> Blocks.OXEYE_DAISY.defaultBlockState();
                case 2 -> Blocks.CORNFLOWER.defaultBlockState();
                default -> Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
            };
            level.setBlock(pos, flower, 2);
            return true;
        }
        if (roll < 0.30f && level.isEmptyBlock(pos.above())) {
            BlockState tall = (canopyDensity > 0.52 && random.nextFloat() < 0.45f)
                    ? Blocks.LARGE_FERN.defaultBlockState()
                    : Blocks.TALL_GRASS.defaultBlockState();
            level.setBlock(pos, tall.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
            level.setBlock(pos.above(), tall.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
            return true;
        }
        BlockState plant = (canopyDensity > 0.52 && random.nextFloat() < 0.45f)
                ? Blocks.FERN.defaultBlockState()
                : Blocks.SHORT_GRASS.defaultBlockState();
        level.setBlock(pos, plant, 2);
        return true;
    }

    private boolean tryPlaceMossyBoulder(WorldGenLevel level, RandomSource random, BlockPos floorPos, BlockPos surfacePos) {
        BlockState boulder = random.nextFloat() < 0.65f
                ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                : Blocks.ANDESITE.defaultBlockState();
        level.setBlock(surfacePos, boulder, 2);
        if (random.nextFloat() < 0.40f && level.isEmptyBlock(surfacePos.above())) {
            level.setBlock(surfacePos.above(), Blocks.MOSS_CARPET.defaultBlockState(), 2);
        }
        if (level.getBlockState(floorPos).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(floorPos, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
        }
        return true;
    }

    private boolean tryPlaceCattail(WorldGenLevel level, RandomSource random, BlockPos basePos, int waterDepth) {
        BlockState soil = level.getBlockState(basePos.below());
        if (!isValidCattailSoil(soil)) {
            return false;
        }
        int airHeight = 1 + (random.nextFloat() < 0.60f ? 1 : (random.nextFloat() < 0.18f ? 2 : 0));
        int totalHeight = waterDepth + airHeight;
        if (totalHeight < 1 || totalHeight > 5) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = basePos.mutable();
        for (int i = 0; i < totalHeight; i++) {
            cursor.set(basePos.getX(), basePos.getY() + i, basePos.getZ());
            BlockState existing = level.getBlockState(cursor);
            boolean expectedWater = i < waterDepth;
            if (expectedWater && !existing.is(Blocks.WATER)) {
                return false;
            }
            if (!expectedWater && !existing.isAir()) {
                return false;
            }
        }

        int age = random.nextInt(16);
        for (int i = 0; i < totalHeight; i++) {
            cursor.set(basePos.getX(), basePos.getY() + i, basePos.getZ());
            boolean isTop = (i == totalHeight - 1);
            boolean waterlogged = (i < waterDepth);
            BlockState cattail = ModBlocks.CATTAIL.get().defaultBlockState()
                    .setValue(CattailBlock.PART, isTop ? CattailBlock.CattailPart.TOP : CattailBlock.CattailPart.BOTTOM)
                    .setValue(CattailBlock.AGE, age)
                    .setValue(CattailBlock.WATERLOGGED, waterlogged);
            level.setBlock(cursor, cattail, 2);
        }
        return true;
    }

    private boolean tryPlaceLilyPad(WorldGenLevel level, int x, int waterY, int z) {
        BlockPos waterPos = new BlockPos(x, waterY, z);
        BlockPos padPos = waterPos.above();
        if (level.getBlockState(waterPos).is(Blocks.WATER) && level.isEmptyBlock(padPos)) {
            level.setBlock(padPos, Blocks.LILY_PAD.defaultBlockState(), 2);
            return true;
        }
        return false;
    }

    private boolean tryPlaceSeagrass(WorldGenLevel level, RandomSource random, BlockPos waterPos, int waterDepth) {
        if (!level.getBlockState(waterPos).is(Blocks.WATER)) {
            return false;
        }
        BlockState floor = level.getBlockState(waterPos.below());
        if (!floor.isSolid() || floor.is(Blocks.MAGMA_BLOCK)) {
            return false;
        }
        BlockPos above = waterPos.above();
        if (waterDepth >= 2 && random.nextInt(3) == 0 && level.getBlockState(above).is(Blocks.WATER)) {
            level.setBlock(waterPos, Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
            level.setBlock(above, Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
            return true;
        }
        level.setBlock(waterPos, Blocks.SEAGRASS.defaultBlockState(), 2);
        return true;
    }

    private static boolean isValidCattailSoil(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }
}
