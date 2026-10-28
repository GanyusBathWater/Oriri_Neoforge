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

        net.ganyusbathwater.oririmod.dungeon.DungeonInstance instance = null;
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            instance = net.ganyusbathwater.oririmod.dungeon.DungeonManager.get(sl).getInstanceForPlayer(player.getUUID());
        }
        breakCoreAndConnectedBarriers(level, pos, instance);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void breakCoreAndConnectedBarriers(Level level, BlockPos pos, net.ganyusbathwater.oririmod.dungeon.DungeonInstance instance) {
        if (instance != null) {
            instance.recordStageBlockChange(instance.getCurrentStageIndex(), pos, level.getBlockState(pos));
        }
        level.destroyBlock(pos, false);
        breakConnectedBarriers(level, pos, instance);
    }

    private static void breakConnectedBarriers(Level level, BlockPos start, net.ganyusbathwater.oririmod.dungeon.DungeonInstance instance) {
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
                    if (instance != null) {
                        instance.recordStageBlockChange(instance.getCurrentStageIndex(), next, st);
                    }
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
