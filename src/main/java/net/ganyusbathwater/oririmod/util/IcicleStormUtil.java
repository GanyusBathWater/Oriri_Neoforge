package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.entity.IcicleEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Utility for spawning an icicle storm attack.
 * <p>
 * Designed to be called from both the Staff of the Eternal Ice and future boss
 * entities.
 * Usage: {@code IcicleStormUtil.unleash(serverLevel, targetPos, ownerEntity);}
 */
@EventBusSubscriber
public final class IcicleStormUtil {

    private static final int SPAWN_HEIGHT = 10; // blocks above target
    private static final double FALL_SPEED = -0.2; // much slower initial downward velocity
    private static final int WAVE_COUNT = 5; // total waves (center + 4 rings)
    private static final int DELAY_BETWEEN_WAVES = 20; // 1 second between each wave (4 seconds total for 5 waves)

    private static final double[] RING_RADII = { 0, 3, 5, 7, 9 };
    // Icicle count per ring
    private static final int[] ICICLES_PER_RING = { 1, 6, 10, 14, 18 };

    // Queue of pending waves
    private static final Queue<PendingWave> PENDING_WAVES = new ConcurrentLinkedQueue<>();

    private record PendingWave(ServerLevel level, BlockPos target, int wave, int ownerId, int executeTick) {
    }

    private IcicleStormUtil() {
    }

    /**
     * Unleash an icicle storm at the given target position.
     * Can be called by both the staff item and a boss entity.
     *
     * @param level  the server level
     * @param target the ground-level block position to target
     * @param owner  the entity that initiated the attack (player or boss); may be
     *               null
     */
    public static void unleash(ServerLevel level, BlockPos target, LivingEntity owner) {
        int ownerId = owner != null ? owner.getId() : 0;

        for (int wave = 0; wave < WAVE_COUNT; wave++) {
            int delay = wave * DELAY_BETWEEN_WAVES;
            int w = wave; // effectively final for lambda

            if (delay == 0) {
                spawnWave(level, target, w, ownerId);
            } else {
                int executeTick = level.getServer().getTickCount() + delay;
                PENDING_WAVES.add(new PendingWave(level, target, w, ownerId, executeTick));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_WAVES.isEmpty())
            return;

        int currentTick = event.getServer().getTickCount();
        Iterator<PendingWave> iterator = PENDING_WAVES.iterator();

        while (iterator.hasNext()) {
            PendingWave pd = iterator.next();
            if (currentTick >= pd.executeTick()) {
                spawnWave(pd.level(), pd.target(), pd.wave(), pd.ownerId());
                iterator.remove();
            }
        }
    }

    private static void spawnWave(ServerLevel level, BlockPos target, int wave, int ownerId) {
        double radius = RING_RADII[wave];
        int count = ICICLES_PER_RING[wave];

        if (wave == 0) {
            // Center icicle
            spawnIcicle(level, target, target.getX() + 0.5, target.getZ() + 0.5, ownerId);
        } else {
            // Ring of icicles
            for (int i = 0; i < count; i++) {
                double angle = (2.0 * Math.PI * i) / count;
                double x = target.getX() + 0.5 + Math.cos(angle) * radius;
                double z = target.getZ() + 0.5 + Math.sin(angle) * radius;
                spawnIcicle(level, target, x, z, ownerId);
            }
        }
    }

    private static void spawnIcicle(ServerLevel level, BlockPos target, double x, double z, int ownerId) {
        IcicleEntity icicle = ModEntities.ICICLE.get().create(level);
        if (icicle == null)
            return;

        double spawnY = target.getY() + SPAWN_HEIGHT;
        icicle.moveTo(x, spawnY, z, 0f, 0f);
        icicle.setDeltaMovement(new Vec3(0, FALL_SPEED, 0));
        icicle.configure(target);
        icicle.setOwnerId(ownerId);

        level.addFreshEntity(icicle);
    }
}
