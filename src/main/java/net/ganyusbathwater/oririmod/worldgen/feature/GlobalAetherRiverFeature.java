package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GlobalAetherRiverFeature extends Feature<NoneFeatureConfiguration> {

    private static final int[][] SCARLET_PROXIMITY_OFFSETS = {
            { 0, 0 },
            { 18, 0 }, { -18, 0 }, { 0, 18 }, { 0, -18 },
            { 34, 0 }, { -34, 0 }, { 0, 34 }, { 0, -34 },
            { 24, 24 }, { -24, 24 }, { 24, -24 }, { -24, -24 }
    };

    public GlobalAetherRiverFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        // Snap to chunk origin to perfectly map the global noise 16x16
        int startX = origin.getX() & ~15;
        int startZ = origin.getZ() & ~15;

        BlockState aetherLiquid = ModFluids.AETHER_BLOCK.get().defaultBlockState();
        BlockState magma = ModBlocks.AETHER_MAGMA_BLOCK.get().defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ElderwoodsBiomeSource ebs = (context.chunkGenerator().getBiomeSource() instanceof ElderwoodsBiomeSource b) ? b : null;

        int riverBottomY = -122;
        int riverSurfaceY = -118;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                double riverIntensity = ElderwoodsChunkGenerator.getRiverIntensity(worldX, worldZ);
                if (riverIntensity <= 0.005) {
                    continue;
                }

                if (ebs != null) {
                    riverIntensity *= ebs.getMainlandRiverFade(worldX, worldZ);
                    if (riverIntensity <= 0.005) {
                        continue;
                    }
                }

                // Safety check: detect Scarlet Cave climate within a ~34-block radius using synchronized
                // climate noise so the Aether River narrows, pinches closed, and stops BEFORE reaching Scarlet Caves.
                double maxScarletHumidity = sampleMaxScarletHumidityInRadius(worldX, worldZ);
                if (maxScarletHumidity > 0.08) {
                    double t = Mth.clamp((maxScarletHumidity - 0.08) / 0.095, 0.0, 1.0);
                    double smoothT = t * t * (3.0 - 2.0 * t);
                    riverIntensity *= (1.0 - smoothT);
                }

                if (riverIntensity > 0.005) {
                    double caveFloorSmooth = ElderwoodsChunkGenerator.getCaveFloorHeightDouble(worldX, worldZ, -115);

                    // Ravine noise: large scale, determines if this part of the river is an open crack or a tunnel
                    double ravineNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float)worldX * 0.005f, (float)worldZ * 0.005f, 2
                    );

                    // Map ravineNoise (-1 to 1) to a multiplier (0 to 1)
                    double crackMultiplier = Math.max(0.0, Math.min(1.0, (ravineNoise - 0.1) / 0.2));

                    // Add rough noise to the tunnel ceiling so it's not perfectly flat
                    double ceilingRoughness = net.ganyusbathwater.oririmod.util.FastNoise.fbm2D(
                        (float)worldX * 0.05f, (float)worldZ * 0.05f, 2
                    );
                    double tunnelCeilingHeight = -106.0 + (ceilingRoughness * 6.0);

                    double localRiverBottom;
                    double localRiverCeiling;
                    if (riverIntensity < 0.25) {
                        double p = Math.pow(riverIntensity / 0.25, 0.4);
                        localRiverBottom = caveFloorSmooth * (1.0 - p) + (-117) * p;

                        double ceilP = Math.pow(riverIntensity / 0.25, 0.6);
                        double crackCeiling = Math.min(-84.0, caveFloorSmooth * (1.0 - ceilP) + (caveFloorSmooth + 24) * ceilP);
                        localRiverCeiling = tunnelCeilingHeight * (1.0 - crackMultiplier) + crackCeiling * crackMultiplier;
                    } else if (riverIntensity < 0.5) {
                        localRiverBottom = -117;

                        double p = (riverIntensity - 0.25) / 0.25;
                        double crackCeiling = Math.min(-82.0, (caveFloorSmooth + 24) * (1.0 - p) + (caveFloorSmooth + 28) * p);
                        localRiverCeiling = tunnelCeilingHeight * (1.0 - crackMultiplier) + crackCeiling * crackMultiplier;
                    } else {
                        double p = Math.pow((riverIntensity - 0.5) / 0.5, 0.8);
                        localRiverBottom = (-117) * (1.0 - p) + riverBottomY * p;

                        double crackCeiling = Math.min(-80.0, caveFloorSmooth + 28);
                        localRiverCeiling = tunnelCeilingHeight * (1.0 - crackMultiplier) + crackCeiling * crackMultiplier;
                    }

                    // Carve down to the local river bottom from the vaulted ceiling
                    int carveTopY = (int) Math.round(localRiverCeiling);
                    int carveBottomY = (int) Math.floor(localRiverBottom);
                    int minCheckY = carveBottomY - 3;
                    if (carveTopY < minCheckY) {
                        continue;
                    }
                    boolean[] nearFluidByY = computeNearFluidMask(level, worldX, worldZ, minCheckY, carveTopY, pos);

                    for (int y = carveTopY; y >= minCheckY; y--) {
                        if (nearFluidByY[y - minCheckY]) {
                            continue;
                        }
                        pos.set(worldX, y, worldZ);
                        BlockState state = level.getBlockState(pos);

                        // Never carve bedrock, Scarlet lithology, or Ocean Abyssal Basalt crust
                        if (y <= -124 || state.is(Blocks.BEDROCK)
                                || state.is(ModBlocks.SCARLET_STONE.get())
                                || state.is(ModBlocks.SCARLET_DEEPSLATE.get())
                                || state.is(ModBlocks.ABYSSAL_BASALT.get())) {
                            continue;
                        }

                        if (y > localRiverBottom) {
                            // Hollow out the trench
                            if (y <= riverSurfaceY) {
                                if (!state.is(ModBlocks.HARDENED_MANASHROOM.get()) && !state.is(ModBlocks.AETHER_MAGMA_BLOCK.get())) {
                                    level.setBlock(pos, aetherLiquid, 2);
                                }
                            } else {
                                if (!state.isAir() && !state.is(Blocks.CAVE_AIR) && !state.is(ModBlocks.HARDENED_MANASHROOM.get()) && !state.is(ModBlocks.AETHER_MAGMA_BLOCK.get()) && !state.is(ModFluids.AETHER_BLOCK.get())) {
                                    level.setBlock(pos, air, 2);
                                }
                            }
                        } else {
                            // Paint the solid riverbed
                            if (state.blocksMotion() && !state.is(ModFluids.AETHER_BLOCK.get()) && !state.is(ModBlocks.AETHER_MAGMA_BLOCK.get())
                                && !state.is(ModBlocks.HARDENED_MANASHROOM.get())) {

                                double decorNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                                    (float)worldX * 0.1f, (float)y * 0.1f, (float)worldZ * 0.1f, 2
                                );

                                BlockState placeState = deepslate;
                                if (decorNoise > 0.6) {
                                    placeState = magma;
                                } else if (decorNoise < -0.6) {
                                    placeState = Blocks.TUFF.defaultBlockState();
                                } else if (decorNoise > 0.3 && decorNoise < 0.4) {
                                    placeState = Blocks.CALCITE.defaultBlockState();
                                } else if (decorNoise > -0.2 && decorNoise < 0.0) {
                                    placeState = Blocks.SMOOTH_BASALT.defaultBlockState();
                                }

                                level.setBlock(pos, placeState, 2);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static double sampleMaxScarletHumidityInRadius(int worldX, int worldZ) {
        double maxHumidity = -1.0;
        for (int[] offset : SCARLET_PROXIMITY_OFFSETS) {
            int sx = worldX + offset[0];
            int sz = worldZ + offset[1];
            double temp = ElderwoodsChunkGenerator.getDesertTemperatureNoise(sx, sz);
            if (temp <= 0.16) {
                double hum = ElderwoodsChunkGenerator.getScarletHumidityNoise(sx, sz);
                if (hum > maxHumidity) {
                    maxHumidity = hum;
                }
            }
        }
        return maxHumidity;
    }

    private static boolean[] computeNearFluidMask(WorldGenLevel level, int bx, int bz, int minCheckY, int maxCheckY,
                                                  BlockPos.MutableBlockPos checkPos) {
        int height = maxCheckY - minCheckY + 1;
        boolean[] mask = new boolean[height];
        int scanMinY = Math.max(level.getMinBuildHeight(), minCheckY - 2);
        int scanMaxY = Math.min(level.getMaxBuildHeight() - 1, maxCheckY + 2);
        int markedCount = 0;

        for (int dx = -2; dx <= 2; dx++) {
            int cx = bx + dx;
            for (int dz = -2; dz <= 2; dz++) {
                int cz = bz + dz;
                for (int cy = scanMinY; cy <= scanMaxY; cy++) {
                    int mStart = Math.max(minCheckY, cy - 2);
                    int mEnd = Math.min(maxCheckY, cy + 2);
                    boolean anyUnmarked = false;
                    for (int y = mStart; y <= mEnd; y++) {
                        if (!mask[y - minCheckY]) {
                            anyUnmarked = true;
                            break;
                        }
                    }
                    if (!anyUnmarked) {
                        continue;
                    }

                    checkPos.set(cx, cy, cz);
                    var fluidState = level.getBlockState(checkPos).getFluidState();
                    if (fluidState.is(FluidTags.WATER) || fluidState.is(FluidTags.LAVA)) {
                        for (int y = mStart; y <= mEnd; y++) {
                            int idx = y - minCheckY;
                            if (!mask[idx]) {
                                mask[idx] = true;
                                markedCount++;
                            }
                        }
                        if (markedCount >= height) {
                            return mask;
                        }
                    }
                }
            }
        }
        return mask;
    }
}
