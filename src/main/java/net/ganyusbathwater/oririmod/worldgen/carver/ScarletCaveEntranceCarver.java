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
                
        // 1. Origin Chunk Strict Biome Verification:
        // Scarlet Cave Entrances MUST strictly originate on mainland Scarlet biomes (never on islands, beaches, or oceans)!
        int midX = chunkPos.getMiddleBlockX();
        int midZ = chunkPos.getMiddleBlockZ();
        int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, midX & 15, midZ & 15);
        if (surfaceY < 65) {
            return false; // Strictly protect water bodies, beaches, and lowlands from flooding
        }

        BlockPos surfacePos = new BlockPos(midX, surfaceY, midZ);
        Holder<Biome> originBiome = biomeAccessor.apply(surfacePos);
        if (!originBiome.is(SCARLET_SWAMP_KEY) && !originBiome.is(SCARLET_FOREST_KEY)) {
            return false;
        }

        long originSeed = (chunkPos.x * 341873128712L) ^ (chunkPos.z * 132897987541L);
        RandomSource originRandom = RandomSource.create(originSeed);

        int maxRadius = 10;
        int minRadius = 5;
        // Start flush with the actual surface terrain (never punch a void into the sky at Y=150!)
        int entranceTop = surfaceY + 1;
        int entranceBottom = -16 + originRandom.nextInt(24);

        int centerX = midX;
        int centerZ = midZ;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        boolean hit = false;

        for (int y = entranceTop; y >= entranceBottom; y--) {
            double depthProgress = (double) (entranceTop - y) / (entranceTop - entranceBottom);

            // Winding (Curve)
            double windingX = Math.sin(depthProgress * 4.0 + centerX * 0.1) * 6.0;
            double windingZ = Math.cos(depthProgress * 3.5 + centerZ * 0.1) * 6.0;

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

                    // Strictly verify that the target position is within Scarlet territory!
                    // Prohibits cutting into beaches, oceans, islands, elderwoods, or highlands!
                    Holder<Biome> targetBiome = biomeAccessor.apply(mutablePos);
                    if (!targetBiome.is(SCARLET_SWAMP_KEY) && !targetBiome.is(SCARLET_FOREST_KEY) && !targetBiome.is(SCARLET_CAVES_KEY)) {
                        continue;
                    }

                    double offsetX = x - windingX;
                    double offsetZ = z - windingZ;
                    double dist = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);

                    if (dist < radiusAtY) {
                        BlockState blockState = chunk.getBlockState(mutablePos);
                        if (blockState.is(Blocks.WATER) || blockState.is(Blocks.SAND) || blockState.is(Blocks.GRAVEL)
                                || blockState.is(ModBlocks.ABYSSAL_BASALT.get()) || blockState.is(ModBlocks.ABYSS_SAND.get())) {
                            continue; // Never breach or carve open ocean, beach sand/gravel, or abyssal seabed
                        }

                        if (this.canReplaceBlock(config, blockState)) {
                            if (y < -105) {
                                continue; // Protect aether rivers
                            }
                            chunk.setBlockState(mutablePos, Blocks.CAVE_AIR.defaultBlockState(), false);
                            if (carvingMask != null) {
                                carvingMask.set(mutablePos.getX() & 15, mutablePos.getY(), mutablePos.getZ() & 15);
                            }
                            
                            // Check if adjacent to liquid inside this chunk to schedule fluid update
                            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                                int nx = mutablePos.getX() + dir.getStepX();
                                int ny = mutablePos.getY() + dir.getStepY();
                                int nz = mutablePos.getZ() + dir.getStepZ();
                                if (nx >= chunk.getPos().getMinBlockX() && nx <= chunk.getPos().getMaxBlockX() &&
                                    nz >= chunk.getPos().getMinBlockZ() && nz <= chunk.getPos().getMaxBlockZ() &&
                                    ny >= chunk.getMinBuildHeight() && ny < chunk.getMaxBuildHeight()) {
                                    
                                    neighborPos.set(nx, ny, nz);
                                    net.minecraft.world.level.material.FluidState fState = chunk.getFluidState(neighborPos);
                                    if (!fState.isEmpty()) {
                                        chunk.markPosForPostprocessing(neighborPos);
                                        chunk.markPosForPostprocessing(mutablePos);
                                        chunk.getFluidTicks().schedule(new net.minecraft.world.ticks.ScheduledTick<>(fState.getType(), new BlockPos(nx, ny, nz), 0, 0));
                                    }
                                }
                            }
                            
                            hit = true;
                        }
                    }
                }
            }
        }

        return hit;
    }
}
