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

/**
 * Stone Mushroom Formation Feature.
 *
 * Generates a mushroom-shaped stone formation made of HARDENED_MANASHROOM:
 * - Tapered stem (wider at base, narrower toward cap)
 * - Hollow chalice cap at the top (like an inverted bowl / chalice)
 * - Aether liquid pool inside the chalice
 * - Random holes in the chalice rim so Aether flows off edges
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

        // ── Elevation & Biome Filter ───────────────────────────────────
        if (origin.getY() > -105) {
            return false; // Confined to deep abyssfloor
        }

        // Strictly check if we are in the Elysian Abyss (High Noise band)
        var biome = level.getBiome(origin);
        if (!biome.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, 
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.ganyusbathwater.oririmod.OririMod.MOD_ID, "elysian_abyss")))) {
            return false;
        }

        // ── Mushroom height ───────────────────────────────────────────────
        int stemHeight = config.minHeight + rng.nextInt(config.maxHeight - config.minHeight + 1);

        // ── Cap dimensions ────────────────────────────────────────────────
        int capRadius = 4 + rng.nextInt(5);   // 4-8 block radius
        int capThickness = 2 + rng.nextInt(2); // 2-3 block cap wall thickness
        int capInnerRadius = capRadius - capThickness;

        // ── PRE-PLACEMENT CLEARANCE CHECK ──────────────────────────────────
        // Scan the area where the CAP will be to ensure it's not inside a wall.
        // We check a circle at the top height.
        int capCenterY = origin.getY() + stemHeight;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        int solidContamination = 0;
        int totalCheckPoints = 0;

        for (int dx = -capRadius; dx <= capRadius; dx++) {
            for (int dz = -capRadius; dz <= capRadius; dz++) {
                if (dx * dx + dz * dz <= capRadius * capRadius) {
                    totalCheckPoints++;
                    checkPos.set(origin.getX() + dx, capCenterY, origin.getZ() + dz);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.isSolid() && !state.canBeReplaced()) {
                        solidContamination++;
                    }
                    
                    // Also check biome at the 4 cardinal edges of the cap
                    if (Math.abs(dx) == capRadius || Math.abs(dz) == capRadius) {
                        if (!level.getBiome(checkPos).is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, 
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.ganyusbathwater.oririmod.OririMod.MOD_ID, "elysian_abyss")))) {
                            return false; // Edge of cap is outside the abyss biome
                        }
                    }
                }
            }
        }

        // Abort if more than 5% of the cap area is obstructed by cave walls
        if (solidContamination > totalCheckPoints * 0.05) {
            return false;
        }

        // Check ground below
        BlockPos groundCheck = origin.below(1);
        BlockState groundState = level.getBlockState(groundCheck);
        if (!groundState.isCollisionShapeFullBlock(level, groundCheck)) {
            return false;
        }

        // ── 1. STEM ───────────────────────────────────────────────────────
        BlockState mushBlock = ModBlocks.HARDENED_MANASHROOM.get().defaultBlockState();

        for (int y = 0; y < stemHeight; y++) {
            double progress = (double) y / stemHeight; // 0 at base, 1 at top
            // Base radius 3, narrows to 1 at top
            int stemR = Math.max(1, (int) Math.round(3.0 - progress * 2.0));

            for (int dx = -stemR; dx <= stemR; dx++) {
                for (int dz = -stemR; dz <= stemR; dz++) {
                    if (dx * dx + dz * dz <= stemR * stemR) {
                        BlockPos p = origin.offset(dx, y, dz);
                        if (level.getBlockState(p).canBeReplaced()) {
                            level.setBlock(p, mushBlock, 3);
                        }
                    }
                }
            }
        }

        // ── 2. CHALICE CAP (rim + underside only, hollow inside) ──────────
        // The cap is 2-3 layers of mushroom block at the rim and underside.
        // Inner area is left hollow (or carved out) to form the chalice.

        int capY = stemHeight; // relative to origin

        for (int dy = 0; dy <= capThickness; dy++) {
            for (int dx = -capRadius; dx <= capRadius; dx++) {
                for (int dz = -capRadius; dz <= capRadius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    boolean inOuter = distSq <= capRadius * capRadius;
                    boolean inInner = distSq < capInnerRadius * capInnerRadius;

                    if (!inOuter) continue;

                    BlockPos p = origin.offset(dx, capY + dy, dz);

                    if (dy == 0) {
                        // Bottom cap layer: full disc (underside of mushroom)
                        if (level.getBlockState(p).canBeReplaced()) {
                            level.setBlock(p, mushBlock, 3);
                        }
                    } else {
                        // Upper layers: only rim (annulus) — leave inner open
                        if (!inInner) {
                            if (level.getBlockState(p).canBeReplaced()) {
                                level.setBlock(p, mushBlock, 3);
                            }
                        } else {
                            BlockPos innerP = origin.offset(dx, capY + dy, dz);
                            BlockState existingInner = level.getBlockState(innerP);
                            if (!existingInner.isAir() && !existingInner.is(Blocks.BEDROCK)) {
                                level.setBlock(innerP, Blocks.CAVE_AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }

        // ── 3. AETHER POOL inside chalice ─────────────────────────────────
        // Fill the inner part of the topmost cap layer with aether source blocks.
        BlockState aetherBlock = ModFluids.AETHER_BLOCK.get().defaultBlockState();

        // Determine chip positions for chipped edges (random holes in rim)
        int numChips = 3 + rng.nextInt(4); // 3-6 chips
        int[] chipAngles = new int[numChips];
        for (int i = 0; i < numChips; i++) {
            chipAngles[i] = rng.nextInt(360);
        }

        for (int dx = -(capInnerRadius - 1); dx <= capInnerRadius - 1; dx++) {
            for (int dz = -(capInnerRadius - 1); dz <= capInnerRadius - 1; dz++) {
                if (dx * dx + dz * dz < capInnerRadius * capInnerRadius) {
                    // Pool level: place aether at the cap bottom + 1
                    BlockPos poolPos = origin.offset(dx, capY + 1, dz);
                    BlockState existing = level.getBlockState(poolPos);
                    if (existing.canBeReplaced() || existing.is(Blocks.CAVE_AIR)) {
                        level.setBlock(poolPos, aetherBlock, 3);
                    }
                }
            }
        }

        // ── 4. CHIPPED EDGES in the rim ───────────────────────────────────
        // Break random sections of the rim so Aether can flow off the edge.
        for (int chip = 0; chip < numChips; chip++) {
            double angle = Math.toRadians(chipAngles[chip]);
            int chipSize = 1 + rng.nextInt(2); // 1-2 block chip

            for (int c = 0; c <= chipSize; c++) {
                int chipX = (int) Math.round(Math.cos(angle) * (capRadius - c));
                int chipZ = (int) Math.round(Math.sin(angle) * (capRadius - c));

                for (int dy = 1; dy <= capThickness; dy++) {
                    BlockPos chipPos = origin.offset(chipX, capY + dy, chipZ);
                    BlockState there = level.getBlockState(chipPos);
                    if (there.is(mushBlock.getBlock())) {
                        level.setBlock(chipPos, Blocks.CAVE_AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        return true;
    }
}
