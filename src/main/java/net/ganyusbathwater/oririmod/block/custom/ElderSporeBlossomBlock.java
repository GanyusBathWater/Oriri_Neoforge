package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ElderSporeBlossomBlock extends SporeBlossomBlock {
    public ElderSporeBlossomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        double d0 = (double)x + random.nextDouble();
        double d1 = (double)y + 0.7D;
        double d2 = (double)z + random.nextDouble();
        
        // Spawn main particle occasionally
        if (random.nextInt(14) == 0) {
            level.addParticle(ModParticles.ELDER_SPORE_BLOSSOM_SPORE_PARTICLE.get(), d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }

        // Spawn ambient falling particles occasionally
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        if (random.nextInt(14) == 0) {
            for (int i = 0; i < 14; ++i) {
                mutable.set(x + random.nextInt(10) - random.nextInt(10), 
                            y - random.nextInt(10), 
                            z + random.nextInt(10) - random.nextInt(10));
                if (level.isEmptyBlock(mutable) && !level.getBlockState(mutable.above()).isSolidRender(level, mutable.above())) {
                    level.addParticle(ModParticles.ELDER_SPORE_BLOSSOM_SPORE_PARTICLE.get(),
                            (double)mutable.getX() + random.nextDouble(), 
                            (double)mutable.getY() + random.nextDouble(), 
                            (double)mutable.getZ() + random.nextDouble(), 
                            0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
}
