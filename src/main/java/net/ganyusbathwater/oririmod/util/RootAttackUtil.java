package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.RootVisualEntity;
import net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPacket;
import net.ganyusbathwater.oririmod.network.packet.SpawnAoEIndicatorPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.PriorityQueue;

import net.ganyusbathwater.oririmod.OririMod;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public final class RootAttackUtil {

    private record PendingRoot(ServerLevel level, BlockPos target, int ownerId, int executeTick) {
    }

    private static final PriorityQueue<PendingRoot> PENDING_ROOTS = new PriorityQueue<>(
            java.util.Comparator.comparingInt(PendingRoot::executeTick));
    private static int currentTick = 0;

    private static final int DELAY_TICKS = 60; // 3 seconds
    private static final float RADIUS = 2.0f;
    private static final float DAMAGE = 10.0f; // 5 hearts

    private RootAttackUtil() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        synchronized (PENDING_ROOTS) {
            while (!PENDING_ROOTS.isEmpty()) {
                PendingRoot peek = PENDING_ROOTS.peek();
                if (peek != null && currentTick >= peek.executeTick()) {
                    PendingRoot root = PENDING_ROOTS.poll();
                    if (root != null && root.level() != null) {
                        executeRootAttack(root.level(), root.target(), root.ownerId());
                    }
                } else {
                    break;
                }
            }
        }
    }

    public static void unleash(ServerLevel level, BlockPos target, int ownerId) {
        // Find local ground
        BlockPos groundPos = target;
        while (groundPos.getY() > level.getMinBuildHeight() && level.isEmptyBlock(groundPos.below())) {
            groundPos = groundPos.below();
        }
        while (groundPos.getY() < level.getMaxBuildHeight() && !level.isEmptyBlock(groundPos)) {
            groundPos = groundPos.above();
        }

        // Show AoE Indicator (radius 2, lifetime 60 ticks, Green color)
        PacketDistributor.sendToAllPlayers(new SpawnAoEIndicatorPayload(
                new SpawnAoEIndicatorPacket(groundPos, RADIUS, DELAY_TICKS, 0x8800FF00)));

        synchronized (PENDING_ROOTS) {
            PENDING_ROOTS.add(new PendingRoot(level, groundPos, ownerId, currentTick + DELAY_TICKS));
        }
    }

    private static void executeRootAttack(ServerLevel level, BlockPos target, int ownerId) {
        AABB area = new AABB(
                target.getX() - RADIUS, target.getY() - 1, target.getZ() - RADIUS,
                target.getX() + RADIUS, target.getY() + 3, target.getZ() + RADIUS);

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.distanceToSqr(target.getX(), target.getY(), target.getZ()) <= RADIUS * RADIUS);

        // Spawn a center visual entity that just sits there
        spawnVisualVine(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, -1, 80);

        // Spawn a few extra random visual vines within the radius for a denser looking
        // attack
        for (int i = 0; i < 4; i++) {
            double offsetX = (level.random.nextDouble() * 2 - 1) * RADIUS;
            double offsetZ = (level.random.nextDouble() * 2 - 1) * RADIUS;
            spawnVisualVine(level, target.getX() + 0.5 + offsetX, target.getY(), target.getZ() + 0.5 + offsetZ, -1, 80);
        }

        for (LivingEntity entity : entities) {
            if (entity.getId() == ownerId)
                continue; // Don't hit owner

            // Apply Damage
            entity.hurt(level.damageSources().generic(), DAMAGE);

            // Apply Stunned MobEffect for 4 seconds (80 ticks)
            entity.addEffect(new MobEffectInstance(ModEffects.STUNNED_EFFECT, 80, 0, false, true, true));

            // Spawn Visual Entity locking onto this entity, duration 80 to match stun
            spawnVisualVine(level, entity.getX(), entity.getY(), entity.getZ(), entity.getId(), 80);
        }
    }

    private static void spawnVisualVine(ServerLevel level, double x, double y, double z, int targetId, int lifespan) {
        RootVisualEntity baseVisual = ModEntities.ROOT_VISUAL.get().create(level);
        if (baseVisual != null) {
            baseVisual.moveTo(x, y, z, 0, 0);
            baseVisual.setTargetId(targetId);
            baseVisual.setLifespan(lifespan);
            level.addFreshEntity(baseVisual);
        }
    }
}
