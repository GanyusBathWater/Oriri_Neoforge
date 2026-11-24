package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyriadCactusBlock extends DirectionalBlock implements BonemealableBlock {
    public static final MapCodec<MyriadCactusBlock> CODEC = simpleCodec(MyriadCactusBlock::new);

    private static final int MAX_TRUNK_HEIGHT = 7;
    private static final int MAX_ARMS = 3;
    private static final int MAX_ARM_VERTICAL_HEIGHT = 3;

    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> BRANCH_SHAPES = new EnumMap<>(Direction.class);

    static {
        // Main pillars spanning the full block length on their axis (0 to 16)
        SHAPES.put(Direction.UP, Block.box(1, 0, 1, 15, 16, 15));
        SHAPES.put(Direction.DOWN, Block.box(1, 0, 1, 15, 16, 15));
        SHAPES.put(Direction.NORTH, Block.box(1, 1, 0, 15, 15, 16));
        SHAPES.put(Direction.SOUTH, Block.box(1, 1, 0, 15, 15, 16));
        SHAPES.put(Direction.WEST, Block.box(0, 1, 1, 16, 15, 15));
        SHAPES.put(Direction.EAST, Block.box(0, 1, 1, 16, 15, 15));

        // 1-pixel branches extending outwards from the 14x14 core
        BRANCH_SHAPES.put(Direction.UP, Block.box(1, 15, 1, 15, 16, 15));
        BRANCH_SHAPES.put(Direction.DOWN, Block.box(1, 0, 1, 15, 1, 15));
        BRANCH_SHAPES.put(Direction.NORTH, Block.box(1, 1, 0, 15, 15, 1));
        BRANCH_SHAPES.put(Direction.SOUTH, Block.box(1, 1, 15, 15, 15, 16));
        BRANCH_SHAPES.put(Direction.WEST, Block.box(0, 1, 1, 1, 15, 15));
        BRANCH_SHAPES.put(Direction.EAST, Block.box(15, 1, 1, 16, 15, 15));
    }

    @Override
    public @NotNull MapCodec<MyriadCactusBlock> codec() {
        return CODEC;
    }

    public MyriadCactusBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(UP, level.getBlockState(pos.above()).is(this))
                .setValue(DOWN, level.getBlockState(pos.below()).is(this))
                .setValue(NORTH, level.getBlockState(pos.north()).is(this))
                .setValue(SOUTH, level.getBlockState(pos.south()).is(this))
                .setValue(EAST, level.getBlockState(pos.east()).is(this))
                .setValue(WEST, level.getBlockState(pos.west()).is(this));
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return state.setValue(getDirectionProperty(direction), neighborState.is(this));
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Direction parentDirection = state.getValue(FACING).getOpposite();
        BlockPos parentPos = pos.relative(parentDirection);
        BlockState parentState = level.getBlockState(parentPos);
        
        if (parentState.is(this)) {
            return true;
        }
        
        return parentState.isFaceSturdy(level, parentPos, state.getValue(FACING)) || parentState.is(net.minecraft.tags.BlockTags.SAND);
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    public static BooleanProperty getDirectionProperty(Direction direction) {
        return switch (direction) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
        };
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPES.get(state.getValue(FACING));
        
        // Add branch shapes if they are connected
        if (state.getValue(UP)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.UP));
        if (state.getValue(DOWN)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.DOWN));
        if (state.getValue(NORTH)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.NORTH));
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.SOUTH));
        if (state.getValue(EAST)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.EAST));
        if (state.getValue(WEST)) shape = Shapes.or(shape, BRANCH_SHAPES.get(Direction.WEST));

        return shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, UP, DOWN, NORTH, SOUTH, EAST, WEST);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void entityInside(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        entity.hurt(level.damageSources().cactus(), 1.0F);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 3)) {
            return;
        }
        // Only tick growth from tip blocks (nothing directly above) so taller cacti do not grow exponentially faster
        if (level.getBlockState(pos.above()).is(this)) {
            return;
        }
        if (random.nextInt(14) == 0) {
            tryGrowStep(level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        CactusStructure structure = analyzeStructure(level, pos);
        if (structure == null) {
            return false;
        }
        if (canGrowTrunk(level, structure)) {
            return true;
        }
        if (!findExtendableArms(level, structure).isEmpty()) {
            return true;
        }
        return !findNewArmCandidates(level, structure).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        tryGrowStep(level, pos, random);
    }

    private record ArmGrowthTarget(BlockPos pos, Direction facing) {}

    private record NewArmCandidate(BlockPos trunkPos, Direction dir) {}

    private record CactusStructure(
            BlockPos trunkBase,
            int trunkHeight,
            BlockPos trunkTop,
            int armCount,
            Set<Direction> usedDirs,
            Set<Integer> usedArmLevels,
            List<BlockPos> armOrigins,
            List<Direction> armDirs
    ) {}

    private CactusStructure analyzeStructure(LevelReader level, BlockPos startPos) {
        // Walk backwards along parent FACING links to find the root of the cactus
        BlockPos cur = startPos;
        for (int step = 0; step < 24; step++) {
            BlockState st = level.getBlockState(cur);
            if (!st.is(this)) {
                return null;
            }
            Direction parentDir = st.getValue(FACING).getOpposite();
            BlockPos parentPos = cur.relative(parentDir);
            if (!level.getBlockState(parentPos).is(this)) {
                break;
            }
            cur = parentPos;
        }

        // If the cactus was rooted vertically, walk down any straight vertical stack to the true base
        while (level.getBlockState(cur.below()).is(this)) {
            cur = cur.below();
        }
        BlockPos trunkBase = cur;

        // Measure vertical trunk height
        int trunkHeight = 0;
        BlockPos scan = trunkBase;
        while (trunkHeight < MAX_TRUNK_HEIGHT + 4 && level.getBlockState(scan).is(this)) {
            trunkHeight++;
            scan = scan.above();
        }
        if (trunkHeight <= 0) {
            return null;
        }
        BlockPos trunkTop = trunkBase.above(trunkHeight - 1);

        // Scan existing orthogonal arms branching off the trunk
        int armCount = 0;
        Set<Direction> usedDirs = EnumSet.noneOf(Direction.class);
        Set<Integer> usedArmLevels = new HashSet<>();
        List<BlockPos> armOrigins = new ArrayList<>();
        List<Direction> armDirs = new ArrayList<>();

        for (int h = 1; h < trunkHeight; h++) {
            BlockPos trunkNode = trunkBase.above(h);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos adj = trunkNode.relative(dir);
                BlockState adjState = level.getBlockState(adj);
                if (adjState.is(this) && adjState.getValue(FACING) == dir) {
                    armCount++;
                    usedDirs.add(dir);
                    usedArmLevels.add(h);
                    armOrigins.add(trunkNode);
                    armDirs.add(dir);
                }
            }
        }

        return new CactusStructure(trunkBase, trunkHeight, trunkTop, armCount, usedDirs, usedArmLevels, armOrigins, armDirs);
    }

    private boolean canPlaceGrowthBlock(LevelReader level, BlockPos pos) {
        if (pos.getY() >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        return existing.isAir() || existing.canBeReplaced();
    }

    private boolean canGrowTrunk(LevelReader level, CactusStructure s) {
        return s.trunkHeight() < MAX_TRUNK_HEIGHT && canPlaceGrowthBlock(level, s.trunkTop().above());
    }

    private List<ArmGrowthTarget> findExtendableArms(LevelReader level, CactusStructure s) {
        List<ArmGrowthTarget> targets = new ArrayList<>();
        for (int i = 0; i < s.armOrigins().size(); i++) {
            BlockPos origin = s.armOrigins().get(i);
            Direction dir = s.armDirs().get(i);
            BlockPos seg1 = origin.relative(dir);
            BlockPos seg2 = seg1.relative(dir);
            BlockState seg2State = level.getBlockState(seg2);

            if (!seg2State.is(this)) {
                // Arm only has 1 horizontal segment so far — complete the 2-block horizontal span
                if (canPlaceGrowthBlock(level, seg2)) {
                    targets.add(new ArmGrowthTarget(seg2, dir));
                }
            } else {
                // Arm has its 2 horizontal segments; check vertical riser up to MAX_ARM_VERTICAL_HEIGHT
                int armVert = 0;
                BlockPos tip = seg2.above();
                while (armVert < MAX_ARM_VERTICAL_HEIGHT && level.getBlockState(tip).is(this)) {
                    armVert++;
                    tip = tip.above();
                }
                if (armVert < MAX_ARM_VERTICAL_HEIGHT && canPlaceGrowthBlock(level, tip)) {
                    targets.add(new ArmGrowthTarget(tip, Direction.UP));
                }
            }
        }
        return targets;
    }

    private List<NewArmCandidate> findNewArmCandidates(LevelReader level, CactusStructure s) {
        List<NewArmCandidate> candidates = new ArrayList<>();
        if (s.armCount() >= MAX_ARMS || s.trunkHeight() < 3) {
            return candidates;
        }
        // Branches sprout from height index >= 2 and below the trunk tip (matching MyriadCactusFeature)
        int maxBranchH = Math.max(2, s.trunkHeight() - 2);
        for (int h = 2; h <= maxBranchH; h++) {
            if (s.usedArmLevels().contains(h)) {
                continue;
            }
            BlockPos trunkNode = s.trunkBase().above(h);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (s.usedDirs().contains(dir)) {
                    continue;
                }
                BlockPos seg1 = trunkNode.relative(dir);
                BlockPos seg2 = seg1.relative(dir);
                if (canPlaceGrowthBlock(level, seg1) && canPlaceGrowthBlock(level, seg2)) {
                    candidates.add(new NewArmCandidate(trunkNode, dir));
                }
            }
        }
        return candidates;
    }

    private boolean tryGrowStep(ServerLevel level, BlockPos pos, RandomSource random) {
        CactusStructure s = analyzeStructure(level, pos);
        if (s == null) {
            return false;
        }

        boolean trunkCanGrow = canGrowTrunk(level, s);
        List<ArmGrowthTarget> extendableArms = findExtendableArms(level, s);
        List<NewArmCandidate> newArms = findNewArmCandidates(level, s);

        if (!trunkCanGrow && extendableArms.isEmpty() && newArms.isEmpty()) {
            return false;
        }

        // Grow trunk first while young (< 3 blocks tall)
        if (s.trunkHeight() < 3 && trunkCanGrow) {
            placeConnectedSegment(level, s.trunkTop().above(), Direction.UP);
            return true;
        }

        // Build weighted choices among trunk growth, extending existing arms, and sprouting new arms
        int trunkWeight = trunkCanGrow ? (s.trunkHeight() < 5 ? 5 : 3) : 0;
        int extendWeight = !extendableArms.isEmpty() ? 4 : 0;
        int sproutWeight = (!newArms.isEmpty() && s.trunkHeight() >= 4) ? 3 : (!newArms.isEmpty() && !trunkCanGrow ? 3 : 0);
        int totalWeight = trunkWeight + extendWeight + sproutWeight;

        if (totalWeight <= 0) {
            if (trunkCanGrow) {
                placeConnectedSegment(level, s.trunkTop().above(), Direction.UP);
                return true;
            }
            return false;
        }

        int roll = random.nextInt(totalWeight);
        if (roll < trunkWeight) {
            placeConnectedSegment(level, s.trunkTop().above(), Direction.UP);
            return true;
        }
        roll -= trunkWeight;

        if (roll < extendWeight) {
            ArmGrowthTarget target = extendableArms.get(random.nextInt(extendableArms.size()));
            placeConnectedSegment(level, target.pos(), target.facing());
            return true;
        }

        NewArmCandidate arm = newArms.get(random.nextInt(newArms.size()));
        BlockPos seg1 = arm.trunkPos().relative(arm.dir());
        BlockPos seg2 = seg1.relative(arm.dir());
        placeConnectedSegment(level, seg1, arm.dir());
        placeConnectedSegment(level, seg2, arm.dir());
        return true;
    }

    private void placeConnectedSegment(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState placed = this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(UP, level.getBlockState(pos.above()).is(this))
                .setValue(DOWN, level.getBlockState(pos.below()).is(this))
                .setValue(NORTH, level.getBlockState(pos.north()).is(this))
                .setValue(SOUTH, level.getBlockState(pos.south()).is(this))
                .setValue(EAST, level.getBlockState(pos.east()).is(this))
                .setValue(WEST, level.getBlockState(pos.west()).is(this));
        level.setBlock(pos, placed, Block.UPDATE_ALL);
    }
}
