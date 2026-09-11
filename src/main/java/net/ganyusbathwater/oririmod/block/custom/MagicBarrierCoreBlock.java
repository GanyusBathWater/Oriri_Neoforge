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
    public MagicBarrierCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        boolean hasKeyItem = stack.getItem() instanceof net.ganyusbathwater.oririmod.item.custom.ManaDestabilizerItem;

        if (!hasKeyItem) {
            // NOT correct item - > Default interaction
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        
        breakCoreAndConnectedBarriers(level, pos);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void breakCoreAndConnectedBarriers(Level level, BlockPos pos) {
        level.destroyBlock(pos, false);
        breakConnectedBarriers(level, pos);
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
                if (visited.contains(next))
                    continue;

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
        return adjacentBlockState.getBlock() instanceof MagicBarrierCoreBlock
                || adjacentBlockState.getBlock() instanceof MagicBarrierBlock
                || super.skipRendering(state, adjacentBlockState, side);
    }
}
