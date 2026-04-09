package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
 * Generates a mushroom-shaped stone formation on the CAVE FLOOR of the Elysian Abyss.
 * Consists of a 5x5 stem pillar, a solid circular cap, and an aether pool on top.
 * Also carves a shallow aether river ring around the base.
 *
 * Critical constraints:
 * - ONLY generates on cave floors (requires both solid below AND ceiling above)
 * - ONLY generates within the elysian_abyss biome
 * - Mushroom cap must fit below the cave ceiling
 * - Rivers never destroy structures or mushroom blocks
 */
public class StoneMushRoomFeature extends Feature<StoneMushRoomConfig> {

    private static final ResourceKey<net.minecraft.world.level.biome.Biome> ELYSIAN_ABYSS_KEY =
            ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));

    // Safety bound: mushroom floor must be below this Y to ensure we're in the cave
    private static final int MAX_FLOOR_Y = -80;

    public StoneMushRoomFeature(Codec<StoneMushRoomConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<StoneMushRoomConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rng = context.random();
        StoneMushRoomConfig config = context.config();

        // ── 0. BIOME CHECK ──────────────────────────────────────────────────
        // Only generate in the Elysian Abyss biome
        if (!level.getBiome(origin).is(ELYSIAN_ABYSS_KEY)) {
            return false;
        }

        // ── 1. FIND CAVE FLOOR ──────────────────────────────────────────────
        // Search downward from origin for an air-over-solid boundary
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        boolean foundFloor = false;

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

        // ── 2. VALIDATE CAVE CONTEXT ────────────────────────────────────────
        // 2a. Safety: floor must be well below the surface
        if (origin.getY() > MAX_FLOOR_Y) return false;

        // 2b. Verify there's a ceiling above (solid block within 120 blocks up)
        //     This prevents placement on the open surface or very tall canyons
        int ceilingY = -1;
        for (int y = origin.getY() + 1; y <= origin.getY() + 120; y++) {
            mPos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(mPos);
            if (!state.isAir() && !state.is(Blocks.CAVE_AIR) && !state.canBeReplaced()) {
                ceilingY = y;
                break;
            }
        }
        if (ceilingY == -1) return false; // No ceiling found = not inside a cave

        // 2c. Avoid stacking on top of other mushroom formations
        if (level.getBlockState(origin.below()).is(ModBlocks.HARDENED_MANASHROOM.get())) return false;

        // ── 3. CALCULATE DIMENSIONS ─────────────────────────────────────────
        int stemHeight = config.minHeight + rng.nextInt(config.maxHeight - config.minHeight + 1);
        int capRadius = 6 + rng.nextInt(4);
        int capThickness = 4;
        int capInnerRadius = capRadius - 2;

        // 3a. Verify the mushroom fits below the ceiling
        int totalHeight = stemHeight + capThickness + 2; // +2 for buffer
        int availableHeight = ceilingY - origin.getY();
        if (totalHeight > availableHeight) {
            // Shrink stem to fit, minimum stem height of 8
            stemHeight = Math.max(8, availableHeight - capThickness - 2);
            if (stemHeight < 8) return false;
        }

        // ── 4. GENERATE STEM (5x5 pillar) ───────────────────────────────────
        BlockState mushBlock = ModBlocks.HARDENED_MANASHROOM.get().defaultBlockState();
        for (int y = 0; y < stemHeight; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    level.setBlock(origin.offset(x, y, z), mushBlock, 3);
                }
            }
        }

        // ── 5. GENERATE CAP ────────────────────────────────────────────────
        // capY is the ABSOLUTE Y where the cap starts
        int capY = origin.getY() + stemHeight;
        for (int y = 0; y < capThickness; y++) {
            for (int dx = -capRadius; dx <= capRadius; dx++) {
                for (int dz = -capRadius; dz <= capRadius; dz++) {
                    double distSq = dx * dx + dz * dz;
                    if (distSq <= capRadius * capRadius) {
                        // Use absolute positioning — NOT origin.offset() which would double the Y
                        BlockPos p = new BlockPos(origin.getX() + dx, capY + y, origin.getZ() + dz);

                        // Solid unless it's the center top pool area
                        if (y < capThickness - 1 || distSq > capInnerRadius * capInnerRadius) {
                            level.setBlock(p, mushBlock, 3);
                        } else {
                            // Top layer center indentation for Aether pool
                            level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        // ── 6. AETHER POOL (on top of cap) ──────────────────────────────────
        BlockState aetherBlock = ModFluids.AETHER_BLOCK.get().defaultBlockState();
        for (int dx = -(capInnerRadius - 1); dx <= capInnerRadius - 1; dx++) {
            for (int dz = -(capInnerRadius - 1); dz <= capInnerRadius - 1; dz++) {
                if (dx * dx + dz * dz < capInnerRadius * capInnerRadius) {
                    // Absolute positioning for pool as well
                    BlockPos poolPos = new BlockPos(origin.getX() + dx, capY + capThickness - 1, origin.getZ() + dz);
                    level.setBlock(poolPos, aetherBlock, 3);
                }
            }
        }

        // ── 7. AETHER RIVER RING ────────────────────────────────────────────
        carveAetherRing(level, origin, rng, capRadius);

        return true;
    }

    /**
     * Carves a shallow aether river ring around the mushroom base.
     * The ring is a wobbling circle carved 2 blocks into the floor and filled with aether.
     * 
     * Key safety: protects mushroom blocks and structures, only carves in cave context.
     */
    private void carveAetherRing(WorldGenLevel level, BlockPos origin, RandomSource rng, int mushroomCapRadius) {
        BlockState aether = ModFluids.AETHER_BLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // ── NEIGHBOR DETECTION (avoid carving into adjacent mushrooms) ────────
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
        int ringDist = mushroomCapRadius + 4 + rng.nextInt(4); // Start ring outside the cap
        int ringWidth = 3 + rng.nextInt(2);
        int riverDepth = 2; // Shallow: only 2 blocks deep
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

                int floorY = findCaveFloor(level, riverX, riverZ, origin.getY(), pos);
                if (floorY == -999) continue;

                // ── SHALLOW TRENCH CARVING ──
                // Only clear 1 block above floor and carve 2 blocks into the ground
                // This prevents destroying structures above the river
                for (int ay = -riverDepth; ay <= 1; ay++) {
                    pos.set(riverX, floorY + ay, riverZ);
                    BlockState s = level.getBlockState(pos);

                    // PROTECT mushrooms and important structure blocks
                    if (isProtectedBlock(s)) continue;

                    level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 3);
                }

                // ── FILL WITH AETHER ──
                // Fill the trench with aether (at and below floor level)
                for (int dy = 0; dy < riverDepth; dy++) {
                    pos.set(riverX, floorY - dy, riverZ);
                    BlockState cur = level.getBlockState(pos);
                    if (cur.is(Blocks.BEDROCK)) continue;
                    if (isProtectedBlock(cur)) continue;
                    level.setBlock(pos, aether, 3);
                }
            }
        }
    }

    /**
     * Finds the cave floor at the given XZ position.
     * Returns the Y of the first air block above solid ground,
     * but ONLY if there's also a ceiling above (confirming cave context).
     */
    private int findCaveFloor(WorldGenLevel level, int x, int z, int fallbackY, BlockPos.MutableBlockPos pos) {
        for (int y = fallbackY + 12; y >= fallbackY - 16; y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.CAVE_AIR)) {
                pos.set(x, y - 1, z);
                BlockState below = level.getBlockState(pos);
                if (!below.isAir() && !below.is(Blocks.BEDROCK) && !below.canBeReplaced()
                        && !below.is(ModBlocks.HARDENED_MANASHROOM.get())) {

                    // Verify cave context: check there's a ceiling, not open sky
                    boolean hasCeiling = false;
                    for (int cy = y + 1; cy <= y + 80; cy++) {
                        pos.set(x, cy, z);
                        BlockState ceilState = level.getBlockState(pos);
                        if (!ceilState.isAir() && !ceilState.is(Blocks.CAVE_AIR)) {
                            hasCeiling = true;
                            break;
                        }
                    }
                    if (hasCeiling) return y;
                }
            }
        }
        return -999;
    }

    /**
     * Checks if a block should be protected from river carving.
     * This prevents rivers from destroying mushroom formations, structure blocks, etc.
     */
    private boolean isProtectedBlock(BlockState state) {
        return state.is(ModBlocks.HARDENED_MANASHROOM.get())
                || state.is(Blocks.BEDROCK);
    }
}
