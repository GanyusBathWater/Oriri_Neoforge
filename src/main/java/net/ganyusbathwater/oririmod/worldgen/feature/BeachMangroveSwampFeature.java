package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.custom.CattailBlock;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper.MonumentProximity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
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
 * Populates abnormally wide, flat Elder Beach basins with Tidal Mangrove Swamp vegetation:
 * vanilla Mangrove & Tall Mangrove trees rooted in mud and shallow water, emergent Cattail clumps,
 * Lily Pads on calm lagoon pools, Moss Carpets on mudbanks, and benthic Seagrass.
 */
public class BeachMangroveSwampFeature extends Feature<NoneFeatureConfiguration> {

    public BeachMangroveSwampFeature(Codec<NoneFeatureConfiguration> codec) {
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

        double swampFactor = ebs.getFlatBeachMangroveFactor(origin.getX(), origin.getZ());
        if (swampFactor < 0.18) {
            return false;
        }

        if (OceanMonumentPlacementHelper.getMonumentProximity(level, origin) == MonumentProximity.INSIDE) {
            return false;
        }

        BlockPos floorPos = findSolidFloorBelow(level, origin);
        if (floorPos == null || !level.getBlockState(floorPos).isSolid()) {
            return false;
        }

        BlockPos basePos = floorPos.above();
        int waterDepth = Math.max(0, (ElderwoodsChunkGenerator.BASE_HEIGHT + 1) - basePos.getY());

        boolean placedAny = false;

        // Place spaced Mangrove / Tall Mangrove trees on dry mudbanks and up to 4-block deep shallows
        if (waterDepth <= 4
                && random.nextFloat() < swampFactor * 0.42f
                && !hasNearbyMangroveLog(level, basePos, 4)) {
            prepareMudRootBed(level, floorPos);
            ResourceKey<ConfiguredFeature<?, ?>> treeKey = (random.nextFloat() < 0.30f)
                    ? TreeFeatures.TALL_MANGROVE
                    : TreeFeatures.MANGROVE;
            Holder<ConfiguredFeature<?, ?>> treeHolder = level.registryAccess()
                    .registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolder(treeKey)
                    .orElse(null);
            if (treeHolder != null && treeHolder.value().place(level, context.chunkGenerator(), random, basePos)) {
                placedAny = true;
            }
        }

        placedAny |= populateSwampPatch(level, random, ebs, origin);
        return placedAny;
    }

    private BlockPos findSolidFloorBelow(WorldGenLevel level, BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int i = 0; i < 6 && cursor.getY() > level.getMinBuildHeight() + 5; i++) {
            BlockState below = level.getBlockState(cursor.below());
            if (below.isSolid() && (level.isEmptyBlock(cursor) || level.getBlockState(cursor).is(Blocks.WATER))) {
                return cursor.below().immutable();
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private boolean hasNearbyMangroveLog(WorldGenLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= 5; dy++) {
                    check.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(check);
                    if (state.is(Blocks.MANGROVE_LOG) || state.is(Blocks.MANGROVE_WOOD)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void prepareMudRootBed(WorldGenLevel level, BlockPos floorPos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, floorPos.getX() + dx, floorPos.getZ() + dz) - 1;
                cursor.set(floorPos.getX() + dx, y, floorPos.getZ() + dz);
                BlockState cur = level.getBlockState(cursor);
                if (cur.is(Blocks.SAND) || cur.is(Blocks.GRAVEL) || cur.is(Blocks.SANDSTONE)) {
                    level.setBlock(cursor, Blocks.MUD.defaultBlockState(), 2);
                }
            }
        }
    }

    private boolean populateSwampPatch(WorldGenLevel level, RandomSource random, ElderwoodsBiomeSource ebs, BlockPos origin) {
        boolean placedAny = false;
        int radius = 4 + random.nextInt(3);
        BlockPos.MutableBlockPos floorPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos surfacePos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || random.nextFloat() < 0.35f) {
                    continue;
                }
                int tx = origin.getX() + dx;
                int tz = origin.getZ() + dz;
                double localSwamp = ebs.getFlatBeachMangroveFactor(tx, tz);
                if (localSwamp < 0.16) {
                    continue;
                }
                if (OceanMonumentPlacementHelper.getMonumentProximity(level, tx, tz) == MonumentProximity.INSIDE) {
                    continue;
                }

                int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, tx, tz) - 1;
                if (floorY < 56 || floorY > ElderwoodsChunkGenerator.BASE_HEIGHT + 4) {
                    continue;
                }
                floorPos.set(tx, floorY, tz);
                BlockState floorState = level.getBlockState(floorPos);
                if (!floorState.isSolid()) {
                    continue;
                }

                int waterDepth = Math.max(0, ElderwoodsChunkGenerator.BASE_HEIGHT - floorY);
                if (waterDepth == 0) {
                    surfacePos.set(tx, floorY + 1, tz);
                    placedAny |= decorateDrySwampBank(level, random, floorState, surfacePos);
                } else if (waterDepth <= 2) {
                    if (random.nextFloat() < 0.16f && tryPlaceCattail(level, random, floorPos.above(), waterDepth)) {
                        placedAny = true;
                    } else if (random.nextFloat() < 0.12f) {
                        placedAny |= tryPlaceLilyPad(level, tx, tz);
                    } else if (random.nextFloat() < 0.24f) {
                        placedAny |= tryPlaceSeagrass(level, random, floorPos.above(), waterDepth);
                    }
                } else if (waterDepth <= 6) {
                    if (random.nextFloat() < 0.16f) {
                        placedAny |= tryPlaceLilyPad(level, tx, tz);
                    }
                    if (random.nextFloat() < 0.26f) {
                        placedAny |= tryPlaceSeagrass(level, random, floorPos.above(), waterDepth);
                    }
                }
            }
        }
        return placedAny;
    }

    private boolean decorateDrySwampBank(WorldGenLevel level, RandomSource random, BlockState floorState, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return false;
        }
        float roll = random.nextFloat();
        if (roll < 0.08f && tryPlaceCattail(level, random, pos, 0)) {
            return true;
        }
        if ((floorState.is(Blocks.MUD) || floorState.is(Blocks.MOSS_BLOCK) || floorState.is(Blocks.MUDDY_MANGROVE_ROOTS))
                && roll < 0.22f) {
            level.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
            return true;
        }
        if (floorState.is(BlockTags.DIRT) && roll < 0.34f) {
            BlockState plant = random.nextFloat() < 0.45f
                    ? Blocks.FERN.defaultBlockState()
                    : Blocks.SHORT_GRASS.defaultBlockState();
            level.setBlock(pos, plant, 2);
            return true;
        }
        return false;
    }

    private boolean tryPlaceCattail(WorldGenLevel level, RandomSource random, BlockPos basePos, int waterDepth) {
        BlockState soil = level.getBlockState(basePos.below());
        if (!isValidCattailSoil(soil)) {
            return false;
        }
        int airHeight = 1 + (random.nextFloat() < 0.65f ? 1 : 0);
        int totalHeight = waterDepth + airHeight;
        if (totalHeight < 1 || totalHeight > 4) {
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

    private boolean tryPlaceLilyPad(WorldGenLevel level, int x, int z) {
        BlockPos waterPos = new BlockPos(x, ElderwoodsChunkGenerator.BASE_HEIGHT, z);
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
