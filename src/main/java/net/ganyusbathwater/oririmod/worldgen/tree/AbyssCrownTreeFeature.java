package net.ganyusbathwater.oririmod.worldgen.tree;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.UpgradedSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AbyssCrownTreeFeature extends Feature<AbyssCrownTreeConfig> {
    private static final ResourceKey<Biome> ELYSIAN_ABYSS_KEY = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));

    public AbyssCrownTreeFeature(Codec<AbyssCrownTreeConfig> codec) {
        super(codec);
    }

    private boolean isValidAnchor(BlockState state) {
        return !state.isAir() && (state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.MOSS_BLOCK) || state.is(BlockTags.DIRT));
    }

    private boolean canReplaceState(BlockState state, boolean forcingGrowth) {
        if (forcingGrowth) {
            return state.isAir();
        }
        return state.isAir() || state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.LEAVES)
                || state.canBeReplaced() || state.is(Blocks.WATER);
    }

    private void placeLog(WorldGenLevel level, BlockPos pos, Direction.Axis axis, RandomSource rnd,
            BlockStateProvider provider, boolean forcingGrowth, int minY, int maxY, int flags, boolean isSapling) {
        if (pos.getY() <= minY || pos.getY() >= maxY) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (canReplaceState(existing, forcingGrowth) && !existing.is(Blocks.STONE)
                && !existing.is(Blocks.DEEPSLATE)) {
            BlockPos targetPos = isSapling ? pos.immutable() : pos;
            BlockState state = provider.getState(rnd, targetPos);
            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                state = state.setValue(RotatedPillarBlock.AXIS, axis);
            }
            level.setBlock(targetPos, state, flags);
        }
    }

    private void placeLeaf(WorldGenLevel level, BlockPos pos, RandomSource rnd,
            BlockStateProvider provider, boolean forcingGrowth, int minY, int maxY, int flags, boolean isSapling) {
        if (pos.getY() <= minY || pos.getY() >= maxY) {
            return;
        }
        if (canReplaceState(level.getBlockState(pos), forcingGrowth)) {
            BlockPos targetPos = isSapling ? pos.immutable() : pos;
            level.setBlock(targetPos, provider.getState(rnd, targetPos), flags);
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<AbyssCrownTreeConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rnd = context.random();
        AbyssCrownTreeConfig config = context.config();

        boolean isSapling = level instanceof ServerLevel;
        int placeFlags = isSapling ? 3 : 2;
        boolean forcingGrowth = UpgradedSaplingBlock.IS_FORCING_GROWTH.get();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();

        if (!isSapling) {
            // Universal Snap: Find the cavern roof from any starting height
            boolean foundCeiling = false;
            int ox = origin.getX();
            int oy = origin.getY();
            int oz = origin.getZ();
            if (level.getBlockState(origin).isAir()) {
                // Case A: Picked an air block - search UP for a valid natural ceiling
                for (int i = 1; i <= 200; i++) {
                    int py = oy + i;
                    abovePos.set(ox, py + 1, oz);
                    if (isValidAnchor(level.getBlockState(abovePos))) {
                        origin = new BlockPos(ox, py, oz);
                        foundCeiling = true;
                        break;
                    }
                }
            } else {
                // Case B: Picked a solid block - search DOWN for the first air pocket
                for (int i = 1; i <= 128; i++) {
                    int py = oy - i;
                    mPos.set(ox, py, oz);
                    if (level.getBlockState(mPos).isAir()) {
                        abovePos.set(ox, py + 1, oz);
                        if (isValidAnchor(level.getBlockState(abovePos))) {
                            origin = new BlockPos(ox, py, oz);
                            foundCeiling = true;
                        }
                        break; // Stop at the first interface
                    }
                }
            }

            if (!foundCeiling)
                return false;

            // Y-range guard: reject if ceiling attach point is outside valid cave ceiling
            // zone
            // Changed minimum Y from -70 to -20 to avoid deep Cheese Chambers.
            // Allowed up to Y=70 because the main ceiling can reach Y=35+.
            ox = origin.getX();
            oy = origin.getY();
            oz = origin.getZ();
            if (oy < -20 || oy > 70)
                return false;

            // Pillar detection: if the column below origin is solid for 30+ blocks,
            // this is the top of a structural pillar, not a cave ceiling. Reject it.
            // Trees must only grow from the underside of open cave ceilings.
            {
                int solidBelow = 0;
                for (int i = 1; i <= 35; i++) {
                    mPos.set(ox, oy - i, oz);
                    BlockState bs = level.getBlockState(mPos);
                    if (!bs.isAir() && !bs.is(Blocks.CAVE_AIR)) {
                        solidBelow++;
                    } else {
                        break; // stop at first air gap (real cave ceiling will have air below)
                    }
                }
                if (solidBelow >= 28)
                    return false; // it's a pillar top, not a ceiling
            }

            // Main Room Detection (Air Drop)
            // Trees should only spawn in the main cavern, which has a massive drop to the
            // floor.
            // We shoot a ray straight down. If we hit the floor in less than 30 blocks,
            // we are in a side cave, ravine, or worm tunnel, so we reject it.
            {
                int freeAirDrop = 0;
                for (int i = 1; i <= 35; i++) {
                    mPos.set(ox, oy - i, oz);
                    BlockState bs = level.getBlockState(mPos);
                    if (bs.isAir() || bs.is(Blocks.CAVE_AIR) || bs.is(Blocks.WATER)) {
                        freeAirDrop++;
                    } else {
                        break;
                    }
                }
                if (freeAirDrop < 30)
                    return false; // Side cave detected! Not enough headroom.
            }
            // Final Biome Check: Only generate if the snapped attachment point is in the
            // Elysian Abyss
            // This ensures trees "cover the entire ceiling" of the biome without escaping
            // into transition zones.
            Holder<Biome> biome = level.getBiome(origin);
            if (!biome.is(ELYSIAN_ABYSS_KEY)) {
                return false;
            }
        }

        // Determine variant: Small (1x1) or Big (2x2)
        int variantRoll = config.trunkRadius().sample(rnd);
        int trunkWidth = (variantRoll > 1) ? 2 : (rnd.nextInt(4) == 0 ? 2 : 1);

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        // If it's a Big (2x2) tree, verify ALL 4 anchor points are valid to prevent
        // "flying" trunks on uneven ceilings
        if (trunkWidth == 2) {
            boolean allAnchored = true;
            for (int dx = 0; dx < 2 && allAnchored; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    mPos.set(ox + dx, oy + 1, oz + dz);
                    BlockState anchorState = level.getBlockState(mPos);
                    if (isSapling) {
                        if (anchorState.isAir() || anchorState.is(Blocks.WATER)) {
                            allAnchored = false;
                            break;
                        }
                    } else if (!isValidAnchor(anchorState)) {
                        allAnchored = false;
                        break;
                    }
                }
            }
            if (!allAnchored) {
                // Fallback: If any part is in air, try to just place a 1x1 if the origin's
                // anchor is valid
                mPos.set(ox, oy + 1, oz);
                if (isSapling || isValidAnchor(level.getBlockState(mPos))) {
                    trunkWidth = 1;
                } else {
                    return false; // Total placement failure
                }
            }
        }

        int trunkHeight;
        int canopyRadius;

        if (trunkWidth == 1) {
            // Small variant: 6-10 Blocks trunk
            trunkHeight = 6 + rnd.nextInt(5);
            canopyRadius = 5 + rnd.nextInt(4);
        } else {
            // Big variant: 11-15 Blocks trunk
            trunkHeight = 11 + rnd.nextInt(5);
            canopyRadius = 8 + rnd.nextInt(5);
        }

        // Trunk
        for (int y = 0; y >= -trunkHeight; y--) {
            for (int dx = 0; dx < trunkWidth; dx++) {
                for (int dz = 0; dz < trunkWidth; dz++) {
                    mPos.set(ox + dx, oy + y, oz + dz);
                    placeLog(level, mPos, Direction.Axis.Y, rnd, config.logProvider(), forcingGrowth, minY, maxY,
                            placeFlags, isSapling);
                }
            }
        }

        // Crown setup
        int crownX = ox + (trunkWidth - 1) / 2;
        int crownY = oy - trunkHeight;
        int crownZ = oz + (trunkWidth - 1) / 2;

        // Branches start some blocks before the leaf level (higher up the trunk)
        int branchStartYOffset = 2 + rnd.nextInt(3); // 2-4 blocks from the bottom
        int branchStartY = crownY + branchStartYOffset;

        // Worm-walker branches - Increased count for stability
        int numBranches = 10 + trunkWidth * 5;
        int depth = (int) (canopyRadius * 0.75);
        double invCanopyRadius = 1.0 / canopyRadius;
        double invDepth = 1.0 / depth;

        for (int i = 0; i < numBranches; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            // Branches reach inside but don't stick out
            int branchLen = canopyRadius - 1 + rnd.nextInt(2);

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            double x = crownX;
            double y = branchStartY;
            double z = crownZ;

            Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;

            for (int step = 0; step < branchLen; step++) {
                x += dx * (0.6 + rnd.nextDouble() * 0.4);
                z += dz * (0.6 + rnd.nextDouble() * 0.4);
                y -= (0.3 + rnd.nextDouble() * 0.7);

                // Add worm wiggle
                double lateral = (rnd.nextDouble() - 0.5) * 1.5;
                x += -dz * lateral;
                z += dx * lateral;

                int bx = (int) Math.round(x);
                int by = (int) Math.round(y);
                int bz = (int) Math.round(z);

                // Tight constraint: Must stay within the ellipse profile
                double normX = (bx - crownX) * invCanopyRadius;
                double normZ = (bz - crownZ) * invCanopyRadius;
                double normY = Math.min(0, by - crownY) * invDepth;

                double distSq = normX * normX + normZ * normZ + normY * normY;

                if (distSq > 0.9) {
                    continue;
                }

                mPos.set(bx, by, bz);
                // Use Stem Provider for branches
                placeLog(level, mPos, axis, rnd, config.stemProvider(), forcingGrowth, minY, maxY, placeFlags,
                        isSapling);
            }
        }

        // Crown leaves: Half-ellipse facing downwards (flat side up)
        for (int dx = -canopyRadius; dx <= canopyRadius; dx++) {
            double normX = dx * invCanopyRadius;
            double normXSq = normX * normX;
            if (normXSq > 1.0) {
                continue;
            }
            for (int dz = -canopyRadius; dz <= canopyRadius; dz++) {
                double normZ = dz * invCanopyRadius;
                double horizDistSq = normXSq + normZ * normZ;
                if (horizDistSq > 1.0) {
                    continue;
                }
                for (int dy = 0; dy >= -depth; dy--) {
                    double normY = dy * invDepth;
                    double distSq = horizDistSq + normY * normY;

                    if (distSq <= 1.0) {
                        // Protect the ellipse shape
                        if (distSq > 0.85 && rnd.nextDouble() < (distSq - 0.85) * 6) {
                            continue;
                        }

                        mPos.set(crownX + dx, crownY + dy, crownZ + dz);
                        placeLeaf(level, mPos, rnd, config.leavesProvider(), forcingGrowth, minY, maxY, placeFlags,
                                isSapling);
                    }
                }
            }
        }

        // After successful placement, paint a localized overgrowth patch on the ceiling
        paintCeilingOvergrowth(level, origin, rnd, trunkWidth, mPos, abovePos, placeFlags, isSapling);

        return true;
    }

    /**
     * Spreads moss and hanging roots around the tree's connection point on the
     * ceiling.
     * Fixed: Now strictly verifies it only places on ceiling surfaces to prevent
     * floor leakage.
     */
    private void paintCeilingOvergrowth(WorldGenLevel level, BlockPos origin, RandomSource rnd, int trunkWidth,
            BlockPos.MutableBlockPos mPos, BlockPos.MutableBlockPos abovePos, int placeFlags, boolean isSapling) {
        int radius = 3 + rnd.nextInt(3);
        int radiusSq = radius * radius;
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        BlockState mossState = Blocks.MOSS_BLOCK.defaultBlockState();

        for (int x = -radius; x <= radius + trunkWidth; x++) {
            int xSq = x * x;
            for (int z = -radius; z <= radius + trunkWidth; z++) {
                if (xSq + z * z > radiusSq)
                    continue;

                mPos.set(ox + x, oy, oz + z);
                abovePos.set(ox + x, oy + 1, oz + z);

                // Ceiling check: Block must be solid, and block BELOW must be air
                if (isValidAnchor(level.getBlockState(abovePos)) && level.getBlockState(mPos).isAir()) {
                    // Paint the ceiling with moss
                    level.setBlock(isSapling ? abovePos.immutable() : abovePos, mossState, placeFlags);
                }
            }
        }
    }
}
