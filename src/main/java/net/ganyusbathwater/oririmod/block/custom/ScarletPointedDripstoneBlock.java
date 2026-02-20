/**
 * ScarletPointedDripstoneBlock
 *
 * This class is a modified copy of Minecraft's vanilla PointedDripstoneBlock
 * (net.minecraft.world.level.block.PointedDripstoneBlock).
 *
 * Modifications:
 * - Collects blood water (ModFluids.BLOOD_WATER_SOURCE) instead of regular water
 * - Uses scarlet_cave_particle for drip effects instead of vanilla drip particles
 * - Fills cauldrons with blood water instead of regular water
 *
 * Original source: net.minecraft.world.level.block.PointedDripstoneBlock
 * Minecraft version: 1.21.1
 */
package net.ganyusbathwater.oririmod.block.custom;

import com.google.common.annotations.VisibleForTesting;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.ganyusbathwater.oririmod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ScarletPointedDripstoneBlock extends Block implements Fallable, SimpleWaterloggedBlock {
    public static final DirectionProperty TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
    private static final int DELAY_BEFORE_FALLING = 2;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02F;
    private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12F;
    private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;

    private static final VoxelShape TIP_MERGE_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape BASE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public ScarletPointedDripstoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(TIP_DIRECTION, Direction.UP)
                .setValue(THICKNESS, DripstoneThickness.TIP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidPointedDripstonePlacement(level, pos, state.getValue(TIP_DIRECTION));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        } else {
            Direction tipDirection = state.getValue(TIP_DIRECTION);
            if (tipDirection == Direction.DOWN && level.getBlockTicks().hasScheduledTick(pos, this)) {
                return state;
            }

            if (direction == tipDirection.getOpposite() && !this.canSurvive(state, level, pos)) {
                if (tipDirection == Direction.DOWN) {
                    level.scheduleTick(pos, this, DELAY_BEFORE_FALLING);
                } else {
                    level.scheduleTick(pos, this, 1);
                }
                return state;
            }

            boolean isTipMerge = state.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
            DripstoneThickness newThickness = calculateDripstoneThickness(level, pos, tipDirection, isTipMerge);
            return state.setValue(THICKNESS, newThickness);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        BlockPos pos = hit.getBlockPos();
        if (!level.isClientSide && projectile.mayInteract(level, pos) && projectile instanceof ThrownTrident
                && projectile.getDeltaMovement().length() > 0.6) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.0F, 2.0F, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isStalagmite(state) && !this.canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        } else {
            spawnFallingStalactite(state, level, pos);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        maybeFillCauldron(state, level, pos, random.nextFloat());
        if (random.nextFloat() < 0.011377778F && isStalactiteStartPos(state, level, pos)) {
            growStalactiteOrStalagmiteIfPossible(state, level, pos, random);
        }
    }

    /**
     * Modified: Uses scarlet_cave_particle for drip effects and blood water
     * visuals.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (canDrip(state)) {
            float probability = random.nextFloat();
            if (probability <= DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE) {
                BlockPos tipPos = findTip(state, level, pos, MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE, true);
                if (tipPos != null) {
                    Fluid fluid = getCauldronFillFluidType(level, tipPos);
                    // Modified: Use scarlet_cave_particle instead of vanilla drip particles
                    ParticleOptions particle;
                    if (fluid == Fluids.WATER) {
                        // Even for water scenario, use our custom particle
                        particle = ModParticles.SCARLET_CAVE_PARTICLE.get();
                    } else if (fluid == Fluids.LAVA) {
                        particle = ParticleTypes.DRIPPING_DRIPSTONE_LAVA;
                    } else if (fluid == ModFluids.BLOOD_WATER_SOURCE.get()) {
                        particle = ModParticles.SCARLET_CAVE_PARTICLE.get();
                    } else {
                        return;
                    }
                    Vec3 offset = state.getOffset(level, pos);
                    double x = (double) pos.getX() + 0.5 + offset.x;
                    double y = (double) ((float) (pos.getY() + 1) - 0.6875F) - 0.0625;
                    double z = (double) pos.getZ() + 0.5 + offset.z;
                    level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction closestDir = context.getNearestLookingVerticalDirection().getOpposite();
        Direction tipDir = calculateTipDirection(level, pos, closestDir);
        if (tipDir == null) {
            return null;
        }
        boolean isTipMerge = !context.isSecondaryUseActive()
                && calculateDripstoneThickness(level, pos, tipDir, false) == DripstoneThickness.TIP_MERGE;
        DripstoneThickness thickness = calculateDripstoneThickness(level, pos, tipDir, isTipMerge);
        return thickness == null ? null
                : this.defaultBlockState()
                        .setValue(TIP_DIRECTION, tipDir)
                        .setValue(THICKNESS, thickness)
                        .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DripstoneThickness thickness = state.getValue(THICKNESS);
        VoxelShape shape;
        if (thickness == DripstoneThickness.TIP_MERGE) {
            shape = TIP_MERGE_SHAPE;
        } else if (thickness == DripstoneThickness.TIP) {
            shape = state.getValue(TIP_DIRECTION) == Direction.DOWN ? TIP_SHAPE_DOWN : TIP_SHAPE_UP;
        } else if (thickness == DripstoneThickness.FRUSTUM) {
            shape = FRUSTUM_SHAPE;
        } else if (thickness == DripstoneThickness.MIDDLE) {
            shape = MIDDLE_SHAPE;
        } else {
            shape = BASE_SHAPE;
        }
        Vec3 offset = state.getOffset(level, pos);
        return shape.move(offset.x, 0.0, offset.z);
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return 0.125F;
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1045, pos, 0);
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    @Override
    protected float getMaxVerticalOffset() {
        return 0.0F;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    // ===== STATIC HELPER METHODS =====

    private static boolean isStalactite(BlockState state) {
        return isPointedDripstoneWithDir(state, Direction.DOWN);
    }

    private static boolean isStalagmite(BlockState state) {
        return isPointedDripstoneWithDir(state, Direction.UP);
    }

    private static boolean isTip(BlockState state, boolean isMerge) {
        if (!(state.getBlock() instanceof ScarletPointedDripstoneBlock)) {
            return false;
        }
        DripstoneThickness thickness = state.getValue(THICKNESS);
        return thickness == DripstoneThickness.TIP || (isMerge && thickness == DripstoneThickness.TIP_MERGE);
    }

    private static boolean isPointedDripstoneWithDir(BlockState state, Direction dir) {
        return state.getBlock() instanceof ScarletPointedDripstoneBlock
                && state.getValue(TIP_DIRECTION) == dir;
    }

    private static boolean isValidPointedDripstonePlacement(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos supportPos = pos.relative(dir.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, dir)
                || isPointedDripstoneWithDir(supportState, dir);
    }

    @Nullable
    private static Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction preferredDir) {
        if (isValidPointedDripstonePlacement(level, pos, preferredDir)) {
            return preferredDir;
        } else if (isValidPointedDripstonePlacement(level, pos, preferredDir.getOpposite())) {
            return preferredDir.getOpposite();
        }
        return null;
    }

    private static DripstoneThickness calculateDripstoneThickness(LevelReader level, BlockPos pos, Direction dir,
            boolean isTipMerge) {
        Direction opposite = dir.getOpposite();
        BlockState stateInDir = level.getBlockState(pos.relative(dir));
        if (isPointedDripstoneWithDir(stateInDir, opposite)) {
            return !isTipMerge && stateInDir.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE
                    ? DripstoneThickness.TIP
                    : DripstoneThickness.TIP_MERGE;
        } else if (!isPointedDripstoneWithDir(stateInDir, dir)) {
            return DripstoneThickness.TIP;
        } else {
            DripstoneThickness adjacentThickness = stateInDir.getValue(THICKNESS);
            if (adjacentThickness != DripstoneThickness.TIP && adjacentThickness != DripstoneThickness.TIP_MERGE) {
                BlockState supportState = level.getBlockState(pos.relative(opposite));
                return isPointedDripstoneWithDir(supportState, dir) ? DripstoneThickness.MIDDLE
                        : DripstoneThickness.BASE;
            } else {
                return DripstoneThickness.FRUSTUM;
            }
        }
    }

    private static boolean canDrip(BlockState state) {
        return isStalactite(state) && state.getValue(THICKNESS) == DripstoneThickness.TIP
                && !state.getValue(WATERLOGGED);
    }

    private static boolean canTipGrow(BlockState state, ServerLevel level, BlockPos pos) {
        Direction dir = state.getValue(TIP_DIRECTION);
        BlockPos below = pos.relative(dir);
        BlockState belowState = level.getBlockState(below);
        if (!belowState.getFluidState().isEmpty()) {
            return false;
        }
        return belowState.isAir() || isTip(belowState, false);
    }

    @Nullable
    private static BlockPos findTip(BlockState state, LevelAccessor level, BlockPos pos, int maxDist,
            boolean allowMerge) {
        if (isTip(state, allowMerge)) {
            return pos;
        }
        Direction dir = state.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> pred = (p, s) -> s.getBlock() instanceof ScarletPointedDripstoneBlock
                && s.getValue(TIP_DIRECTION) == dir;
        return findBlockVertical(level, pos, dir.getAxisDirection(), pred,
                s -> isTip(s, allowMerge), maxDist).orElse(null);
    }

    @Nullable
    private static Direction findStalactiteTipAboveCauldron(Level level, BlockPos pos) {
        // Check UP direction for forming stalactite above a cauldron (cauldron filling
        // logic)
        // We search for a tip above
        BlockPos above = pos.above();
        for (int i = 0; i < MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON; i++) {
            BlockState stateAt = level.getBlockState(above);
            if (stateAt.getBlock() instanceof ScarletPointedDripstoneBlock) {
                if (isStalactite(stateAt) && stateAt.getValue(THICKNESS) == DripstoneThickness.TIP) {
                    return Direction.DOWN;
                }
                above = above.above();
            } else {
                break;
            }
        }
        return null;
    }

    private static boolean isStalactiteStartPos(BlockState state, ServerLevel level, BlockPos pos) {
        return isStalactite(state)
                && !isPointedDripstoneWithDir(level.getBlockState(pos.above()), Direction.DOWN);
    }

    /**
     * Modified: Fills cauldron with blood water instead of regular water.
     */
    @VisibleForTesting
    public static void maybeFillCauldron(BlockState state, ServerLevel level, BlockPos pos, float dripChance) {
        if (dripChance > 0.17578125F || !isStalactiteStartPos(state, level, pos)) {
            return;
        }

        BlockPos tipPos = findTip(state, level, pos, MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE, true);
        if (tipPos == null) {
            return;
        }

        Fluid fluid = getCauldronFillFluidType(level, tipPos);
        if (fluid == Fluids.EMPTY) {
            return;
        }

        BlockPos cauldronBelow = findFillableCauldronBelowStalactiteTip(level, tipPos);
        if (cauldronBelow == null) {
            return;
        }

        BlockState cauldronState = level.getBlockState(cauldronBelow);
        if (!(cauldronState.getBlock() instanceof AbstractCauldronBlock cauldronBlock)) {
            return;
        }

        // Modified: Blood water fills the cauldron.
        // receiveStalactiteDrip is protected, so we use reflection to call it.
        try {
            java.lang.reflect.Method method = AbstractCauldronBlock.class.getDeclaredMethod(
                    "receiveStalactiteDrip", BlockState.class, Level.class, BlockPos.class, Fluid.class);
            method.setAccessible(true);
            method.invoke(cauldronBlock, cauldronState, level, cauldronBelow, fluid);
        } catch (Exception e) {
            // Fallback: just fire the game event without filling cauldron
        }
        level.gameEvent(GameEvent.FLUID_PLACE, cauldronBelow, GameEvent.Context.of(cauldronState));
    }

    /**
     * Modified: Returns blood water fluid type instead of regular water
     * when there is a water/blood water source above.
     */
    private static Fluid getCauldronFillFluidType(Level level, BlockPos tipPos) {
        return getFluidAboveStalactite(level, tipPos, level.getBlockState(tipPos))
                .filter(fluidInfo -> fluidInfo.fluid.is(FluidTags.WATER)
                        || fluidInfo.fluid.isSame(ModFluids.BLOOD_WATER_SOURCE.get()))
                .map(fluidInfo -> (Fluid) ModFluids.BLOOD_WATER_SOURCE.get()) // Always return blood water for this
                                                                              // block
                .orElseGet(() -> getFluidAboveStalactite(level, tipPos, level.getBlockState(tipPos))
                        .filter(fluidInfo -> fluidInfo.fluid.is(FluidTags.LAVA))
                        .map(fluidInfo -> (Fluid) Fluids.LAVA)
                        .orElse(Fluids.EMPTY));
    }

    @Nullable
    private static BlockPos findFillableCauldronBelowStalactiteTip(Level level, BlockPos tipPos) {
        Predicate<BlockState> isCauldron = s -> s.getBlock() instanceof AbstractCauldronBlock;
        return findBlockVertical(level, tipPos, Direction.DOWN.getAxisDirection(),
                (p, s) -> canDripThrough(level, p, s), isCauldron,
                MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON).orElse(null);
    }

    @Nullable
    private static BlockPos findRootBlock(Level level, BlockPos pos, BlockState state, int maxDist) {
        Direction dir = state.getValue(TIP_DIRECTION);
        BiPredicate<BlockPos, BlockState> pred = (p, s) -> s.getBlock() instanceof ScarletPointedDripstoneBlock
                && s.getValue(TIP_DIRECTION) == dir;
        return findBlockVertical(level, pos, dir.getOpposite().getAxisDirection(), pred,
                s -> !(s.getBlock() instanceof ScarletPointedDripstoneBlock), maxDist).orElse(null);
    }

    private static void growStalactiteOrStalagmiteIfPossible(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        BlockState aboveState = level.getBlockState(pos.above());
        // Scarlet dripstone grows from scarlet dripstone block (or scarlet stone)
        if (!isScarletDripstoneBase(aboveState)) {
            return;
        }

        BlockPos tipPos = findTip(state, level, pos, MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE, true);
        if (tipPos == null) {
            return;
        }

        // Check for fluid above for growth
        if (hasFluidAboveStalactite(level, tipPos)) {
            grow(level, tipPos, Direction.DOWN);
            // Grow matching stalagmite below if possible
            BlockPos stalagmiteBelow = findStalagmiteTipBelow(level, tipPos);
            if (stalagmiteBelow != null) {
                grow(level, stalagmiteBelow, Direction.UP);
            }
        }
    }

    private static boolean isScarletDripstoneBase(BlockState state) {
        return state.is(net.ganyusbathwater.oririmod.block.ModBlocks.SCARLET_DRIPSTONE_BLOCK.get());
    }

    private static boolean hasFluidAboveStalactite(Level level, BlockPos tipPos) {
        return getFluidAboveStalactite(level, tipPos, level.getBlockState(tipPos)).isPresent();
    }

    private static void grow(ServerLevel level, BlockPos tipPos, Direction growDir) {
        BlockPos growPos = tipPos.relative(growDir);
        BlockState growState = level.getBlockState(growPos);
        if (isUnmergedTipWithDirection(level.getBlockState(tipPos), growDir.getOpposite())) {
            // Create new tip at the grow position
            ScarletPointedDripstoneBlock block = (ScarletPointedDripstoneBlock) net.ganyusbathwater.oririmod.block.ModBlocks.POINTED_SCARLET_DRIPSTONE
                    .get();
            BlockState newState = block.defaultBlockState()
                    .setValue(TIP_DIRECTION, growDir.getOpposite())
                    .setValue(THICKNESS, DripstoneThickness.TIP)
                    .setValue(WATERLOGGED, growState.getFluidState().getType() == Fluids.WATER);
            if (growState.isAir() || growState.getFluidState().is(Fluids.WATER)) {
                createDripstone(level, growPos, growDir, newState);
            }
        }
    }

    private static void createDripstone(ServerLevel level, BlockPos pos, Direction dir, BlockState state) {
        level.setBlock(pos, state, 3);
        // Update neighbors
        BlockPos belowOrAbove = pos.relative(dir.getOpposite());
        level.blockUpdated(belowOrAbove, level.getBlockState(belowOrAbove).getBlock());
    }

    @Nullable
    private static BlockPos findStalagmiteTipBelow(Level level, BlockPos tipPos) {
        for (int i = 1; i <= MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON; i++) {
            BlockPos below = tipPos.below(i);
            BlockState belowState = level.getBlockState(below);
            if (belowState.getBlock() instanceof ScarletPointedDripstoneBlock
                    && isStalagmite(belowState)
                    && belowState.getValue(THICKNESS) == DripstoneThickness.TIP) {
                return below;
            }
            if (!canDripThrough(level, below, belowState)) {
                break;
            }
        }
        return null;
    }

    private static boolean isUnmergedTipWithDirection(BlockState state, Direction dir) {
        return isTip(state, false) && state.getValue(TIP_DIRECTION) == dir;
    }

    private static boolean canDripThrough(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.isSolidRender(level, pos)) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !Shapes.joinIsNotEmpty(Shapes.block(), shape, net.minecraft.world.phys.shapes.BooleanOp.AND);
    }

    /**
     * Modified: Detects blood water sources and regular water/lava above the
     * stalactite.
     */
    private static Optional<FluidInfo> getFluidAboveStalactite(Level level, BlockPos tipPos, BlockState tipState) {
        if (!isStalactite(tipState)) {
            return Optional.empty();
        }

        Direction searchDir = Direction.UP;
        BiPredicate<BlockPos, BlockState> isDripstone = (p,
                s) -> s.getBlock() instanceof ScarletPointedDripstoneBlock
                        && s.getValue(TIP_DIRECTION) == Direction.DOWN;

        // Find the block above the entire stalactite chain
        Optional<BlockPos> rootAbove = findBlockVertical(level, tipPos, searchDir.getAxisDirection(), isDripstone,
                s -> !(s.getBlock() instanceof ScarletPointedDripstoneBlock),
                MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE);

        return rootAbove.flatMap(pos -> {
            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);
            FluidState fluidState = aboveState.getFluidState();

            // Check for blood water
            if (fluidState.getType().isSame(ModFluids.BLOOD_WATER_SOURCE.get())
                    || fluidState.getType().isSame(ModFluids.BLOOD_WATER_FLOWING.get())) {
                return Optional.of(new FluidInfo(above, fluidState.getType(), aboveState));
            }

            // Check for regular fluids
            if (!fluidState.isEmpty()) {
                return Optional.of(new FluidInfo(above, fluidState.getType(), aboveState));
            }

            return Optional.empty();
        });
    }

    private static void spawnFallingStalactite(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockState currentState = state;
        while (isStalactite(currentState)) {
            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, mutable, currentState);
            if (isTip(currentState, false)) {
                int maxFallDistance = Math.max(1 + pos.getY() - mutable.getY(), 6);
                float damage = (float) maxFallDistance;
                fallingBlock.setHurtsEntities(damage, 40);
                break;
            }
            mutable.move(Direction.DOWN);
            currentState = level.getBlockState(mutable);
        }
    }

    private static Optional<BlockPos> findBlockVertical(LevelAccessor level, BlockPos pos,
            Direction.AxisDirection axisDir,
            BiPredicate<BlockPos, BlockState> continuePredicate, Predicate<BlockState> targetPredicate, int maxDist) {
        Direction dir = Direction.get(axisDir, Direction.Axis.Y);
        BlockPos.MutableBlockPos searchPos = pos.relative(dir).mutable();

        for (int i = 1; i < maxDist; i++) {
            BlockState searchState = level.getBlockState(searchPos);
            if (targetPredicate.test(searchState)) {
                return Optional.of(searchPos.immutable());
            }
            if (!continuePredicate.test(searchPos, searchState)) {
                return Optional.empty();
            }
            searchPos.move(dir);
        }
        return Optional.empty();
    }

    // Records
    private record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
    }
}
