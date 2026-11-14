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
        BlockPos.MutableBlockPos auxPos = new BlockPos.MutableBlockPos();
        boolean foundFloor = false;

        int startY = origin.getY() + 32;
        int endY = origin.getY() - 64;

        for (int y = startY; y >= endY; y--) {
            mPos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(mPos);
            BlockState below = level.getBlockState(auxPos.set(origin.getX(), y - 1, origin.getZ()));

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

        // ── 3. CALCULATE DIMENSIONS ─────────────────────────────────────────
        int stemHeight = config.minHeight + rng.nextInt(config.maxHeight - config.minHeight + 1);
        int capRadius = 4 + rng.nextInt(3); // 4 to 6 blocks wide (Max radius 8 with crust, to fit in chunk bounds and prevent cutting)
        int capThickness = 4 + rng.nextInt(2); // 4 to 5 blocks thick
        int capInnerRadius = capRadius - 2;

        // 2b. Verify there's a ceiling above (solid block within 120 blocks up)
        //     This prevents placement on the open surface or very tall canyons.
        //     ALSO: Air Rise Scan ensures we are on the MAIN cavern floor, not a side-cave.
        int ceilingY = -1;
        int freeAirRise = 0;
        for (int y = origin.getY() + 1; y <= origin.getY() + 120; y++) {
            mPos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(mPos);
            if (state.isAir() || state.is(Blocks.CAVE_AIR) || state.canBeReplaced() || state.is(Blocks.WATER)) {
                if (ceilingY == -1) freeAirRise++;
            } else {
                if (ceilingY == -1) ceilingY = y;
                break;
            }
        }
        if (ceilingY == -1) return false; // No ceiling found = not inside a cave
        if (freeAirRise < stemHeight + capThickness + 2) return false; // Not enough room for this specific mushroom!

        // 2c. Avoid stacking on top of other mushroom formations (or their magma crusts)
        BlockState groundState = level.getBlockState(origin.below());
        if (groundState.is(ModBlocks.HARDENED_MANASHROOM.get()) || groundState.is(ModBlocks.AETHER_MAGMA_BLOCK.get())) {
            return false;
        }

        // 2d. Exclusion zone: reject if another mushroom is too close
        //     Scan radius set to 24 blocks to allow forests while preventing heavy cap clipping.
        int scanRadius = 12; // Reduced to match smaller mushrooms
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        outer:
        for (int sx = -scanRadius; sx <= scanRadius; sx += 4) {
            for (int sz = -scanRadius; sz <= scanRadius; sz += 4) {
                // We check a few vertical levels to catch stems or caps
                for (int sy = -5; sy <= 35; sy += 5) {
                    scanPos.set(origin.getX() + sx, origin.getY() + sy, origin.getZ() + sz);
                    BlockState state = level.getBlockState(scanPos);
                    if (state.is(ModBlocks.HARDENED_MANASHROOM.get()) || state.is(ModBlocks.AETHER_MAGMA_BLOCK.get())) {
                        double d2 = sx * sx + sz * sz;
                        if (d2 < scanRadius * scanRadius) {
                            return false; // within exclusion zone of an existing mushroom
                        }
                        break outer;
                    }
                }
            }
        }

        // 3a. Verify the mushroom fits below the ceiling
        int totalHeight = stemHeight + capThickness + 2; // +2 for buffer
        int availableHeight = ceilingY - origin.getY();
        if (totalHeight > availableHeight) {
            // Shrink stem to fit, minimum stem height of 8
            stemHeight = Math.max(8, availableHeight - capThickness - 2);
            if (stemHeight < 8) return false;
        }

        // ── 4. GENERATE STEM (5x5 pillar with Deepslate Gradient) ───────────
        BlockState mushBlock = ModBlocks.HARDENED_MANASHROOM.get().defaultBlockState();
        BlockState deepslateBlock = Blocks.DEEPSLATE.defaultBlockState();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int bottomY = 0;
                while (bottomY > -2 && level.isEmptyBlock(mPos.set(ox + x, oy + bottomY - 1, oz + z))) {
                    bottomY--;
                }

                for (int y = bottomY; y < stemHeight; y++) {
                    double progress = (double) Math.max(0, y) / stemHeight;
                    BlockState toPlace = mushBlock;
                    if (y < 0 || progress < 0.2) {
                        toPlace = deepslateBlock;
                    } else if (progress < 0.6 && rng.nextDouble() > (progress - 0.2) / 0.4) {
                        toPlace = deepslateBlock;
                    }
                    level.setBlock(mPos.set(ox + x, oy + y, oz + z), toPlace, 2);
                }
            }
        }

        // ── 5. GENERATE UMBRELLA CAP ────────────────────────────────────────
        BlockState aetherMagma = ModBlocks.AETHER_MAGMA_BLOCK.get().defaultBlockState();
        BlockState aetherLiquid = ModFluids.AETHER_BLOCK.get().defaultBlockState();
        BlockState caveAir = Blocks.CAVE_AIR.defaultBlockState();

        int capY = oy + stemHeight;
        int maxRadius = capRadius + 2;
        int edgeDrop = 2 + rng.nextInt(3);
        int bowlRadius = maxRadius - 3;
        int poolY = capThickness - 2;

        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                double distSq = dx * dx + dz * dz;
                double dist = Math.sqrt(distSq);
                if (dist > maxRadius) continue;

                double t = dist / maxRadius;
                int topY = (int) Math.round(capThickness * Math.sqrt(1 - t * t));
                int bottomY = (int) Math.round(-edgeDrop * (t * t));

                if (dist > bowlRadius && dist <= bowlRadius + 2 && topY < poolY + 1) {
                    topY = poolY + 1;
                }

                for (int y = bottomY; y <= topY; y++) {
                    mPos.set(ox + dx, capY + y, oz + dz);
                    BlockState existing = level.getBlockState(mPos);

                    if (dist <= bowlRadius && y >= poolY) {
                        if (y == poolY) {
                            if (existing.canBeReplaced() || existing.isAir() || existing.is(Blocks.WATER) || existing.is(Blocks.CAVE_AIR)) {
                                level.setBlock(mPos, aetherLiquid, 2);
                            }
                        } else if (existing.canBeReplaced() || existing.is(Blocks.WATER) || existing.is(aetherLiquid.getBlock())) {
                            level.setBlock(mPos, caveAir, 2);
                        }
                    } else if (existing.canBeReplaced() || existing.isAir() || existing.is(Blocks.WATER) || existing.is(Blocks.CAVE_AIR)) {
                        boolean isCrust = (y >= topY - 2) || (dist > maxRadius - 1.5);
                        level.setBlock(mPos, isCrust ? aetherMagma : mushBlock, 2);
                    }
                }
            }
        }

        // ── 6. AETHER FALLS (Spouts for flowing liquid) ─────────────────────
        int notchCount = 4 + rng.nextInt(3);
        for (int n = 0; n < notchCount; n++) {
            double angle = (2.0 * Math.PI / notchCount) * n + rng.nextDouble() * 0.4;
            int nx = (int) Math.round(Math.cos(angle) * (bowlRadius + 1));
            int nz = (int) Math.round(Math.sin(angle) * (bowlRadius + 1));

            BlockPos spoutPos = new BlockPos(ox + nx, capY + poolY, oz + nz);
            level.setBlock(spoutPos, aetherLiquid, 2);

            for (int clearY = poolY + 1; clearY <= capThickness + 2; clearY++) {
                level.setBlock(mPos.set(ox + nx, capY + clearY, oz + nz), caveAir, 2);
            }

            level.scheduleTick(spoutPos, ModFluids.AETHER_SOURCE.get(), 0);
        }

        return true;
    }
}
