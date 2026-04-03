package net.ganyusbathwater.oririmod.worldgen.tree;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class AbyssCrownTreeFeature extends Feature<AbyssCrownTreeConfig> {
    public AbyssCrownTreeFeature(Codec<AbyssCrownTreeConfig> codec) {
        super(codec);
    }

    private boolean isValidAnchor(BlockState state) {
        return !state.isAir() && (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK) || state.is(BlockTags.DIRT));
    }

    private boolean canReplace(WorldGenLevel level, BlockPos pos) {
        if (!level.isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.LEAVES) || state.canBeReplaced() || state.is(net.minecraft.world.level.block.Blocks.WATER))) {
            return false;
        }
        return pos.getY() > level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight();
    }

    private void placeLog(WorldGenLevel level, BlockPos pos, Direction.Axis axis, RandomSource rnd, net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider provider) {
        if (canReplace(level, pos)) {
            BlockState state = provider.getState(rnd, pos);
            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                state = state.setValue(RotatedPillarBlock.AXIS, axis);
            }
            level.setBlock(pos, state, 3);
        }
    }

    private void placeLeaf(WorldGenLevel level, BlockPos pos, RandomSource rnd, net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider provider) {
        if (canReplace(level, pos)) {
            level.setBlock(pos, provider.getState(rnd, pos), 3);
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<AbyssCrownTreeConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rnd = context.random();
        AbyssCrownTreeConfig config = context.config();

        // Universal Snap: Find the cavern roof from any starting height
        boolean foundCeiling = false;
        if (level.getBlockState(origin).isAir()) {
            // Case A: Picked an air block - search UP for a valid natural ceiling
            for (int i = 1; i <= 200; i++) {
                BlockPos potOrigin = origin.above(i);
                if (isValidAnchor(level.getBlockState(potOrigin.above()))) {
                    origin = potOrigin;
                    foundCeiling = true;
                    break;
                }
            }
        } else {
            // Case B: Picked a solid block - search DOWN for the first air pocket
            for (int i = 1; i <= 128; i++) {
                BlockPos potOrigin = origin.below(i);
                if (level.getBlockState(potOrigin).isAir()) {
                    if (isValidAnchor(level.getBlockState(potOrigin.above()))) {
                        origin = potOrigin;
                        foundCeiling = true;
                    }
                    break; // Stop at the first interface
                }
            }
        }

        if (!foundCeiling) return false;

        // Final Biome Check: Only generate if the snapped attachment point is in the Elysian Abyss
        // This ensures trees "cover the entire ceiling" of the biome without escaping into transition zones.
        var biome = level.getBiome(origin);
        if (!biome.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, 
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.ganyusbathwater.oririmod.OririMod.MOD_ID, "elysian_abyss")))) {
            return false;
        }

        // Determine variant: Small (1x1) or Big (2x2)
        int variantRoll = config.trunkRadius().sample(rnd); 
        int trunkWidth = (variantRoll > 1) ? 2 : (rnd.nextInt(4) == 0 ? 2 : 1); 

        // If it's a Big (2x2) tree, verify ALL 4 anchor points are valid to prevent "flying" trunks on uneven ceilings
        if (trunkWidth == 2) {
            boolean allAnchored = true;
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    // Check the block directly above the trunk start for each part of the 2x2
                    if (!isValidAnchor(level.getBlockState(origin.offset(dx, 1, dz)))) {
                        allAnchored = false;
                        break;
                    }
                }
            }
            if (!allAnchored) {
                // Fallback: If any part is in air, try to just place a 1x1 if the origin's anchor is valid
                if (isValidAnchor(level.getBlockState(origin.above()))) {
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
                    placeLog(level, origin.offset(dx, y, dz), Direction.Axis.Y, rnd, config.logProvider());
                }
            }
        }

        // Crown setup
        BlockPos crownCenter = origin.offset((trunkWidth - 1) / 2, -trunkHeight, (trunkWidth - 1) / 2);
        
        // Branches start some blocks before the leaf level (higher up the trunk)
        int branchStartYOffset = 2 + rnd.nextInt(3); // 2-4 blocks from the bottom
        BlockPos branchStartCenter = crownCenter.above(branchStartYOffset);

        // Worm-walker branches - Increased count for stability
        int numBranches = 10 + trunkWidth * 5; 
        int depth = (int) (canopyRadius * 0.75);

        for (int i = 0; i < numBranches; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            // Branches reach inside but don't stick out
            int branchLen = canopyRadius - 1 + rnd.nextInt(2); 
            
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            
            double x = branchStartCenter.getX();
            double y = branchStartCenter.getY();
            double z = branchStartCenter.getZ();

            for (int step = 0; step < branchLen; step++) {
                x += dx * (0.6 + rnd.nextDouble() * 0.4);
                z += dz * (0.6 + rnd.nextDouble() * 0.4);
                y -= (0.3 + rnd.nextDouble() * 0.7);

                // Add worm wiggle
                double lateral = (rnd.nextDouble() - 0.5) * 1.5;
                x += -dz * lateral;
                z += dx * lateral;

                BlockPos bPos = new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
                
                // Tight constraint: Must stay within the ellipse profile
                int rdx = bPos.getX() - crownCenter.getX();
                int rdz = bPos.getZ() - crownCenter.getZ();
                int rdy = bPos.getY() - crownCenter.getY();

                double normX = (double) rdx / canopyRadius;
                double normZ = (double) rdz / canopyRadius;
                double normY = (double) Math.min(0, rdy) / depth;
                
                double distSq = normX * normX + normZ * normZ + normY * normY;

                if (distSq > 0.9) {
                    continue; 
                }
                
                Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
                // Use Stem Provider for branches
                placeLog(level, bPos, axis, rnd, config.stemProvider());
            }
        }

        // Crown leaves: Half-ellipse facing downwards (flat side up)
        for (int dx = -canopyRadius; dx <= canopyRadius; dx++) {
            for (int dz = -canopyRadius; dz <= canopyRadius; dz++) {
                for (int dy = 0; dy >= -depth; dy--) {
                    double normX = (double) dx / canopyRadius;
                    double normZ = (double) dz / canopyRadius;
                    double normY = (double) dy / depth;
                    
                    double distSq = normX * normX + normZ * normZ + normY * normY;
                    
                    if (distSq <= 1.0) {
                        // Protect the ellipse shape
                        if (distSq > 0.85 && rnd.nextDouble() < (distSq - 0.85) * 6) {
                            continue;
                        }
                        
                        BlockPos leafPos = crownCenter.offset(dx, dy, dz);
                        placeLeaf(level, leafPos, rnd, config.leavesProvider());
                    }
                }
            }
        }

        // After successful placement, paint a localized overgrowth patch on the ceiling
        paintCeilingOvergrowth(level, origin, rnd, trunkWidth);

        return true;
    }

    /**
     * Spreads moss and hanging roots around the tree's connection point on the ceiling.
     * Fixed: Now strictly verifies it only places on ceiling surfaces to prevent floor leakage.
     */
    private void paintCeilingOvergrowth(WorldGenLevel level, BlockPos origin, RandomSource rnd, int trunkWidth) {
        int radius = 3 + rnd.nextInt(3);
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        
        for (int x = -radius; x <= radius + trunkWidth; x++) {
            for (int z = -radius; z <= radius + trunkWidth; z++) {
                double distSq = x * x + z * z;
                if (distSq > radius * radius) continue;

                mPos.set(origin.getX() + x, origin.getY(), origin.getZ() + z);
                
                // Ceiling check: Block must be solid, and block BELOW must be air
                if (isValidAnchor(level.getBlockState(mPos.above())) && level.getBlockState(mPos).isAir()) {
                    // Paint the ceiling with moss
                    level.setBlock(mPos.above(), net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState(), 3);
                    
                    // Occasional hanging roots or glow berries could be added here if desired
                    if (rnd.nextFloat() < 0.2f) {
                        // Place hanging roots or similar
                    }
                }
            }
        }
    }
}
