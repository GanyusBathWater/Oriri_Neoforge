package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.MagicBoltEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.PriorityQueue;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class HellStaffBurstUtil {

    private record PendingBurst(ServerLevel level, LivingEntity owner, boolean explosive, int executeTick) {
    }

    private static final PriorityQueue<PendingBurst> PENDING_BURSTS = new PriorityQueue<>(
            java.util.Comparator.comparingInt(PendingBurst::executeTick));
    private static int currentTick = 0;

    private HellStaffBurstUtil() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        synchronized (PENDING_BURSTS) {
            while (!PENDING_BURSTS.isEmpty()) {
                PendingBurst peek = PENDING_BURSTS.peek();
                if (peek != null && currentTick >= peek.executeTick()) {
                    PendingBurst burst = PENDING_BURSTS.poll();
                    if (burst != null && burst.level() != null && burst.owner() != null && burst.owner().isAlive()) {
                        spawnFireball(burst.level(), burst.owner(), burst.explosive());
                    }
                } else {
                    break;
                }
            }
        }
    }

    public static void unleash(ServerLevel level, LivingEntity owner, int weaponLevel) {
        boolean explosive = weaponLevel >= 3;
        int burstCount = weaponLevel >= 2 ? 3 : 1;
        
        // Shoot first one immediately
        spawnFireball(level, owner, explosive);

        if (burstCount > 1) {
            synchronized (PENDING_BURSTS) {
                // Schedule remaining fireballs like a blaze (6 ticks apart)
                PENDING_BURSTS.add(new PendingBurst(level, owner, explosive, currentTick + 6));
                PENDING_BURSTS.add(new PendingBurst(level, owner, explosive, currentTick + 12));
            }
        }
    }

    private static void spawnFireball(ServerLevel level, LivingEntity owner, boolean explosive) {
        MagicBoltEntity bolt = new MagicBoltEntity(level, owner);
        bolt.setAbility(MagicBoltAbility.BLAZE);
        if (explosive) {
            bolt.setExplodesOnHit(true);
        }
        bolt.launchStraight(owner, 2.2F);
        level.addFreshEntity(bolt);
    }
}
