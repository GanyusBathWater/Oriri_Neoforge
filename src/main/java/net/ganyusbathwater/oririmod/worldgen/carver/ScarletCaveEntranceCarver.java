package net.ganyusbathwater.oririmod.worldgen.carver;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.function.Function;

public class ScarletCaveEntranceCarver extends WorldCarver<CaveCarverConfiguration> {

    // Biome keys for scarlet biomes
    private static final ResourceKey<Biome> SCARLET_SWAMP_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));

    public ScarletCaveEntranceCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
        net.ganyusbathwater.oririmod.OririMod.LOGGER.info("ScarletCaveEntranceCarver Constructed!");
    }

    @Override
    public int getRange() {
        return 4; // Check up to 4 chunks away
    }

    @Override
    public boolean isStartChunk(CaveCarverConfiguration config, RandomSource random) {
        return random.nextFloat() <= config.probability;
    }

    @Override
    public boolean carve(CarvingContext context, CaveCarverConfiguration config, ChunkAccess chunk,
            Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource random, Aquifer aquifer,
            net.minecraft.world.level.ChunkPos chunkPos, CarvingMask carvingMask) {
        net.ganyusbathwater.oririmod.OririMod.LOGGER
                .info("ScarletCaveEntranceCarver carving chunk at " + chunk.getPos() + " from origin " + chunkPos);
                
        // Use the origin chunk position so all target chunks agree on the exact same entrance geometry
        BlockPos pos = chunkPos.getMiddleBlockPosition(0);
        
        long originSeed = (chunkPos.x * 341873128712L) ^ (chunkPos.z * 132897987541L);
        RandomSource originRandom = RandomSource.create(originSeed);

        int maxRadius = 14; // Increased from 8 to give much more reach
        int minRadius = 6;  // Increased from 4
        int entranceTop = 150; // High enough to cut through any surface terrain
        int entranceBottom = -16 + originRandom.nextInt(32); // Deterministic bottom

        net.ganyusbathwater.oririmod.OririMod.LOGGER
                .info("DEBUG_SCARLET: Carving from Y=" + entranceTop + " down to Y=" + entranceBottom);

        int centerX = pos.getX();
        int centerZ = pos.getZ();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        // Removed rimBlockState and rimUnderBlockState as they are no longer used

        // Iterate bounding box
        // Check winding offset to ensure we cover enough area
        // Removed maxCheckRad because it caused chunk cutoffs by limiting iteration bounds

        boolean hit = false;
        int carvedCount = 0;

        // This loop structure is complex for a carver. Usually carvers iterate XYZ.
        // But here we iterate Y then XZ to shape the vertical hole.

        for (int y = entranceTop; y >= entranceBottom; y--) {
            double depthProgress = (double) (entranceTop - y) / (entranceTop - entranceBottom);

            // Winding (Curve)
            double windingX = Math.sin(depthProgress * 4.0 + centerX * 0.1) * 8.0; // Increased winding
            double windingZ = Math.cos(depthProgress * 3.5 + centerZ * 0.1) * 8.0;

            double radiusAtY = Mth.lerp(depthProgress, maxRadius, minRadius);

            // Wobble/Noise
            double wobble = Math.sin(centerX * 0.2 + y * 0.1) * Math.cos(centerZ * 0.2 + y * 0.1) * 1.5;
            double extraNoise = originRandom.nextFloat() * 1.5;

            radiusAtY += wobble + extraNoise;

            for (int worldX = chunk.getPos().getMinBlockX(); worldX <= chunk.getPos().getMaxBlockX(); worldX++) {
                for (int worldZ = chunk.getPos().getMinBlockZ(); worldZ <= chunk.getPos().getMaxBlockZ(); worldZ++) {
                    int x = worldX - centerX;
                    int z = worldZ - centerZ;

                    mutablePos.set(worldX, y, worldZ);

                    double offsetX = x - windingX;
                    double offsetZ = z - windingZ;
                    double dist = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

                    if (dist < radiusAtY) {
                        // Carve Air
                        BlockState blockState = chunk.getBlockState(mutablePos);
                        if (this.canReplaceBlock(config, blockState)) {
                            if (y < -105) {
                                continue; // Protect aether rivers
                            }
                            // Manual carving to avoid using the context (which has a null generator)
                            chunk.setBlockState(mutablePos, Blocks.CAVE_AIR.defaultBlockState(), false);
                            if (carvingMask != null) {
                                carvingMask.set(mutablePos.getX() & 15, mutablePos.getY(), mutablePos.getZ() & 15);
                            }
                            hit = true;
                            carvedCount++;
                        }
                    }
                }
            }
        }

        return hit;
    }
}
