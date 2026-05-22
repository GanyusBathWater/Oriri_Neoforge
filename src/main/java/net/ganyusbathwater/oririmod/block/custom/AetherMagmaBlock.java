package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

public class AetherMagmaBlock extends MagmaBlock {
    private static final float BASE_DAMAGE = 4.0F;
    private static final int DAMAGE_INTERVAL_TICKS = 40; // 2 seconds
    private static final int RESET_AFTER_TICKS = 100;

    private static final Map<Entity, Float> NEXT_DAMAGE = new WeakHashMap<>();
    private static final Map<Entity, Integer> LAST_HIT_TICK = new WeakHashMap<>();

    public AetherMagmaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity.isAlive()) {
            final int now = entity.tickCount;

            Integer last = LAST_HIT_TICK.get(entity);
            if (last != null && now - last > RESET_AFTER_TICKS) {
                NEXT_DAMAGE.put(entity, BASE_DAMAGE);
            }

            if (now % DAMAGE_INTERVAL_TICKS == 0) {
                float damage = NEXT_DAMAGE.getOrDefault(entity, BASE_DAMAGE);
                entity.hurt(level.damageSources().generic(), damage);

                LAST_HIT_TICK.put(entity, now);
                NEXT_DAMAGE.put(entity, damage * 1.15F);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
    }
}
