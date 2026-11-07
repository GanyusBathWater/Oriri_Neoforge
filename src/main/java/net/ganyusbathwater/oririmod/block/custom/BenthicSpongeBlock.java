package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayDeque;
import java.util.Queue;

public class BenthicSpongeBlock extends Block {
    public static final int MAX_DEPTH = 10;
    public static final int MAX_COUNT = 256;

    public BenthicSpongeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            this.tryAbsorbWater(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        this.tryAbsorbWater(level, pos);
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    protected void tryAbsorbWater(Level level, BlockPos pos) {
        if (this.removeWaterBreadthFirstSearch(level, pos)) {
            level.setBlock(pos, ModBlocks.WET_BENTHIC_SPONGE.get().defaultBlockState(), 2);
            level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private boolean removeWaterBreadthFirstSearch(Level level, BlockPos startPos) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos);
        int removedCount = 0;

        while (!queue.isEmpty() && removedCount < MAX_COUNT) {
            BlockPos current = queue.poll();

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                BlockState blockState = level.getBlockState(neighbor);
                FluidState fluidState = level.getFluidState(neighbor);

                if (fluidState.is(FluidTags.WATER)) {
                    if (blockState.getBlock() instanceof LiquidBlock) {
                        level.setBlock(neighbor, Blocks.AIR.defaultBlockState(), 3);
                        removedCount++;
                        if (current.distManhattan(startPos) < MAX_DEPTH) {
                            queue.add(neighbor);
                        }
                    } else if (blockState.is(Blocks.KELP) || blockState.is(Blocks.KELP_PLANT) || blockState.is(Blocks.SEAGRASS) || blockState.is(Blocks.TALL_SEAGRASS)) {
                        BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(neighbor) : null;
                        dropResources(blockState, level, neighbor, blockEntity);
                        level.setBlock(neighbor, Blocks.AIR.defaultBlockState(), 3);
                        removedCount++;
                        if (current.distManhattan(startPos) < MAX_DEPTH) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return removedCount > 0;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.benthic_sponge.desc").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.oririmod.benthic_sponge.capacity").withStyle(net.minecraft.ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

