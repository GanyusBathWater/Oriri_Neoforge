package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class MagicBarrierCoreBlock extends Block {
    public MagicBarrierCoreBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        boolean hasKeyItem = stack.is(Items.AMETHYST_SHARD);

        if (!hasKeyItem) {
            // NOT correct item - > Default interaction
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Destroy Core yourself without dropping block
        level.destroyBlock(pos, false);

        // Then all connected barrier blocks
        breakConnectedBarriers(level, pos);

        // Key is consumed
        stack.shrink(1);

        return ItemInteractionResult.sidedSuccess(false);
    }

    private static void breakConnectedBarriers(Level level, BlockPos start) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next)) continue;

                BlockState st = level.getBlockState(next);
                if (st.getBlock() instanceof MagicBarrierBlock) {
                    level.destroyBlock(next, false);
                    queue.addLast(next);
                }
                visited.add(next);
            }
        }
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.is(this) || super.skipRendering(state, adjacentBlockState, side);
    }
}
