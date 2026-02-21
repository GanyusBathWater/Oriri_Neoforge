package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

public class UpgradedSaplingBlock extends SaplingBlock {

    public static final ThreadLocal<Boolean> IS_FORCING_GROWTH = ThreadLocal.withInitial(() -> false);

    public UpgradedSaplingBlock(TreeGrower treeGrower, Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    public boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Can be placed on almost any solid block, not just dirt/grass
        return state.canOcclude() || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        // Force the tree to grow immediately on the first bonemeal, bypassing the
        // two-stage growth process
        IS_FORCING_GROWTH.set(true);
        try {
            this.treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
        } finally {
            IS_FORCING_GROWTH.remove();
        }
    }
}
