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
        for (int i = 0; i < dirX.length; i++) {
            int px = origin.getX() + (int) Math.round(dirX[i] * checkRadiusX * (i >= 4 ? 0.707 : 1.0));
            int pz = origin.getZ() + (int) Math.round(dirZ[i] * checkRadiusZ * (i >= 4 ? 0.707 : 1.0));
            int py = origin.getY();
            while (level.isEmptyBlock(new BlockPos(px, py - 1, pz)) && py > context.chunkGenerator().getMinY()) py--;
            while (!level.isEmptyBlock(new BlockPos(px, py, pz)) && py < context.chunkGenerator().getGenDepth()) py++;
            if (py < minPerimeterY || !level.getBlockState(new BlockPos(px, py - 1, pz)).is(ModBlocks.SOL_SAND.get())) {
                return false;
            }
        }

        // Scan the area to find both lowest and highest surface points to guarantee flat enclosed rim
        int lowestSurfaceY = Integer.MAX_VALUE;
        int highestSurfaceY = Integer.MIN_VALUE;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double distanceSq = (double)(x * x) / (radiusX * radiusX) + (double)(z * z) / (radiusZ * radiusZ);
                if (distanceSq <= 1.0) {
                    BlockPos p = origin.offset(x, 0, z);
                    int y = p.getY();
                    while (level.isEmptyBlock(new BlockPos(p.getX(), y - 1, p.getZ())) && y > context.chunkGenerator().getMinY()) y--;
                    while (!level.isEmptyBlock(new BlockPos(p.getX(), y, p.getZ())) && y < context.chunkGenerator().getGenDepth()) y++;
                    if (y < lowestSurfaceY) {
                        lowestSurfaceY = y;
                    }
                    if (y > highestSurfaceY) {
                        highestSurfaceY = y;
                    }
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

        // Carve crater and fill with water (Oval, sloping sides)
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double distanceSq = (double)(x * x) / (radiusX * radiusX) + (double)(z * z) / (radiusZ * radiusZ);
                if (distanceSq <= 1.0) {
                    int currentDepth = (int) Math.round(depth * (1.0 - distanceSq));

                    // Find actual surface at x, z so we know how much air to carve
                    BlockPos p = origin.offset(x, 0, z);
                    int actualY = p.getY();
                    while (level.isEmptyBlock(new BlockPos(p.getX(), actualY - 1, p.getZ())) && actualY > context.chunkGenerator().getMinY()) actualY--;
                    while (!level.isEmptyBlock(new BlockPos(p.getX(), actualY, p.getZ())) && actualY < context.chunkGenerator().getGenDepth()) actualY++;

                    for (int y = waterLevel - currentDepth - 1; y <= actualY; y++) {
                        BlockPos pos = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
                        
                        if (y <= waterLevel) {
                            if (y > waterLevel - currentDepth) {
                                // Water inside the bowl
                                setBlock(level, pos, Blocks.WATER.defaultBlockState());
                            } else {
                                // Sand bottom prevents leaking into caves
                                setBlock(level, pos, ModBlocks.SOL_SAND.get().defaultBlockState());
                            }
                        } else {
                            // Air above water to carve the crater out of the hill
                            setBlock(level, pos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Moss patches and trees around the edge
        int maxRadius = Math.max(radiusX, radiusZ);
        int borderRadius = maxRadius + 3 + random.nextInt(3);
        BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
        EpochTreeFeature treeFeature = new EpochTreeFeature(EpochTreeFeature.EpochTreeConfig.CODEC);
        EpochTreeFeature.EpochTreeConfig treeConfig = new EpochTreeFeature.EpochTreeConfig(true); // Fruitful trees near oasis!

        for (int x = -borderRadius; x <= borderRadius; x++) {
            for (int z = -borderRadius; z <= borderRadius; z++) {
                double distSq = (double)(x * x) / (radiusX * radiusX) + (double)(z * z) / (radiusZ * radiusZ);
                if (distSq > 1.0 && distSq <= ((double)(borderRadius * borderRadius) / (maxRadius * maxRadius))) {
                    BlockPos surfacePos = origin.offset(x, 0, z);
                    
                    // Find actual surface y
                    int actualY = surfacePos.getY();
                    while (level.isEmptyBlock(new BlockPos(surfacePos.getX(), actualY - 1, surfacePos.getZ())) && actualY > context.chunkGenerator().getMinY()) {
                        actualY--;
                    }
                    while (!level.isEmptyBlock(new BlockPos(surfacePos.getX(), actualY, surfacePos.getZ())) && actualY < context.chunkGenerator().getGenDepth()) {
                        actualY++;
                    }
                    BlockPos placePos = new BlockPos(surfacePos.getX(), actualY, surfacePos.getZ());
                    BlockPos belowPos = placePos.below();

                    if (level.getBlockState(belowPos).is(ModBlocks.SOL_SAND.get())) {
                        // Swap with moss occasionally
                        if (random.nextFloat() < 0.4f) {
                            setBlock(level, belowPos, moss);
                        }

                        // Spawn some Epoch Trees clustered around
                        if (random.nextFloat() < 0.05f) { // 5% chance per block in the border band
                            FeaturePlaceContext<EpochTreeFeature.EpochTreeConfig> treeContext = new FeaturePlaceContext<>(
                                context.topFeature(), level, context.chunkGenerator(), random, placePos, treeConfig
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
