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
        // Nur Server-Logik ausführen
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        // ---- Schlüssel-Item ----
        boolean hasKeyItem = stack.is(Items.AMETHYST_SHARD);

        if (!hasKeyItem) {
            // NICHT richtiges Item -> Standard-Interaktion
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Zerstöre Core selbst ohne Block zu droppen
        level.destroyBlock(pos, false);

        // Danach alle verbundenen Barrier-Blöcke
        breakConnectedBarriers(level, pos);

        // Schlüssel wird verbraucht, wenn nicht dann folgende Zeile kommentieren
        stack.shrink(1);

        return ItemInteractionResult.sidedSuccess(false);
    }

    /**
     * BFS über 6 Richtungen, entfernt alle direkt zusammenhängenden MagicBarrierBlocks.
     * Start ist der ehemals besetzte Core-Blockplatz.
     */
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
                    // Zerstöre den Barrier-Block (ohne Drops; bei Bedarf true)
                    level.destroyBlock(next, false);
                    queue.addLast(next);
                }
                visited.add(next);
            }
        }
    }
}
