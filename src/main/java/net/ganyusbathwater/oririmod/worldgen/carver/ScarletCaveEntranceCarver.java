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
    private static final ResourceKey<Biome> SCARLET_PLAINS_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_plains"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));

    public ScarletCaveEntranceCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
        net.ganyusbathwater.oririmod.OririMod.LOGGER.info("ScarletCaveEntranceCarver Constructed!");
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
                .info("ScarletCaveEntranceCarver carving chunk at " + chunk.getPos());
        // We only care about the center of the chunk for the "Entrance" logic
        BlockPos pos = chunk.getPos().getMiddleBlockPosition(0);
        int surfaceY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX() & 15, pos.getZ() & 15);
        net.ganyusbathwater.oririmod.OririMod.LOGGER.info("DEBUG_SCARLET: surfaceY at " + pos + " is " + surfaceY);

        if (surfaceY < -64) {
            net.ganyusbathwater.oririmod.OririMod.LOGGER.info("DEBUG_SCARLET: surfaceY too low, aborting");
            return false;
        }

        // Check for liquids (Water, Lava, etc.) in the entrance area to avoid cutting
        // into ponds
        int checkRadius = 8;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int z = -checkRadius; z <= checkRadius; z++) {
                checkPos.set(pos.getX() + x, surfaceY, pos.getZ() + z);
                if (chunk.getBlockState(checkPos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                    net.ganyusbathwater.oririmod.OririMod.LOGGER
                            .info("DEBUG_SCARLET: Aborting carve due to liquid at " + checkPos);
                    return false;
                }
                // Check one block below just in case surfaceY is slightly off or liquid is
                // recessed
                checkPos.set(pos.getX() + x, surfaceY - 1, pos.getZ() + z);
                if (chunk.getBlockState(checkPos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                    net.ganyusbathwater.oririmod.OririMod.LOGGER
                            .info("DEBUG_SCARLET: Aborting carve due to liquid at " + checkPos);
                    return false;
                }
            }
        }

        int maxRadius = 8;
        int minRadius = 4;
        int entranceTop = surfaceY;
        int entranceBottom = -16 + random.nextInt(32); // Random bottom between -16 and 16

        // Ensure valid range
        if (entranceBottom >= entranceTop)
            entranceBottom = entranceTop - 10;

        net.ganyusbathwater.oririmod.OririMod.LOGGER
                .info("DEBUG_SCARLET: Carving from Y=" + entranceTop + " down to Y=" + entranceBottom);

        int centerX = pos.getX();
        int centerZ = pos.getZ();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        // Removed rimBlockState and rimUnderBlockState as they are no longer used

        // Iterate bounding box
        // Check winding offset to ensure we cover enough area
        int maxWindingOffset = 6;
        int maxCheckRad = maxRadius + maxWindingOffset + 2;

        boolean hit = false;
        int carvedCount = 0;

        // This loop structure is complex for a carver. Usually carvers iterate XYZ.
        // But here we iterate Y then XZ to shape the vertical hole.

        for (int y = entranceTop; y >= entranceBottom; y--) {
            double depthProgress = (double) (entranceTop - y) / (entranceTop - entranceBottom);

            // Winding (Curve)
            double windingX = Math.sin(depthProgress * 4.0 + centerX * 0.1) * 6.0;
            double windingZ = Math.cos(depthProgress * 3.5 + centerZ * 0.1) * 6.0;

            double radiusAtY = Mth.lerp(depthProgress, maxRadius, minRadius);

            // Wobble/Noise
            double wobble = Math.sin(centerX * 0.2 + y * 0.1) * Math.cos(centerZ * 0.2 + y * 0.1) * 1.5;
            double extraNoise = random.nextFloat() * 1.5;

            radiusAtY += wobble + extraNoise;

            for (int x = -maxCheckRad; x <= maxCheckRad; x++) {
                for (int z = -maxCheckRad; z <= maxCheckRad; z++) {
                    int worldX = centerX + x;
                    int worldZ = centerZ + z;

                    // Standard carver boundary check (approximate)
                    if (chunk.getPos().x != (worldX >> 4) || chunk.getPos().z != (worldZ >> 4)) {
                        continue;
                    }

                    mutablePos.set(worldX, y, worldZ);

                    double offsetX = x - windingX;
                    double offsetZ = z - windingZ;
                    double dist = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

                    if (dist < radiusAtY) {
                        // Carve Air
                        BlockState blockState = chunk.getBlockState(mutablePos);
                        if (this.canReplaceBlock(config, blockState)) {
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
