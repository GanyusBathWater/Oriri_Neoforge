package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.entity.IcicleEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

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
public final class IcicleStormUtil {

    private static final int SPAWN_HEIGHT = 10; // blocks above target
    private static final int DELAY_BETWEEN_WAVES = 20; // 1 second between each wave (4 seconds total for 5 waves)

    private static final double[] RING_RADII = { 0, 3, 5, 7, 9, 11 };
    // Icicle count per ring
    private static final int[] ICICLES_PER_RING = { 1, 6, 10, 14, 18, 22 };

    private static final Queue<PendingWave> PENDING_WAVES = new ConcurrentLinkedQueue<>();

    private record PendingWave(ServerLevel level, BlockPos target, int wave, int ownerId, int executeTick) {
    }

    private IcicleStormUtil() {
    }

    public static void unleash(ServerLevel level, BlockPos target, LivingEntity owner, int levelOfWeapon) {
        int ownerId = owner != null ? owner.getId() : 0;
        int waveCount = levelOfWeapon >= 2 ? 6 : 5;
        boolean fastFall = levelOfWeapon >= 3;

        for (int wave = 0; wave < waveCount; wave++) {
            spawnWave(level, target, wave, ownerId, fastFall);
        }
    }

    private static void spawnWave(ServerLevel level, BlockPos target, int wave, int ownerId, boolean fastFall) {
        double radius = RING_RADII[wave];
        int count = ICICLES_PER_RING[wave];

        if (wave == 0) {
            spawnIcicle(level, target, target.getX() + 0.5, target.getZ() + 0.5, ownerId, wave, fastFall);
        } else {
            for (int i = 0; i < count; i++) {
                double angle = (2.0 * Math.PI * i) / count;
                double x = target.getX() + 0.5 + Math.cos(angle) * radius;
                double z = target.getZ() + 0.5 + Math.sin(angle) * radius;
                spawnIcicle(level, target, x, z, ownerId, wave, fastFall);
            }
        }
    }

    private static void spawnIcicle(ServerLevel level, BlockPos target, double x, double z, int ownerId, int wave, boolean fastFall) {
        IcicleEntity icicle = ModEntities.ICICLE.get().create(level);
        if (icicle == null)
            return;

        double spawnY = target.getY() + SPAWN_HEIGHT;
        icicle.moveTo(x, spawnY, z, 0f, 0f);
        icicle.setDeltaMovement(Vec3.ZERO); // Gravity handled after floating phase
        icicle.configure(target);
        icicle.setOwnerId(ownerId);
        icicle.setFastFall(fastFall);

        int delay = fastFall ? (DELAY_BETWEEN_WAVES / 2) : DELAY_BETWEEN_WAVES;
        int baseFloat = fastFall ? 15 : 30;
        int floatTicks = baseFloat + (wave * delay);
        icicle.setFloatingTicks(floatTicks);

        level.addFreshEntity(icicle);
    }
}
