package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.worldgen.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.resources.ResourceKey;

import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

public class ElderwoodsPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public ElderwoodsPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch (state.getValue(AXIS)) {
            case Z:
                return Z_AXIS_AABB;
            case X:
            default:
                return X_AXIS_AABB;
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        ResourceKey<Level> resourcekey = level.dimension() == ModDimensions.ELDERWOODS_LEVEL_KEY ? Level.OVERWORLD
                : ModDimensions.ELDERWOODS_LEVEL_KEY;
        ServerLevel serverlevel = level.getServer().getLevel(resourcekey);
        if (serverlevel == null) {
            return null;
        } else {
            ElderwoodsPortalForcer forcer = new ElderwoodsPortalForcer(serverlevel);
            BlockPos targetPos = entity.blockPosition();

            // 1:1 coordinate mapping as approved in the implementation plan
            var optional = forcer.findPortalAround(targetPos, false, serverlevel.getWorldBorder());
            if (optional.isEmpty()) {
                optional = forcer.createPortal(targetPos, level.getBlockState(pos).getValue(AXIS));
            }

            if (optional.isPresent()) {
                BlockPos finalPos = optional.get().minCorner;
                return new DimensionTransition(serverlevel,
                        new Vec3(finalPos.getX() + 0.5, finalPos.getY(), finalPos.getZ() + 0.5),
                        entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(),
                        DimensionTransition.DO_NOTHING);
            }
            return null;
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
            BlockPos currentPos, BlockPos facingPos) {
        Direction.Axis axis = facing.getAxis();
        Direction.Axis portalAxis = state.getValue(AXIS);
        boolean flag = portalAxis != axis && axis.isHorizontal();
        return !flag && !facingState.is(this) && !(new PortalShape(level, currentPos, portalAxis)).isComplete()
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    public static class PortalShape {
        private static final int MIN_WIDTH = 2;
        private static final int MAX_WIDTH = 21;
        private static final int MIN_HEIGHT = 3;
        private static final int MAX_HEIGHT = 21;
        private final LevelAccessor level;
        private final Direction.Axis axis;
        private final Direction rightDir;
        private int numPortalBlocks;
        private BlockPos bottomLeft;
        private int height;
        private final int width;

        public PortalShape(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
            this.level = level;
            this.axis = axis;
            this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
            this.bottomLeft = this.calculateBottomLeft(pos);
            if (this.bottomLeft == null) {
                this.bottomLeft = pos;
                this.width = 1;
                this.height = 1;
            } else {
                this.width = this.calculateWidth();
                if (this.width > 0) {
                    this.height = this.calculateHeight();
                }
            }
        }

        @Nullable
        private BlockPos calculateBottomLeft(BlockPos pos) {
            for (int i = Math.max(this.level.getMinBuildHeight(), pos.getY() - MAX_HEIGHT); pos.getY() > i
                    && this.isEmpty(this.level.getBlockState(pos.below())); pos = pos.below()) {
                ;
            }

            Direction direction = this.rightDir.getOpposite();
            int j = this.getDistanceUntilEdge(pos, direction) - 1;
            return j < 0 ? null : pos.relative(direction, j);
        }

        private int calculateWidth() {
            int i = this.getDistanceUntilEdge(this.bottomLeft, this.rightDir);
            return i >= MIN_WIDTH && i <= MAX_WIDTH ? i : 0;
        }

        private int getDistanceUntilEdge(BlockPos pos, Direction direction) {
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            for (int i = 0; i <= MAX_WIDTH; ++i) {
                mutablePos.set(pos).move(direction, i);
                BlockState blockstate = this.level.getBlockState(mutablePos);
                if (!this.isEmpty(blockstate)) {
                    if (blockstate.is(ModBlocks.MANA_CRYSTAL_BLOCK.get())) {
                        return i;
                    }
                    break;
                }

                BlockState blockstate1 = this.level.getBlockState(mutablePos.move(Direction.DOWN));
                if (!blockstate1.is(ModBlocks.MANA_CRYSTAL_BLOCK.get())) {
                    break;
                }
            }

            return 0;
        }

        private int calculateHeight() {
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            int i = this.getDistanceUntilTop(mutablePos);
            return i >= MIN_HEIGHT && i <= MAX_HEIGHT && this.hasTopFrame(mutablePos, i) ? i : 0;
        }

        private boolean hasTopFrame(BlockPos.MutableBlockPos pos, int height) {
            for (int i = 0; i < this.width; ++i) {
                BlockPos.MutableBlockPos mutablePos = pos.set(this.bottomLeft).move(Direction.UP, height)
                        .move(this.rightDir, i);
                if (!this.level.getBlockState(mutablePos).is(ModBlocks.MANA_CRYSTAL_BLOCK.get())) {
                    return false;
                }
            }

            return true;
        }

        private int getDistanceUntilTop(BlockPos.MutableBlockPos pos) {
            for (int i = 0; i < MAX_HEIGHT; ++i) {
                pos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir.getOpposite(), 1);
                if (!this.level.getBlockState(pos).is(ModBlocks.MANA_CRYSTAL_BLOCK.get())) {
                    return i;
                }

                pos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, this.width);
                if (!this.level.getBlockState(pos).is(ModBlocks.MANA_CRYSTAL_BLOCK.get())) {
                    return i;
                }

                for (int j = 0; j < this.width; ++j) {
                    pos.set(this.bottomLeft).move(Direction.UP, i).move(this.rightDir, j);
                    BlockState blockstate = this.level.getBlockState(pos);
                    if (!this.isEmpty(blockstate)) {
                        return i;
                    }

                    if (blockstate.is(ModBlocks.ELDERWOODS_PORTAL_BLOCK.get())) {
                        ++this.numPortalBlocks;
                    }
                }
            }

            return MAX_HEIGHT;
        }

        private boolean isEmpty(BlockState state) {
            return state.isAir() || state.is(ModBlocks.ELDERWOODS_PORTAL_BLOCK.get());
        }

        public boolean isComplete() {
            return this.bottomLeft != null && this.width >= MIN_WIDTH && this.width <= MAX_WIDTH
                    && this.height >= MIN_HEIGHT && this.height <= MAX_HEIGHT;
        }

        public void createPortalBlocks() {
            BlockState blockstate = ModBlocks.ELDERWOODS_PORTAL_BLOCK.get().defaultBlockState().setValue(AXIS,
                    this.axis);
            BlockPos.betweenClosed(this.bottomLeft,
                    this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
                    .forEach((pos) -> {
                        this.level.setBlock(pos, blockstate, 18);
                    });
        }
    }
}
