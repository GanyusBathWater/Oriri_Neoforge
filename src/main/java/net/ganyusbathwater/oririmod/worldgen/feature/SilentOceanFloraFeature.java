package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper.MonumentProximity;
import net.ganyusbathwater.oririmod.worldgen.feature.OceanEcologyHelper.SilentOceanHabitat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates naturally zoned Silent Ocean and shallow beach marine flora
 * (kelp forests, seagrass meadows, sea pickles) while strictly respecting
 * Ocean Monument footprints across chunk decoration boundaries.
 */
public class SilentOceanFloraFeature extends Feature<NoneFeatureConfiguration> {

    public SilentOceanFloraFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (!level.getBlockState(origin).is(Blocks.WATER)) {
            return false;
        }

        // Scan downward to locate seabed under the water column
        BlockPos.MutableBlockPos scanPos = origin.mutable();
        while (scanPos.getY() > 28 && level.getBlockState(scanPos).is(Blocks.WATER)) {
            scanPos.move(0, -1, 0);
        }

        BlockState originFloor = level.getBlockState(scanPos);
        if (!isValidSeabedSubstrate(originFloor)) {
            return false;
        }

        int baseFloorY = scanPos.getY();
        if (baseFloorY < 28 || baseFloorY >= ElderwoodsChunkGenerator.BASE_HEIGHT - 1) {
            return false;
        }

        long seed = level.getSeed();
        Holder<Biome> originBiome = level.getBiome(scanPos);
        boolean isSilentOcean = originBiome.is(ElderwoodsChunkGenerator.THE_SILENT_OCEANS_KEY);

        if (isSilentOcean) {
            SilentOceanHabitat originHabitat = OceanEcologyHelper.getSilentOceanHabitat(
                    seed, scanPos.getX(), scanPos.getZ());
            if (originHabitat == SilentOceanHabitat.BARREN_SEABED) {
                return false;
            }
        }

        int patchRadius = 3 + random.nextInt(4);
        boolean placedAny = false;
        BlockPos.MutableBlockPos targetFloorPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos targetWaterPos = new BlockPos.MutableBlockPos();

        for (int dx = -patchRadius; dx <= patchRadius; dx++) {
            for (int dz = -patchRadius; dz <= patchRadius; dz++) {
                if (dx * dx + dz * dz > patchRadius * patchRadius + random.nextInt(3)) {
                    continue;
                }

                int targetX = origin.getX() + dx;
                int targetZ = origin.getZ() + dz;

                // Never spill flora into or adjacent to an Ocean Monument footprint across chunk borders
                if (OceanMonumentPlacementHelper.getMonumentProximity(level, targetX, targetZ)
                        == MonumentProximity.INSIDE) {
                    continue;
                }

                if (!findLocalFloor(level, targetX, baseFloorY, targetZ, targetFloorPos)) {
                    continue;
                }

                targetWaterPos.set(targetX, targetFloorPos.getY() + 1, targetZ);
                float canopyDensity = OceanEcologyHelper.getFloraCanopyMask(seed, targetX, targetZ);
                if (canopyDensity < 0.20f) {
                    continue;
                }

                Holder<Biome> colBiome = level.getBiome(targetFloorPos);
                if (colBiome.is(ElderwoodsChunkGenerator.THE_SILENT_OCEANS_KEY)) {
                    SilentOceanHabitat habitat = OceanEcologyHelper.getSilentOceanHabitat(seed, targetX, targetZ);
                    placedAny |= placeForSilentOceanHabitat(
                            level, random, targetFloorPos, targetWaterPos, habitat, canopyDensity);
                } else {
                    placedAny |= placeBeachFlora(level, random, targetFloorPos, targetWaterPos, canopyDensity);
                }
            }
        }

        return placedAny;
    }

    private boolean placeForSilentOceanHabitat(WorldGenLevel level, RandomSource random,
            BlockPos floorPos, BlockPos waterPos, SilentOceanHabitat habitat, float canopyDensity) {
        switch (habitat) {
            case BARREN_SEABED:
                return false;
            case KELP_FOREST:
                if (random.nextFloat() < canopyDensity * 0.62f && waterPos.getY() < ElderwoodsChunkGenerator.BASE_HEIGHT - 3) {
                    return tryPlaceKelp(level, random, waterPos, 4, 15);
                }
                if (random.nextFloat() < canopyDensity * 0.30f) {
                    return tryPlaceSeagrassOrPickle(level, random, waterPos, false);
                }
                return false;
            case MIXED_REEF:
                if (random.nextFloat() < canopyDensity * 0.42f && waterPos.getY() < ElderwoodsChunkGenerator.BASE_HEIGHT - 4) {
                    return tryPlaceKelp(level, random, waterPos, 3, 11);
                }
                if (random.nextFloat() < canopyDensity * 0.55f) {
                    return tryPlaceSeagrassOrPickle(level, random, waterPos, true);
                }
                return false;
            case CORAL_GARDEN:
                // Understory seagrass/pickles around coral beds; no canopy-blocking kelp forest
                if (random.nextFloat() < canopyDensity * 0.40f) {
                    return tryPlaceSeagrassOrPickle(level, random, waterPos, true);
                }
                return false;
            case SEAGRASS_MEADOW:
                if (random.nextFloat() < canopyDensity * 0.78f) {
                    return tryPlaceSeagrassOrPickle(level, random, waterPos, false);
                }
                return false;
        }
        return false;
    }

    private boolean placeBeachFlora(WorldGenLevel level, RandomSource random,
            BlockPos floorPos, BlockPos waterPos, float canopyDensity) {
        // Beach shallows keep sandy gaps while allowing patchy seagrass and occasional nearshore kelp in deeper coves
        int depth = ElderwoodsChunkGenerator.BASE_HEIGHT - waterPos.getY();
        if (depth >= 7 && OceanEcologyHelper.getSilentOceanHabitat(level.getSeed(), floorPos.getX(), floorPos.getZ())
                == SilentOceanHabitat.KELP_FOREST && random.nextFloat() < canopyDensity * 0.35f) {
            return tryPlaceKelp(level, random, waterPos, 3, Math.min(8, depth - 1));
        }
        if (random.nextFloat() < canopyDensity * 0.52f) {
            return tryPlaceSeagrassOrPickle(level, random, waterPos, true);
        }
        return false;
    }

    private boolean tryPlaceKelp(WorldGenLevel level, RandomSource random, BlockPos baseWaterPos,
            int minHeight, int maxExtraHeight) {
        int maxAllowedTop = ElderwoodsChunkGenerator.BASE_HEIGHT - 2;
        int desiredHeight = minHeight + random.nextInt(Math.max(1, maxExtraHeight));
        int height = Math.min(desiredHeight, maxAllowedTop - baseWaterPos.getY());
        if (height < 2) {
            return false;
        }

        BlockPos.MutableBlockPos kelpPos = baseWaterPos.mutable();
        int actualTopOffset = -1;
        for (int h = 0; h < height; h++) {
            kelpPos.set(baseWaterPos.getX(), baseWaterPos.getY() + h, baseWaterPos.getZ());
            if (!level.getBlockState(kelpPos).is(Blocks.WATER)) {
                break;
            }
            actualTopOffset = h;
        }
        if (actualTopOffset < 1) {
            return false;
        }

        for (int h = 0; h < actualTopOffset; h++) {
            kelpPos.set(baseWaterPos.getX(), baseWaterPos.getY() + h, baseWaterPos.getZ());
            level.setBlock(kelpPos, Blocks.KELP_PLANT.defaultBlockState(), 2);
        }
        kelpPos.set(baseWaterPos.getX(), baseWaterPos.getY() + actualTopOffset, baseWaterPos.getZ());
        level.setBlock(kelpPos, Blocks.KELP.defaultBlockState().setValue(KelpBlock.AGE, 18 + random.nextInt(7)), 2);
        return true;
    }

    private boolean tryPlaceSeagrassOrPickle(WorldGenLevel level, RandomSource random, BlockPos waterPos,
            boolean allowSeaPickles) {
        if (!level.getBlockState(waterPos).is(Blocks.WATER)) {
            return false;
        }
        if (allowSeaPickles && random.nextInt(14) == 0) {
            BlockState pickle = Blocks.SEA_PICKLE.defaultBlockState()
                    .setValue(SeaPickleBlock.PICKLES, 1 + random.nextInt(4))
                    .setValue(SeaPickleBlock.WATERLOGGED, true);
            level.setBlock(waterPos, pickle, 2);
            return true;
        }

        BlockPos abovePos = waterPos.above();
        if (random.nextInt(4) == 0
                && waterPos.getY() + 1 < ElderwoodsChunkGenerator.BASE_HEIGHT
                && level.getBlockState(abovePos).is(Blocks.WATER)) {
            level.setBlock(waterPos, Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
            level.setBlock(abovePos, Blocks.TALL_SEAGRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
            return true;
        }

        level.setBlock(waterPos, Blocks.SEAGRASS.defaultBlockState(), 2);
        return true;
    }

    private boolean findLocalFloor(WorldGenLevel level, int x, int approxFloorY, int z,
            BlockPos.MutableBlockPos outFloor) {
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (int dy = 4; dy >= -4; dy--) {
            int y = approxFloorY + dy;
            if (y < 28 || y >= ElderwoodsChunkGenerator.BASE_HEIGHT - 1) {
                continue;
            }
            outFloor.set(x, y, z);
            above.set(x, y + 1, z);
            if (level.getBlockState(above).is(Blocks.WATER) && isValidSeabedSubstrate(level.getBlockState(outFloor))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidSeabedSubstrate(BlockState state) {
        return state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.STONE)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(BlockTags.TERRACOTTA);
    }
}
