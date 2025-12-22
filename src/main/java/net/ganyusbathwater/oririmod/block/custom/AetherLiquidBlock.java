package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.Map;
import java.util.WeakHashMap;

public final class AetherLiquidBlock extends LiquidBlock {
    private static final float BASE_DAMAGE = 4.0F;
    private static final int DAMAGE_INTERVAL_TICKS = 10;
    private static final int RESET_AFTER_TICKS = 100;

    private static final Map<Entity, Float> NEXT_DAMAGE = new WeakHashMap<>();
    private static final Map<Entity, Integer> LAST_HIT_TICK = new WeakHashMap<>();

    public AetherLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // \*vor\* super.tick: so wird der Kontakt im selben Update abgearbeitet,
        // bevor das Fluid noch weiter fließt/Updates verzögert werden.
        if (tryConvertOnContact(level, pos)) {
            return;
        }

        super.tick(state, level, pos, random);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        if (level.isClientSide) return;

        // Sofort-Reaktion zusätzlich behalten
        tryConvertOnContact(level, pos);
    }

    private static boolean isWater(Fluid f) {
        return f == Fluids.WATER || f == Fluids.FLOWING_WATER;
    }

    private static boolean isLava(Fluid f) {
        return f == Fluids.LAVA || f == Fluids.FLOWING_LAVA;
    }

    /**
     * Konvertiert bei Kontakt sofort und räumt optional die Gegen-Seite mit auf,
     * damit nicht erst ein Tick später Wasser/Lava „verschwindet“.
     */
    private boolean tryConvertOnContact(Level level, BlockPos pos) {
        // Für Übereinander-Fälle ist DOWN entscheidend; zusätzlich horizontale Seiten.
        Direction[] checkDirs = new Direction[] {
                Direction.DOWN,
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST,
                Direction.UP
        };

        for (Direction dir : checkDirs) {
            BlockPos otherPos = pos.relative(dir);
            FluidState otherFs = level.getFluidState(otherPos);
            Fluid other = otherFs.getType();

            if (isWater(other)) {
                // Aether + Wasser \=\> Blackstone
                level.setBlockAndUpdate(pos, Blocks.BLACKSTONE.defaultBlockState());

                // Wenn am Nachbar noch Fluid steht (Flowing/Source), direkt entfernen/ersetzen,
                // damit es nicht „1 Tick später“ passiert.
                if (!otherFs.isEmpty()) {
                    level.setBlockAndUpdate(otherPos, Blocks.AIR.defaultBlockState());
                }
                return true;
            }

            if (isLava(other)) {
                // Aether + Lava \=\> Cobblestone
                level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());

                if (!otherFs.isEmpty()) {
                    level.setBlockAndUpdate(otherPos, Blocks.AIR.defaultBlockState());
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        if (level.isClientSide || !entity.isAlive()) return;

        final int now = entity.tickCount;

        Integer last = LAST_HIT_TICK.get(entity);
        if (last != null && now - last > RESET_AFTER_TICKS) {
            NEXT_DAMAGE.put(entity, BASE_DAMAGE);
        }

        if (now % DAMAGE_INTERVAL_TICKS != 0) return;

        float damage = NEXT_DAMAGE.getOrDefault(entity, BASE_DAMAGE);
        entity.hurt(level.damageSources().generic(), damage);

        LAST_HIT_TICK.put(entity, now);
        NEXT_DAMAGE.put(entity, damage * 2.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(120) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.0D;
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.02D, 0.0D);
        }

        if (random.nextInt(250) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.02D;
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.01D, 0.0D);
        }
    }
}