package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.mojang.serialization.MapCodec;

/**
 * Scarlet Tooth Leaves - A damaging plant block.
 * Entities walking through this block take 1 damage per tick,
 * similar to berry bushes but without slowing movement as much.
 */
public class ScarletToothLeavesBlock extends BushBlock {
    public static final MapCodec<ScarletToothLeavesBlock> CODEC = simpleCodec(ScarletToothLeavesBlock::new);

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    public ScarletToothLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            // Slow down the entity slightly
            entity.makeStuckInBlock(state, new Vec3(0.8D, 0.75D, 0.8D));

            // Deal 1 damage per tick (server-side only)
            if (!level.isClientSide) {
                livingEntity.hurt(level.damageSources().cactus(), 1.0F);
            }
        }
    }
}
