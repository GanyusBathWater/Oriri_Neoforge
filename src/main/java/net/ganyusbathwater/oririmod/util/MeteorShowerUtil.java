package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPacket;
import net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.PriorityQueue;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class MeteorShowerUtil {

    private record PendingMeteor(ServerLevel level, BlockPos target, int ownerId, int executeTick) {
    }

    private static final PriorityQueue<PendingMeteor> PENDING_METEORS = new PriorityQueue<>(
            java.util.Comparator.comparingInt(PendingMeteor::executeTick));
    private static int currentTick = 0;
    private static int latestScheduledTick = 0;

    private MeteorShowerUtil() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        synchronized (PENDING_METEORS) {
            while (!PENDING_METEORS.isEmpty()) {
                PendingMeteor peek = PENDING_METEORS.peek();
                if (peek != null && currentTick >= peek.executeTick()) {
                    PendingMeteor wave = PENDING_METEORS.poll();
                    if (wave != null && wave.level() != null) {
                        spawnSingleMeteor(wave.level(), wave.target(), wave.ownerId());
                    }
                } else {
                    break;
                }
            }
        }
    }

    public static void unleash(ServerLevel level, BlockPos target, int ownerId) {
        int meteorCount = 15;
        double radius = 16.0;
        int delayBetweenMeteors = 10; // Spawns one meteor every 10 ticks

        for (int i = 0; i < meteorCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2 * radius;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2 * radius;

            double targetX = target.getX() + 0.5 + offsetX;
            double targetZ = target.getZ() + 0.5 + offsetZ;

            BlockPos roughPos = BlockPos.containing(targetX, target.getY(), targetZ);

            synchronized (PENDING_METEORS) {
                int baseTick = Math.max(currentTick, latestScheduledTick);
                int executeTick = baseTick + delayBetweenMeteors;
                latestScheduledTick = executeTick;
                PENDING_METEORS.add(new PendingMeteor(level, roughPos, ownerId, executeTick));
            }
        }
    }

    private static void spawnSingleMeteor(ServerLevel level, BlockPos roughPos, int ownerId) {
        // Find local ground starting from roughPos
        BlockPos groundPos = roughPos;
        while (groundPos.getY() > level.getMinBuildHeight() && level.isEmptyBlock(groundPos.below())) {
            groundPos = groundPos.below();
        }
        while (groundPos.getY() < level.getMaxBuildHeight() && !level.isEmptyBlock(groundPos)) {
            groundPos = groundPos.above();
        }

        PacketDistributor.sendToAllPlayers(new SpawnAoEIndicatorPayload(
                new SpawnAoEIndicatorPacket(groundPos, 2.0f, 40, 0x88FF0000)));

        MeteorEntity meteor = ModEntities.METEOR.get().create(level);
        if (meteor == null)
            return;

        double targetX = roughPos.getX() + 0.5;
        double targetZ = roughPos.getZ() + 0.5;

        // Trace upwards to find the ceiling so it doesn't get stuck in the cave roof
        int clearance = 0;
        int maxClearance = 15; // Spawns up to 15 blocks high, significantly lower than before
        while (clearance < maxClearance && level.isEmptyBlock(groundPos.above(clearance + 1))) {
            clearance++;
        }

        double spawnY = groundPos.getY() + Math.max(3, clearance) - 0.5;

        meteor.moveTo(targetX, spawnY, targetZ, 0f, 0f);
        meteor.setDeltaMovement(Vec3.ZERO);

        meteor.configure(groundPos, 2.5f, 0);
        meteor.setMeteorScale(2.5f);
        meteor.setDestroysBlocks(false);
        meteor.setMeteorGravity(0.015f);
        if (ownerId > 0) {
            meteor.setOwnerId(ownerId);
        }

        level.addFreshEntity(meteor);
    }
}
