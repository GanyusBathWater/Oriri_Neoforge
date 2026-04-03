package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Stone Mushroom Formation Feature with Integrated Aether Rivers.
 *
 * Generates a mushroom-shaped stone formation and carves an Aether River ring around it.
 */
public class StoneMushRoomFeature extends Feature<StoneMushRoomConfig> {

    public StoneMushRoomFeature(Codec<StoneMushRoomConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<StoneMushRoomConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rng = context.random();
        StoneMushRoomConfig config = context.config();

        // ── 1. GROUNDING ON FLAT FLOOR ──────────────────────────────────
        // With the ChunkGenerator force-flattening, we mostly expect floor at Y=-115.
        // We still search to be safe against biome edges.
        boolean foundFloor = false;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        
        int startY = origin.getY() + 32;
        int endY = origin.getY() - 64;
        
        for (int y = startY; y >= endY; y--) {
            mPos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(mPos);
            BlockState below = level.getBlockState(mPos.below());
            
            if ((state.isAir() || state.is(Blocks.CAVE_AIR)) && 
                (!below.isAir() && !below.is(Blocks.CAVE_AIR) && !below.is(Blocks.BEDROCK) && !below.canBeReplaced())) {
                
                origin = mPos.immutable();
                foundFloor = true;
                break;
            }
        }
        
        if (!foundFloor) return false;
        
        // Avoid stacking
        if (level.getBlockState(origin.below()).is(ModBlocks.HARDENED_MANASHROOM.get())) return false;

        // ── Mushroom dimensions ──────────────────────────────────────────
        int stemHeight = config.minHeight + rng.nextInt(config.maxHeight - config.minHeight + 1);
        int capRadius = 6 + rng.nextInt(4); // Increased radius
        int capThickness = 4; // Substantially thicker
        int capInnerRadius = capRadius - 2;

        // ── 1. GENERATE BULKIER STEM (5x5) ───────────────────────────────
        BlockState mushBlock = ModBlocks.HARDENED_MANASHROOM.get().defaultBlockState();
        for (int y = 0; y < stemHeight; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    level.setBlock(origin.offset(x, y, z), mushBlock, 3);
                }
            }
        }

        // ── 2. GENERATE SOLID CAP ────────────────────────────────────────
        int capY = origin.getY() + stemHeight;
        for (int y = 0; y < capThickness; y++) {
            for (int dx = -capRadius; dx <= capRadius; dx++) {
                for (int dz = -capRadius; dz <= capRadius; dz++) {
                    double distSq = dx * dx + dz * dz;
                    if (distSq <= capRadius * capRadius) {
                        BlockPos p = origin.offset(dx, capY + y, dz);
                        
                        // Solid unless it's the center top pool
                        if (y < capThickness - 1 || distSq > capInnerRadius * capInnerRadius) {
                            level.setBlock(p, mushBlock, 3);
                        } else {
                            // Top layer center indentation for Aether
                            level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        // ── 3. AETHER POOL ───────────────────────────────────────────────
        BlockState aetherBlock = ModFluids.AETHER_BLOCK.get().defaultBlockState();
        for (int dx = -(capInnerRadius - 1); dx <= capInnerRadius - 1; dx++) {
            for (int dz = -(capInnerRadius - 1); dz <= capInnerRadius - 1; dz++) {
                if (dx * dx + dz * dz < capInnerRadius * capInnerRadius) {
                    BlockPos poolPos = origin.offset(dx, capY + capThickness - 1, dz);
                    level.setBlock(poolPos, aetherBlock, 3);
                }
            }
        }

        // ── 4. INTEGRATED AETHER RIVER RING ───────────────────────────────
        carveAetherRing(level, origin, rng);

        return true;
    }

    private void carveAetherRing(WorldGenLevel level, BlockPos origin, RandomSource rng) {
        BlockState aether = ModFluids.AETHER_BLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // ── NEIGHBOR DETECTION ───────────────────────────────────────────
        List<BlockPos> neighbors = new ArrayList<>();
        int neighborScan = 52;
        for (int dx = -neighborScan; dx <= neighborScan; dx += 2) {
            for (int dz = -neighborScan; dz <= neighborScan; dz += 2) {
                if (Math.abs(dx) < 8 && Math.abs(dz) < 8) continue;
                for (int dy = -30; dy <= 30; dy += 6) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockState(pos).is(ModBlocks.HARDENED_MANASHROOM.get())) {
                        neighbors.add(pos.immutable());
                        break;
                    }
                }
            }
        }

        // Ring parameters
        int ringDist = 10 + rng.nextInt(6);
        int ringWidth = 3 + rng.nextInt(2); // Slightly wider rivers
        int riverDepth = 5; 
        double phase = rng.nextDouble() * Math.PI * 2.0;

        for (int angleDeg = 0; angleDeg < 360; angleDeg += 1) {
            double angleRad = Math.toRadians(angleDeg);
            double wobble = Math.sin(angleRad * 2.3 + phase) * 3.5 + Math.cos(angleRad * 1.9 + phase * 1.5) * 2.0;
            double r = ringDist + wobble;

            for (double offset = -ringWidth / 2.0; offset <= ringWidth / 2.0; offset += 0.75) {
                int riverX = (int) Math.round(origin.getX() + (r + offset) * Math.cos(angleRad));
                int riverZ = (int) Math.round(origin.getZ() + (r + offset) * Math.sin(angleRad));

                // ── COLLISION AVOIDANCE ──
                boolean excluded = false;
                for (BlockPos neighbor : neighbors) {
                    double distSq = Math.pow(riverX - neighbor.getX(), 2) + Math.pow(riverZ - neighbor.getZ(), 2);
                    if (distSq < 16.0 * 16.0) { 
                        excluded = true;
                        break;
                    }
                }
                if (excluded) continue;

                int floorY = findAbyssFloor(level, riverX, riverZ, origin.getY(), pos);
                if (floorY == -999) continue; 
                
                // 1. Trench Carver: Cut physically into the ground
                // We clear from floorY - 2 upwards to create space.
                for (int ay = -2; ay <= 12; ay++) {
                    pos.set(riverX, floorY + ay, riverZ);
                    BlockState s = level.getBlockState(pos);
                    
                    // PROTECT MUSHROOMS
                    if (s.is(ModBlocks.HARDENED_MANASHROOM.get())) continue;

                    if (ay <= 0 || !s.isAir() || s.is(Blocks.MOSS_BLOCK)) {
                        level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 3);
                    }
                }

                // 2. Fill Channel: Place Aether
                for (int dy = 1; dy <= riverDepth; dy++) {
                    pos.set(riverX, floorY - dy, riverZ);
                    BlockState cur = level.getBlockState(pos);
                    if (cur.is(Blocks.BEDROCK)) continue;
                    if (cur.is(ModBlocks.HARDENED_MANASHROOM.get())) continue;
                    level.setBlock(pos, aether, 3);
                }
            }
        }
    }

    private int findAbyssFloor(WorldGenLevel level, int x, int z, int fallbackY, BlockPos.MutableBlockPos pos) {
        for (int y = fallbackY + 12; y >= fallbackY - 16; y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.CAVE_AIR)) {
                pos.set(x, y - 1, z);
                BlockState below = level.getBlockState(pos);
                if (!below.isAir() && !below.is(Blocks.BEDROCK) && !below.canBeReplaced() && !below.is(ModBlocks.HARDENED_MANASHROOM.get())) return y;
            }
        }
        return -999;
    }
}
