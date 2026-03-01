package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ElderwoodsPortalForcer {
    private final ServerLevel level;

    public ElderwoodsPortalForcer(ServerLevel level) {
        this.level = level;
    }

    public Optional<BlockUtil.FoundRectangle> findPortalAround(BlockPos pos, boolean isEntrance,
            WorldBorder worldBorder) {
        int i = isEntrance ? 16 : 128;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = -i; x <= i; x++) {
            for (int z = -i; z <= i; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    mutablePos.set(pos.getX() + x, y, pos.getZ() + z);
                    if (worldBorder.isWithinBounds(mutablePos)
                            && level.getBlockState(mutablePos).is(ModBlocks.ELDERWOODS_PORTAL_BLOCK.get())) {
                        // Minimal implementation of finding the rectangle
                        return Optional.of(new BlockUtil.FoundRectangle(mutablePos.immutable(), 2, 3));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        double d0 = -1.0D;
        BlockPos blockpos = null;
        double d1 = -1.0D;
        BlockPos blockpos1 = null;
        WorldBorder worldborder = this.level.getWorldBorder();
        int i = this.level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        // Simple strategy: Find a solid block at the heightmap and build there
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        if (y < level.getMinBuildHeight())
            y = 64;

        BlockPos targetPos = new BlockPos(pos.getX(), y, pos.getZ());

        // Build the frame
        buildPortalFrame(targetPos, axis);

        return Optional.of(new BlockUtil.FoundRectangle(targetPos.above().immutable(), 2, 3));
    }

    private void buildPortalFrame(BlockPos pos, Direction.Axis axis) {
        BlockState frame = ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState();
        BlockState portal = ModBlocks.ELDERWOODS_PORTAL_BLOCK.get().defaultBlockState()
                .setValue(ElderwoodsPortalBlock.AXIS, axis);

        Direction right = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;

        // Simple 4x5 frame (2x3 opening)
        for (int i = -1; i <= 2; i++) {
            for (int j = -1; j <= 4; j++) {
                BlockPos p = pos.relative(right, i).above(j);
                if (i == -1 || i == 2 || j == -1 || j == 4) {
                    level.setBlockAndUpdate(p, frame);
                } else {
                    level.setBlockAndUpdate(p, portal);
                }
            }
        }
    }
}
