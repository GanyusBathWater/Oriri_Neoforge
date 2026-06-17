package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GlobalAetherRiverFeature extends Feature<NoneFeatureConfiguration> {

    public GlobalAetherRiverFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rng = context.random();

        // Snap to chunk origin to perfectly map the global noise 16x16
        int startX = origin.getX() & ~15;
        int startZ = origin.getZ() & ~15;

        BlockState aetherLiquid = ModFluids.AETHER_BLOCK.get().defaultBlockState();
        BlockState magma = ModBlocks.AETHER_MAGMA_BLOCK.get().defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int riverBottomY = -122;
        int riverSurfaceY = -118;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                double riverIntensity = ElderwoodsChunkGenerator.getRiverIntensity(worldX, worldZ);

                if (riverIntensity > 0.0) {
                    double caveFloorSmooth = ElderwoodsChunkGenerator.getCaveFloorHeightDouble(worldX, worldZ, -115);
                    
                    // Ravine noise: large scale, determines if this part of the river is an open crack or a tunnel
                    double ravineNoise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float)worldX * 0.005f, 0.0f, (float)worldZ * 0.005f, 2
                    );
                    
                    // Map ravineNoise (-1 to 1) to a multiplier (0 to 1)
                    // Lowered threshold to 0.1 to drastically increase ravine crack frequency
                    double crackMultiplier = Math.max(0.0, Math.min(1.0, (ravineNoise - 0.1) / 0.2));

                    // Add rough noise to the tunnel ceiling so it's not perfectly flat
                    double ceilingRoughness = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                        (float)worldX * 0.05f, 0.0f, (float)worldZ * 0.05f, 2
                    );
                    // Tunnel ceiling fluctuates between -112 and -100
                    double tunnelCeilingHeight = -106.0 + (ceilingRoughness * 6.0);
                    
                    double localRiverBottom;
                    double localRiverCeiling;
                    if (riverIntensity < 0.25) {
                        // Stage 1: Steep Ravine Wall (drops from cave floor down to riverbank)
                        double p = Math.pow(riverIntensity / 0.25, 0.4); 
                        localRiverBottom = caveFloorSmooth * (1.0 - p) + (-117) * p;
                        
                        // Ceiling rises quickly to open the crack
                        double ceilP = Math.pow(riverIntensity / 0.25, 0.6);
                        double crackCeiling = caveFloorSmooth * (1.0 - ceilP) + (caveFloorSmooth + 40) * ceilP;
                        localRiverCeiling = tunnelCeilingHeight * (1.0 - crackMultiplier) + crackCeiling * crackMultiplier;
                    } else if (riverIntensity < 0.5) {
                        // Stage 2: Flat Walkable Riverbank
                        localRiverBottom = -117;
                        
                        // Ceiling continues to rise slightly to form a vaulted roof
                        double p = (riverIntensity - 0.25) / 0.25;
                        double crackCeiling = (caveFloorSmooth + 40) * (1.0 - p) + (caveFloorSmooth + 50) * p;
                        localRiverCeiling = tunnelCeilingHeight * (1.0 - crackMultiplier) + crackCeiling * crackMultiplier;
                    } else {
                        // Stage 3: U-Shaped River Channel
                        double p = Math.pow((riverIntensity - 0.5) / 0.5, 0.8);
                        localRiverBottom = (-117) * (1.0 - p) + riverBottomY * p;
                        
                        // Ceiling is max height in the center
                        double crackCeiling = caveFloorSmooth + 50;
                        localRiverCeiling = tunnelCeilingHeight * (1.0 - crackMultiplier) + crackCeiling * crackMultiplier;
                    }

                    // Carve down to the local river bottom from the vaulted ceiling
                    int carveTopY = (int) Math.round(localRiverCeiling);
                    int carveBottomY = (int) Math.floor(localRiverBottom);

                    for (int y = carveTopY; y >= carveBottomY - 3; y--) {
                        pos.set(worldX, y, worldZ);
                        BlockState state = level.getBlockState(pos);

                        // Don't carve bedrock or already empty blocks
                        if (y <= -124 || state.is(Blocks.BEDROCK)) continue;

                        if (y > localRiverBottom) {
                            // Hollow out the trench
                            if (y <= riverSurfaceY) {
                                // Only replace blocks that are not part of the mushroom structure
                                if (!state.is(ModBlocks.HARDENED_MANASHROOM.get()) && !state.is(ModBlocks.AETHER_MAGMA_BLOCK.get())) {
                                    level.setBlock(pos, aetherLiquid, 3);
                                }
                            } else {
                                // Carve out the ravine. We use a blacklist so we correctly delete ores and dripstone!
                                if (!state.isAir() && !state.is(Blocks.CAVE_AIR) && !state.is(ModBlocks.HARDENED_MANASHROOM.get()) && !state.is(ModBlocks.AETHER_MAGMA_BLOCK.get()) && !state.is(ModFluids.AETHER_BLOCK.get())) {
                                    level.setBlock(pos, air, 3);
                                }
                            }
                        } else if (y >= localRiverBottom - 3.0) {
                            // Paint the solid riverbed
                            // Only paint if it's currently solid rock
                            if (state.isSolid() && !state.is(ModFluids.AETHER_BLOCK.get()) && !state.is(ModBlocks.AETHER_MAGMA_BLOCK.get())
                                && !state.is(ModBlocks.HARDENED_MANASHROOM.get())) {
                                
                                // Organic 3D noise fields for variety of blocks!
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
                                
                                level.setBlock(pos, placeState, 3);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
