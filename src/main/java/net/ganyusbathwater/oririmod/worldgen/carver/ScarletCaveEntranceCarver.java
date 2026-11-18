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

import java.util.function.Function;

public class ScarletCaveEntranceCarver extends WorldCarver<CaveCarverConfiguration> {

    // Biome keys for scarlet biomes
    private static final ResourceKey<Biome> SCARLET_SWAMP_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_swamp"));
    private static final ResourceKey<Biome> SCARLET_FOREST_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_forest"));
    private static final ResourceKey<Biome> SCARLET_CAVES_KEY = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scarlet_caves"));

    public ScarletCaveEntranceCarver(Codec<CaveCarverConfiguration> codec) {
        super(codec);
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
                
        // 1. Origin Chunk Strict Horizontal Bounding + Biome Verification:
        // Max winding is 6 and max radius is 13.5, so any block carved by chunkPos lies within +-20 blocks of (midX, midZ).
        int midX = chunkPos.getMiddleBlockX();
        int midZ = chunkPos.getMiddleBlockZ();
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();
        if (chunkMaxX < midX - 20 || chunkMinX > midX + 20 || chunkMaxZ < midZ - 20 || chunkMinZ > midZ + 20) {
            return false;
        }

        // Fast biome pre-check before sampling surface height
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Holder<Biome> originBiome = biomeAccessor.apply(mutablePos.set(midX, 80, midZ));
        if (!originBiome.is(SCARLET_SWAMP_KEY) && !originBiome.is(SCARLET_FOREST_KEY)) {
            return false;
        }

        int surfaceY = chunk.getPos().equals(chunkPos)
                ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, midX & 15, midZ & 15)
                : context.getMinGenY() >= -128
                    ? net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource.sampleStaticSurfaceHeight(midX, midZ)
                    : 72;
        if (surfaceY < 65) {
            return false; // Strictly protect water bodies, beaches, and lowlands from flooding
        }

        originBiome = biomeAccessor.apply(mutablePos.set(midX, surfaceY, midZ));
        if (!originBiome.is(SCARLET_SWAMP_KEY) && !originBiome.is(SCARLET_FOREST_KEY)) {
            return false;
        }

        long originSeed = (chunkPos.x * 341873128712L) ^ (chunkPos.z * 132897987541L);
        RandomSource originRandom = RandomSource.create(originSeed);

        int maxRadius = 10;
        int minRadius = 5;
        // Start flush with the actual surface terrain (never punch a void into the sky at Y=150!)
        int entranceTop = surfaceY + 1;
        int entranceBottom = Math.max(-40, -24 + originRandom.nextInt(18));
        int minBuildY = chunk.getMinBuildHeight();

        int centerX = midX;
        int centerZ = midZ;
        BlockState caveAir = Blocks.CAVE_AIR.defaultBlockState();
        BlockState abyssalBasalt = ModBlocks.ABYSSAL_BASALT.get().defaultBlockState();
        boolean hit = false;

        for (int y = entranceTop; y >= entranceBottom; y--) {
            double depthProgress = (double) (entranceTop - y) / (entranceTop - entranceBottom);

            // Winding (Curve)
            double windingX = Math.sin(depthProgress * 4.0 + centerX * 0.1) * 6.0;
            double windingZ = Math.cos(depthProgress * 3.5 + centerZ * 0.1) * 6.0;

            double radiusAtY = Mth.lerp(depthProgress, maxRadius, minRadius);

            // Smooth organic rock irregularity (no random horizontal per-Y shelf jitter or vertical biome cuts)
            double wobble = Math.sin(centerX * 0.14 + y * 0.22) * Math.cos(centerZ * 0.14 + y * 0.19) * 1.1;
            if (depthProgress > 0.78) {
                double bottomTaper = 1.0 - (depthProgress - 0.78) / 0.22;
                radiusAtY *= Mth.clamp(bottomTaper, 0.25, 1.0);
            }

            radiusAtY += wobble;
            if (y < -48 || y <= minBuildY || radiusAtY <= 0.8) {
                continue;
            }

            double tunnelCenterX = centerX + windingX;
            double tunnelCenterZ = centerZ + windingZ;
            int minWX = Math.max(chunkMinX, Mth.floor(tunnelCenterX - radiusAtY));
            int maxWX = Math.min(chunkMaxX, Mth.ceil(tunnelCenterX + radiusAtY));
            int minWZ = Math.max(chunkMinZ, Mth.floor(tunnelCenterZ - radiusAtY));
            int maxWZ = Math.min(chunkMaxZ, Mth.ceil(tunnelCenterZ + radiusAtY));
            if (minWX > maxWX || minWZ > maxWZ) {
                continue;
            }

            double radiusSq = radiusAtY * radiusAtY;

            for (int worldX = minWX; worldX <= maxWX; worldX++) {
                double offsetX = worldX - tunnelCenterX;
                double offsetXSq = offsetX * offsetX;
                if (offsetXSq >= radiusSq) {
                    continue;
                }
                for (int worldZ = minWZ; worldZ <= maxWZ; worldZ++) {
                    double offsetZ = worldZ - tunnelCenterZ;
                    if (offsetXSq + offsetZ * offsetZ >= radiusSq) {
                        continue;
                    }

                    mutablePos.set(worldX, y, worldZ);

                    // Once an entrance originates inside mainland Scarlet Forest/Swamp, let its natural
                    // tapered cylinder finish smoothly instead of slicing flat vertical walls on 4x4 biome edges.
                    BlockState blockState = chunk.getBlockState(mutablePos);
                    if (blockState.is(Blocks.WATER) || blockState.is(Blocks.SAND) || blockState.is(Blocks.GRAVEL)
                            || blockState.is(Blocks.MUD) || blockState.is(Blocks.CLAY) || blockState.is(Blocks.COARSE_DIRT)
                            || blockState.is(abyssalBasalt.getBlock()) || blockState.is(ModBlocks.ABYSS_SAND.get())) {
                        continue; // Never breach or carve open ocean, beach sand/gravel, or abyssal seabed
                    }

                    if (this.canReplaceBlock(config, blockState)) {
                        if (net.ganyusbathwater.oririmod.worldgen.ElderwoodsCaveAquifer
                                .hasAdjacentFluidInChunk(chunk, worldX, y, worldZ, 1, 1)) {
                            continue; // Never breach aquifer water/lava walls
                        }
                        chunk.setBlockState(mutablePos, caveAir, false);
                        if (carvingMask != null) {
                            carvingMask.set(worldX & 15, y, worldZ & 15);
                        }
                        hit = true;
                    }
                }
            }
        }

        return hit;
    }
}
