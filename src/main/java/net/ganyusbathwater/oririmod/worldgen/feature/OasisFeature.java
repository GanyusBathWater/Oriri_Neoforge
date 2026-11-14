package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class OasisFeature extends Feature<NoneFeatureConfiguration> {

    private static final EpochTreeFeature SHARED_EPOCH_TREE = new EpochTreeFeature(EpochTreeFeature.EpochTreeConfig.CODEC);
    private static final EpochTreeFeature.EpochTreeConfig SHARED_TREE_CONFIG = new EpochTreeFeature.EpochTreeConfig(true);

    public OasisFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Must spawn on Sol Sand
        if (!level.getBlockState(origin.below()).is(ModBlocks.SOL_SAND.get())) {
            return false;
        }

        int radiusX = 5 + random.nextInt(3); // 5 to 7 radius X
        int radiusZ = 5 + random.nextInt(3); // 5 to 7 radius Z
        int depth = 3 + random.nextInt(2);   // 3 to 4 blocks deep at center

        // Ensure Oasis only spawns deep inside broad desert terrain well above sea level,
        // never punching salt craters into small desert islands or steep coastal dune slopes.
        int minPerimeterY = ElderwoodsChunkGenerator.BASE_HEIGHT + 4;
        int checkRadiusX = radiusX + 5;
        int checkRadiusZ = radiusZ + 5;
        int[] dirX = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dirZ = {0, 0, 1, -1, 1, -1, 1, -1};
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < dirX.length; i++) {
            int px = origin.getX() + (int) Math.round(dirX[i] * checkRadiusX * (i >= 4 ? 0.707 : 1.0));
            int pz = origin.getZ() + (int) Math.round(dirZ[i] * checkRadiusZ * (i >= 4 ? 0.707 : 1.0));
            int py = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, px, pz);
            mutablePos.set(px, py - 1, pz);
            if (py < minPerimeterY || !level.getBlockState(mutablePos).is(ModBlocks.SOL_SAND.get())) {
                return false;
            }
        }

        // Scan the area once, caching surface heights across the bowl so carving doesn't re-query
        int spanX = radiusX * 2 + 1;
        int spanZ = radiusZ * 2 + 1;
        int[] surfaceGrid = new int[spanX * spanZ];
        int lowestSurfaceY = Integer.MAX_VALUE;
        int highestSurfaceY = Integer.MIN_VALUE;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double distanceSq = (double)(x * x) / (radiusX * radiusX) + (double)(z * z) / (radiusZ * radiusZ);
                if (distanceSq <= 1.0) {
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + x, origin.getZ() + z);
                    surfaceGrid[(x + radiusX) * spanZ + (z + radiusZ)] = y;
                    if (y < lowestSurfaceY) lowestSurfaceY = y;
                    if (y > highestSurfaceY) highestSurfaceY = y;
                }
            }
        }

        if (lowestSurfaceY < ElderwoodsChunkGenerator.BASE_HEIGHT + 3
                || highestSurfaceY - lowestSurfaceY > 6) {
            return false;
        }
        
        // Water is strictly below the lowest terrain lip, guaranteeing a flat, spill-free pond without artificial walls
        int waterLevel = lowestSurfaceY - 1;
        if (waterLevel - depth < ElderwoodsChunkGenerator.BASE_HEIGHT) {
            return false;
        }

        BlockState waterState = Blocks.WATER.defaultBlockState();
        BlockState solSandState = ModBlocks.SOL_SAND.get().defaultBlockState();
        BlockState airState = Blocks.AIR.defaultBlockState();

        // Carve crater and fill with water (Oval, sloping sides)
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double distanceSq = (double)(x * x) / (radiusX * radiusX) + (double)(z * z) / (radiusZ * radiusZ);
                if (distanceSq <= 1.0) {
                    int currentDepth = (int) Math.round(depth * (1.0 - distanceSq));
                    int actualY = surfaceGrid[(x + radiusX) * spanZ + (z + radiusZ)];
                    int wx = origin.getX() + x;
                    int wz = origin.getZ() + z;

                    for (int y = waterLevel - currentDepth - 1; y <= actualY; y++) {
                        mutablePos.set(wx, y, wz);
                        if (y <= waterLevel) {
                            level.setBlock(mutablePos, (y > waterLevel - currentDepth) ? waterState : solSandState, 2);
                        } else {
                            level.setBlock(mutablePos, airState, 2);
                        }
                    }
                }
            }
        }

        // Moss patches and trees around the edge
        int maxRadius = Math.max(radiusX, radiusZ);
        int borderRadius = maxRadius + 3 + random.nextInt(3);
        BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
        EpochTreeFeature treeFeature = SHARED_EPOCH_TREE;
        EpochTreeFeature.EpochTreeConfig treeConfig = SHARED_TREE_CONFIG; // Fruitful trees near oasis!

        for (int x = -borderRadius; x <= borderRadius; x++) {
            for (int z = -borderRadius; z <= borderRadius; z++) {
                double distSq = (double)(x * x) / (radiusX * radiusX) + (double)(z * z) / (radiusZ * radiusZ);
                if (distSq > 1.0 && distSq <= ((double)(borderRadius * borderRadius) / (maxRadius * maxRadius))) {
                    int wx = origin.getX() + x;
                    int wz = origin.getZ() + z;
                    int actualY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, wx, wz);
                    mutablePos.set(wx, actualY - 1, wz);

                    if (level.getBlockState(mutablePos).is(ModBlocks.SOL_SAND.get())) {
                        // Swap with moss occasionally
                        if (random.nextFloat() < 0.4f) {
                            level.setBlock(mutablePos, moss, 2);
                        }

                        // Spawn some Epoch Trees clustered around
                        if (random.nextFloat() < 0.05f) { // 5% chance per block in the border band
                            FeaturePlaceContext<EpochTreeFeature.EpochTreeConfig> treeContext = new FeaturePlaceContext<>(
                                context.topFeature(), level, context.chunkGenerator(), random, new BlockPos(wx, actualY, wz), treeConfig
                            );
                            treeFeature.place(treeContext);
                        }
                    }
                }
            }
        }

        return true;
    }
}
