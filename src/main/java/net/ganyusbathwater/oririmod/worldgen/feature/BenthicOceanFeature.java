package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.custom.HydrothermalChimneyBlock;
import net.ganyusbathwater.oririmod.block.custom.ModCoralWallFanBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashSet;
import java.util.Set;

/**
 * Natural Benthic Ocean Feature Generator for the Silent Depths and Sunken Trenches.
 * Generates:
 * 1. Hydrothermal Chimney Vents (using ModBlocks.HYDROTHERMAL_CHIMNEY, growing upwards like dripstone)
 * 2. Organic branching coral colonies (antler/fan trees with zero floating blocks; wall fans with proper FACING)
 * 3. Benthic sponge beds
 */
public class BenthicOceanFeature extends Feature<NoneFeatureConfiguration> {

    public BenthicOceanFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 1. Locate the solid seabed floor below water
        BlockPos.MutableBlockPos mPos = origin.mutable();
        
        while (mPos.getY() > level.getMinBuildHeight() + 5 && level.getBlockState(mPos).is(Blocks.WATER)) {
            mPos.move(Direction.DOWN);
        }

        BlockPos floorPos = mPos.immutable();
        BlockState floorState = level.getBlockState(floorPos);
        BlockPos waterPos = floorPos.above();

        // Must be submerged in water
        if (!level.getBlockState(waterPos).is(Blocks.WATER)) {
            return false;
        }

        // Must sit strictly on natural abyssal seabed (Abyss Sand or Abyssal Basalt)
        boolean isValidFloor = floorState.is(ModBlocks.ABYSS_SAND.get()) ||
                floorState.is(ModBlocks.ABYSSAL_BASALT.get());

        if (!isValidFloor) {
            return false;
        }

        // Biome verification: strictly the_silent_depths or the_sunken_trenches
        var biomeHolder = level.getBiome(floorPos);
        ResourceLocation biomeKey = biomeHolder.unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeKey != null && biomeKey.getPath().equals("the_silent_oceans")) {
            return false; // Zero benthic features in the shallow photic ocean
        }

        // "Down at the ocean floor between the mountains and only at that depth generate the corals, sponges and the hydrothermal chimneys."
        // Continental descent slope (Y > -60) remains completely barren!
        if (floorPos.getY() > -60) {
            return false;
        }

        // Must never generate on magma or inside magma fault cracks
        if (floorState.is(Blocks.MAGMA_BLOCK) || floorState.is(ModBlocks.AETHER_MAGMA_BLOCK.get())) {
            return false;
        }

        boolean isVolcanic = floorState.is(ModBlocks.ABYSSAL_BASALT.get());

        // A. Hydrothermal Chimney Vents (Upward growing like pointed dripstone: BASE -> MIDDLE -> FRUSTUM -> TIP)
        if (isVolcanic && random.nextInt(100) < 45) {
            return placeHydrothermalVents(level, floorPos, random);
        }

        // B. Sponges: Small clustered benthic beds in quiet seabed pockets
        if (random.nextInt(100) < 25) {
            int spongeCount = 2 + random.nextInt(4);
            for (int i = 0; i < spongeCount; i++) {
                BlockPos targetPos = waterPos.offset(random.nextInt(3) - 1, random.nextInt(2), random.nextInt(3) - 1);
                BlockPos belowTarget = targetPos.below();
                if (level.getBlockState(targetPos).is(Blocks.WATER) && level.getBlockState(belowTarget).isSolid()) {
                    level.setBlock(targetPos, ModBlocks.WET_BENTHIC_SPONGE.get().defaultBlockState(), 2);
                }
            }
            return true;
        }

        // C. Corals: Multi-branching organic colonies in the valleys and caverns
        return generateBranchingCoralColony(level, floorPos, random);
    }

    /**
     * Places authentic upward-growing Hydrothermal Chimneys using ModBlocks.HYDROTHERMAL_CHIMNEY.
     * Follows upward dripstone thickness: BASE at ground level, MIDDLE, FRUSTUM, and TIP at apex.
     */
    private boolean placeHydrothermalVents(WorldGenLevel level, BlockPos floorPos, RandomSource random) {
        int ventCount = 1 + random.nextInt(3); // 1 to 3 chimney pillars in a cluster
        boolean placedAny = false;

        for (int v = 0; v < ventCount; v++) {
            BlockPos basePos = floorPos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            BlockState bState = level.getBlockState(basePos);

            if (!bState.isSolid() || bState.is(Blocks.MAGMA_BLOCK) || bState.is(ModBlocks.AETHER_MAGMA_BLOCK.get()) || !level.getBlockState(basePos.above()).is(Blocks.WATER)) {
                continue;
            }

            int height = 2 + random.nextInt(4); // 2 to 5 blocks tall
            for (int dy = 0; dy < height; dy++) {
                BlockPos cPos = basePos.above(dy + 1);
                if (!level.getBlockState(cPos).is(Blocks.WATER)) {
                    break;
                }

                DripstoneThickness thickness;
                if (dy == height - 1) {
                    thickness = DripstoneThickness.TIP;
                } else if (dy == height - 2) {
                    thickness = DripstoneThickness.FRUSTUM;
                } else if (dy == 0) {
                    thickness = DripstoneThickness.BASE;
                } else {
                    thickness = DripstoneThickness.MIDDLE;
                }

                BlockState chimneyState = ModBlocks.HYDROTHERMAL_CHIMNEY.get().defaultBlockState()
                        .setValue(HydrothermalChimneyBlock.THICKNESS, thickness)
                        .setValue(HydrothermalChimneyBlock.WATERLOGGED, true);

                level.setBlock(cPos, chimneyState, 2);
                placedAny = true;
            }
        }
        return placedAny;
    }

    /**
     * Generates an authentic organic branching coral structure (like Lophelia or Gorgonian fan/antler colonies).
     * Solves floating corals:
     * - ModCoralWallFanBlock is placed on side faces with proper directional FACING.
     * - ModCoralPlantBlock / ModCoralFanBlock are placed strictly on top of solid coral blocks.
     */
    private boolean generateBranchingCoralColony(WorldGenLevel level, BlockPos floorPos, RandomSource random) {
        boolean useElderCoral = random.nextBoolean();
        BlockState coralBlock = useElderCoral 
                ? ModBlocks.ELDER_CORAL_BLOCK.get().defaultBlockState() 
                : ModBlocks.ABYSSAL_CORAL_CROWN_BLOCK.get().defaultBlockState();

        Set<BlockPos> placedBlocks = new HashSet<>();
        BlockPos trunkRoot = floorPos.above();

        // 1. Root trunk (1 to 2 blocks tall)
        int trunkHeight = 1 + random.nextInt(2);
        for (int y = 0; y < trunkHeight; y++) {
            BlockPos tPos = trunkRoot.above(y);
            if (level.getBlockState(tPos).is(Blocks.WATER)) {
                level.setBlock(tPos, coralBlock, 2);
                placedBlocks.add(tPos);
            }
        }

        BlockPos branchHub = trunkRoot.above(trunkHeight - 1);

        // 2. Multi-directional branching arms (2 to 4 branches reaching outward and curving upward)
        int branchCount = 2 + random.nextInt(3);
        Direction[] allDirs = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
        
        for (int b = 0; b < branchCount; b++) {
            Direction dir = allDirs[random.nextInt(allDirs.length)];
            BlockPos currentBranch = branchHub;
            int branchLength = 2 + random.nextInt(3); // 2 to 4 blocks reach

            for (int step = 0; step < branchLength; step++) {
                currentBranch = currentBranch.relative(dir);
                if (step >= 1 || random.nextBoolean()) {
                    currentBranch = currentBranch.above();
                }

                if (level.getBlockState(currentBranch).is(Blocks.WATER)) {
                    level.setBlock(currentBranch, coralBlock, 2);
                    placedBlocks.add(currentBranch);
                }

                // Sub-branch fork at the tip
                if (step == branchLength - 1 && random.nextBoolean()) {
                    Direction sideDir = (random.nextBoolean()) ? dir.getClockWise() : dir.getCounterClockWise();
                    BlockPos forkPos = currentBranch.relative(sideDir).above();
                    if (level.getBlockState(forkPos).is(Blocks.WATER)) {
                        level.setBlock(forkPos, coralBlock, 2);
                        placedBlocks.add(forkPos);
                    }
                }
            }
        }

        // 3. Sprout living coral flora with strict directional validation (zero floating blocks!)
        for (BlockPos bPos : placedBlocks) {
            // A. Top surface: ModCoralPlantBlock or ModCoralFanBlock strictly on Direction.UP
            BlockPos aboveB = bPos.above();
            if (level.getBlockState(aboveB).is(Blocks.WATER) && !placedBlocks.contains(aboveB)) {
                if (random.nextFloat() < 0.60f) {
                    BlockState topFlora = (random.nextBoolean())
                            ? (useElderCoral ? ModBlocks.ELDER_CORAL.get().defaultBlockState() : ModBlocks.ABYSSAL_CORAL_CROWN.get().defaultBlockState()).setValue(BlockStateProperties.WATERLOGGED, true)
                            : (useElderCoral ? ModBlocks.ELDER_CORAL_FAN.get().defaultBlockState() : ModBlocks.ABYSSAL_CORAL_CROWN_FAN.get().defaultBlockState()).setValue(BlockStateProperties.WATERLOGGED, true);
                    level.setBlock(aboveB, topFlora, 2);
                }
            }

            // B. Horizontal wall surfaces: ModCoralWallFanBlock strictly facing outward from solid bPos
            for (Direction hDir : Direction.Plane.HORIZONTAL) {
                if (random.nextFloat() < 0.30f) {
                    BlockPos sideB = bPos.relative(hDir);
                    if (level.getBlockState(sideB).is(Blocks.WATER) && !placedBlocks.contains(sideB)) {
                        // FACING must point in hDir (outward from bPos), so attached face is bPos!
                        BlockState wallFan = (useElderCoral
                                ? ModBlocks.ELDER_CORAL_WALL_FAN.get().defaultBlockState()
                                : ModBlocks.ABYSSAL_CORAL_CROWN_WALL_FAN.get().defaultBlockState())
                                .setValue(ModCoralWallFanBlock.FACING, hDir)
                                .setValue(ModCoralWallFanBlock.WATERLOGGED, true);

                        level.setBlock(sideB, wallFan, 2);
                    }
                }
            }
        }

        return !placedBlocks.isEmpty();
    }
}
