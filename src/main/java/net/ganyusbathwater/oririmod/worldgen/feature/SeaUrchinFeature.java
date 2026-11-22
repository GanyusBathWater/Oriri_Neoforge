package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class SeaUrchinFeature extends Feature<NoneFeatureConfiguration> {

    public SeaUrchinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static class Spike {
        Vector3f dir;
        float length;
        float baseWidth;

        Spike(Vector3f dir, float length, float baseWidth) {
            this.dir = dir;
            this.length = length;
            this.baseWidth = baseWidth;
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rng = context.random();

        WorldGenRegion region = level instanceof WorldGenRegion r ? r : null;
        int centerCx = region != null ? region.getCenter().x : (origin.getX() >> 4);
        int centerCz = region != null ? region.getCenter().z : (origin.getZ() >> 4);

        // Bias origin toward chunk center (local 6..9) so 10-22 block protruding spikes
        // fit inside the 3x3 chunk write radius without flat chunk-border clipping.
        if (region != null) {
            int clampedLocalX = Mth.clamp(origin.getX() - (centerCx << 4), 6, 9);
            int clampedLocalZ = Mth.clamp(origin.getZ() - (centerCz << 4), 6, 9);
            origin = new BlockPos((centerCx << 4) + clampedLocalX, origin.getY(), (centerCz << 4) + clampedLocalZ);
        }

        // 50/50 Chance between Amethyst and Mana Crystals
        boolean isMana = rng.nextBoolean();
        BlockState crystalBlock = isMana ? ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState clusterBlock = isMana ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState() : Blocks.AMETHYST_CLUSTER.defaultBlockState();

        // Ensure we are placing it inside solid rock
        if (!level.getBlockState(origin).isSolid()) {
            boolean foundSolid = false;
            for (int i = 0; i < 5; i++) {
                origin = origin.below();
                if (level.getBlockState(origin).isSolid()) {
                    foundSolid = true;
                    break;
                }
            }
            if (!foundSolid) return false;
        }

        int maxBounding = 24;
        float noiseScale = 0.15f;
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        int minWorldY = level.getMinBuildHeight() + 1;

        int coreRadius = rng.nextInt(3) + 4; // 4 to 6 blocks core
        int numSpikes = 15 + rng.nextInt(20); // 15 to 35 spikes

        // Collect all underground structure piece boxes (Mineshafts, Trial Chambers, etc.) intersecting
        // this urchin's volume so neighbor-chunk structure carving can never sever protruding spikes.
        BoundingBox urchinBox = new BoundingBox(
                ox - maxBounding - 2, oy - maxBounding - 2, oz - maxBounding - 2,
                ox + maxBounding + 2, oy + maxBounding + 2, oz + maxBounding + 2);
        List<BoundingBox> structureObstacles = collectStructureObstacles(level, region, centerCx, centerCz, urchinBox);

        if (!structureObstacles.isEmpty()) {
            BoundingBox coreBox = new BoundingBox(
                    ox - coreRadius - 2, oy - coreRadius - 2, oz - coreRadius - 2,
                    ox + coreRadius + 2, oy + coreRadius + 2, oz + coreRadius + 2);
            for (BoundingBox obstacle : structureObstacles) {
                if (obstacle.intersects(coreBox)) {
                    return false;
                }
            }
        }

        List<Spike> spikes = new ArrayList<>();
        for (int i = 0; i < numSpikes; i++) {
            float theta = rng.nextFloat() * 2.0f * (float) Math.PI;
            float phi = (float) Math.acos(2.0f * rng.nextFloat() - 1.0f);

            float dx = (float) (Math.sin(phi) * Math.cos(theta));
            float dy = (float) (Math.cos(phi));
            float dz = (float) (Math.sin(phi) * Math.sin(theta));

            Vector3f direction = new Vector3f(dx, dy, dz).normalize();

            float length = 10.0f + rng.nextFloat() * 12.0f; // 10 to 22 blocks long
            float width = 3.0f + rng.nextFloat() * 2.5f; // 3 to 5.5 blocks wide base

            // Shorten any spike before it enters a structure corridor or deep Aether River
            // so later neighbor-chunk carving never leaves floating severed crystal tips.
            for (float t = 1.0f; t <= length; t += 1.0f) {
                int px = ox + Math.round(direction.x() * t);
                int py = oy + Math.round(direction.y() * t);
                int pz = oz + Math.round(direction.z() * t);

                if (py <= -78 && ElderwoodsChunkGenerator.getRiverIntensity(px, pz) > 0.005) {
                    length = t - 2.0f;
                    break;
                }

                if (!structureObstacles.isEmpty()) {
                    int pad = (int) Math.ceil(width * Math.max(0.0f, 1.0f - t / length));
                    boolean hitObstacle = false;
                    for (BoundingBox box : structureObstacles) {
                        if (px >= box.minX() - pad && px <= box.maxX() + pad
                                && py >= box.minY() - pad && py <= box.maxY() + pad
                                && pz >= box.minZ() - pad && pz <= box.maxZ() + pad) {
                            hitObstacle = true;
                            break;
                        }
                    }
                    if (hitObstacle) {
                        length = t - 2.0f;
                        break;
                    }
                }
            }

            if (length >= coreRadius + 2.5f) {
                spikes.add(new Spike(direction, length, width));
            }
        }
        int activeSpikes = spikes.size();

        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        int span = maxBounding * 2 + 1;
        int spanSq = span * span;
        int[] placedOffsets = new int[2048];
        int placedCount = 0;

        double maxPossibleCoreRadius = coreRadius + 2.0;

        // Phase 1: Carve volume and place solid crystals (column-first for heightmap & chunk locality)
        for (int x = -maxBounding; x <= maxBounding; x++) {
            int wx = ox + x;
            if (region != null && Math.abs((wx >> 4) - centerCx) > 1) {
                continue;
            }
            for (int z = -maxBounding; z <= maxBounding; z++) {
                int xzSq = x * x + z * z;
                if (xzSq > maxBounding * maxBounding) {
                    continue;
                }
                int wz = oz + z;
                if (region != null && Math.abs((wz >> 4) - centerCz) > 1) {
                    continue;
                }

                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz);
                int sphereMaxY = (int) Math.sqrt(maxBounding * maxBounding - xzSq);
                int minY = Math.max(-sphereMaxY, minWorldY - oy);
                int maxY = Math.min(sphereMaxY, (surfaceY - 5) - oy);
                if (minY > maxY) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    double distSq = xzSq + y * y;
                    double dist = Math.sqrt(distSq);

                    // Conservative envelope check before reading blocks or evaluating 3D FBM
                    boolean possibleCore = dist < maxPossibleCoreRadius;
                    boolean possibleSpike = false;
                    if (!possibleCore) {
                        for (int si = 0; si < activeSpikes; si++) {
                            Spike s = spikes.get(si);
                            double dot = x * s.dir.x() + y * s.dir.y() + z * s.dir.z();
                            if (dot >= -1.0 && dot < s.length) {
                                double distToAxisSq = Math.max(0.0, distSq - dot * dot);
                                double taper = 1.0 - (Math.max(0.0, dot) / s.length);
                                double maxThick = (s.baseWidth + 1.4) * taper;
                                if (distToAxisSq < maxThick * maxThick) {
                                    possibleSpike = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!possibleCore && !possibleSpike) {
                        continue;
                    }

                    int wy = oy + y;
                    if (!structureObstacles.isEmpty() && isInsideAnyBox(structureObstacles, wx, wy, wz)) {
                        continue;
                    }
                    mPos.set(wx, wy, wz);
                    BlockState currentState = level.getBlockState(mPos);
                    if (currentState.is(Blocks.BEDROCK)
                            || currentState.is(BlockTags.FEATURES_CANNOT_REPLACE)
                            || currentState.hasBlockEntity()
                            || currentState.is(Blocks.LAVA)) {
                        continue;
                    }
                    boolean wasSolid = currentState.isSolid();
                    boolean canReplace = wasSolid || currentState.isAir()
                            || currentState.is(Blocks.WATER) || currentState.canBeReplaced();
                    if (!canReplace) {
                        continue;
                    }

                    double noise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(
                            wx * noiseScale, wy * noiseScale, wz * noiseScale, 2);

                    boolean isInside = false;
                    double distortedCoreRadius = coreRadius + (noise * 2.0);
                    if (dist < distortedCoreRadius) {
                        isInside = true;
                    } else if (possibleSpike) {
                        for (int si = 0; si < activeSpikes; si++) {
                            Spike s = spikes.get(si);
                            double dot = x * s.dir.x() + y * s.dir.y() + z * s.dir.z();
                            if (dot >= -1.0 && dot < s.length) {
                                double distToAxis = Math.sqrt(Math.max(0.0, distSq - dot * dot));
                                double taper = 1.0 - (Math.max(0.0, dot) / s.length);
                                // Positive thickness floor prevents 3D noise from pinching spikes into detached tips
                                double minThickness = Math.max(0.85, s.baseWidth * 0.52) * taper;
                                double distortedThickness = Math.max(minThickness, (s.baseWidth + noise * 1.2) * taper);
                                if (distToAxis < distortedThickness) {
                                    isInside = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (isInside) {
                        // Geode Crust: only where the core embeds into existing solid rock
                        if (wasSolid && dist > distortedCoreRadius - 1.5 && dist < distortedCoreRadius + 0.5) {
                            if (rng.nextFloat() < 0.2f) {
                                level.setBlock(mPos, calcite, 2);
                            } else if (rng.nextFloat() < 0.1f) {
                                level.setBlock(mPos, smoothBasalt, 2);
                            } else {
                                level.setBlock(mPos, crystalBlock, 2);
                                if (placedCount == placedOffsets.length) {
                                    placedOffsets = java.util.Arrays.copyOf(placedOffsets, placedOffsets.length * 2);
                                }
                                placedOffsets[placedCount++] = ((x + maxBounding) * span + (y + maxBounding)) * span + (z + maxBounding);
                            }
                        } else {
                            level.setBlock(mPos, crystalBlock, 2);
                            if (placedCount == placedOffsets.length) {
                                placedOffsets = java.util.Arrays.copyOf(placedOffsets, placedOffsets.length * 2);
                            }
                            placedOffsets[placedCount++] = ((x + maxBounding) * span + (y + maxBounding)) * span + (z + maxBounding);
                        }
                    }
                }
            }
        }

        // Phase 2: Place clusters only on exposed surfaces adjacent to placed crystal blocks
        BitSet visitedAir = new BitSet((span + 2) * (span + 2) * (span + 2));
        int vSpan = span + 2;
        net.minecraft.world.level.block.Block crystalBlockType = crystalBlock.getBlock();
        Direction[] directions = Direction.values();

        for (int i = 0; i < placedCount; i++) {
            int idx = placedOffsets[i];
            int pz = (idx % span) - maxBounding;
            int py = ((idx / span) % span) - maxBounding;
            int px = (idx / spanSq) - maxBounding;

            for (Direction d : directions) {
                int ax = px + d.getStepX();
                int ay = py + d.getStepY();
                int az = pz + d.getStepZ();
                int vx = ax + maxBounding + 1;
                int vy = ay + maxBounding + 1;
                int vz = az + maxBounding + 1;
                int vIdx = (vx * vSpan + vy) * vSpan + vz;
                if (visitedAir.get(vIdx)) {
                    continue;
                }
                visitedAir.set(vIdx);

                int awx = ox + ax;
                int awy = oy + ay;
                int awz = oz + az;
                if (awy < minWorldY) {
                    continue;
                }
                if (region != null && (Math.abs((awx >> 4) - centerCx) > 1 || Math.abs((awz >> 4) - centerCz) > 1)) {
                    continue;
                }

                mPos.set(awx, awy, awz);
                BlockState airState = level.getBlockState(mPos);
                boolean isWater = airState.getFluidState().is(FluidTags.WATER);
                if (airState.isAir() || isWater) {
                    Direction attachedFace = null;
                    for (Direction nd : directions) {
                        neighborPos.set(awx + nd.getStepX(), awy + nd.getStepY(), awz + nd.getStepZ());
                        if (level.getBlockState(neighborPos).is(crystalBlockType)) {
                            attachedFace = nd;
                            break;
                        }
                    }
                    if (attachedFace != null && rng.nextFloat() < 0.04f) {
                        BlockState placedCluster = clusterBlock.setValue(
                                BlockStateProperties.FACING, attachedFace.getOpposite());
                        if (placedCluster.hasProperty(BlockStateProperties.WATERLOGGED)) {
                            placedCluster = placedCluster.setValue(BlockStateProperties.WATERLOGGED, isWater);
                        }
                        level.setBlock(mPos, placedCluster, 2);
                    }
                }
            }
        }

        return true;
    }

    private static List<BoundingBox> collectStructureObstacles(
            WorldGenLevel level, WorldGenRegion region, int centerCx, int centerCz, BoundingBox urchinBox) {
        LongSet startChunks = new LongOpenHashSet();
        for (int cx = centerCx - 1; cx <= centerCx + 1; cx++) {
            for (int cz = centerCz - 1; cz <= centerCz + 1; cz++) {
                if (region != null && !region.hasChunk(cx, cz)) {
                    continue;
                }
                startChunks.add(ChunkPos.asLong(cx, cz));
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
                if (chunk == null) {
                    continue;
                }
                for (LongSet refs : chunk.getAllReferences().values()) {
                    startChunks.addAll(refs);
                }
            }
        }

        List<BoundingBox> obstacles = List.of();
        for (long packed : startChunks) {
            int scx = ChunkPos.getX(packed);
            int scz = ChunkPos.getZ(packed);
            if (region != null && !region.hasChunk(scx, scz)) {
                continue;
            }
            ChunkAccess startChunk = level.getChunk(scx, scz, ChunkStatus.STRUCTURE_STARTS, false);
            if (startChunk == null) {
                continue;
            }
            for (StructureStart start : startChunk.getAllStarts().values()) {
                if (!start.isValid() || !start.getBoundingBox().intersects(urchinBox)) {
                    continue;
                }
                for (StructurePiece piece : start.getPieces()) {
                    BoundingBox pb = piece.getBoundingBox();
                    BoundingBox padded = new BoundingBox(
                            pb.minX() - 2, pb.minY() - 2, pb.minZ() - 2,
                            pb.maxX() + 2, pb.maxY() + 2, pb.maxZ() + 2);
                    if (padded.intersects(urchinBox)) {
                        if (obstacles.isEmpty()) {
                            obstacles = new ArrayList<>();
                        }
                        obstacles.add(padded);
                    }
                }
            }
        }
        return obstacles;
    }

    private static boolean isInsideAnyBox(List<BoundingBox> boxes, int x, int y, int z) {
        for (BoundingBox box : boxes) {
            if (box.isInside(x, y, z)) {
                return true;
            }
        }
        return false;
    }
}
