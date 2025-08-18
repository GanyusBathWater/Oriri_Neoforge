package net.ganyusbathwater.oririmod.block.custom;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class MagicBarrierBlock extends Block {

    public MagicBarrierBlock(Properties properties) {
        super(properties);
    }


    public static void destroyConnectedBarriers(ServerLevel level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> toVisit = new HashSet<>();

        // Start mit allen Nachbarn
        for (Direction dir : Direction.values()) {
            toVisit.add(startPos.relative(dir));
        }

        while (!toVisit.isEmpty()) {
            BlockPos current = toVisit.iterator().next();
            toVisit.remove(current);

            if (!visited.add(current)) continue; // schon besucht

            BlockState state = level.getBlockState(current);
            if (state.getBlock() instanceof MagicBarrierBlock) {
                // Block zerstören
                level.destroyBlock(current, false);

                // neue Nachbarn hinzufügen
                for (Direction dir : Direction.values()) {
                    toVisit.add(current.relative(dir));
                }
            }
        }
    }
}

    /*
    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        world.updateNeighbors(pos, this);
        super.onBroken(world, pos, state);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean notify) {
        if (!world.isClient) {
            // Prüfe, ob der Nachbarblock an fromPos jetzt Luft ist (also zerstört)
            if (neighborBlock instanceof MagicBarrierBlock || neighborBlock instanceof MagicBarrierCoreBlock) {
                if (world.getBlockState(fromPos).isAir()) {
                    world.breakBlock(pos, false);
                }
            }
        }
    }
    */
