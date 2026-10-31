package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.custom.DisasterTornadoEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Tornado natural disaster.
 *
 * <p>Spawns a massive, wandering procedural vortex that tears up soft blocks,
 * grass, and foliage, and spirals vulnerable entities lacking knockback resistance.
 *
 * <p>Player bases and inhabited chunks are strictly protected.
 */
public final class TornadoDisaster {

    private static final double MIN_SPAWN_DIST = 25.0;
    private static final double MAX_SPAWN_DIST = 60.0;
    private static final double CLUSTER_RADIUS = 64.0;
    private static final int BASE_SPAWN_INTERVAL = 300;

    private static int spawnCountdown = 0;

    private TornadoDisaster() {
    }

    static void onStart(ServerLevel level, NaturalDisasterManager manager) {
        spawnCountdown = 40; // 2s grace period

        if (!OririConfig.COMMON.naturalDisasters.disasterAnnouncements.get()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            boolean targeted = manager.isForced() || scanAround(level, player).isFresh();
            Component msg = targeted
                    ? Component.translatable("message.oririmod.disaster.tornado.start").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.oririmod.disaster.tornado.start_sheltered").withStyle(ChatFormatting.YELLOW);
            player.sendSystemMessage(msg);
        }
    }

    static void onStop(ServerLevel level) {
        spawnCountdown = 0;
        if (level.getServer() != null) {
            for (ServerLevel lvl : level.getServer().getAllLevels()) {
                for (net.minecraft.world.entity.Entity entity : lvl.getAllEntities()) {
                    if (entity instanceof DisasterTornadoEntity tornado) {
                        tornado.startDissipating();
                    }
                }
            }
        }
    }

    static void tick(ServerLevel level, NaturalDisasterManager manager) {
        if (--spawnCountdown > 0) {
            return;
        }

        OririConfig.DisasterSettings settings = NaturalDisasterType.TORNADO.settings();
        double intensity = settings.intensity.get();
        RandomSource random = level.random;

        int interval = (int) Math.round(BASE_SPAWN_INTERVAL / Math.max(0.1, intensity) * (0.8 + random.nextDouble() * 0.4));
        spawnCountdown = Math.max(60, interval);

        List<Vec3> handledCenters = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || isNearHandled(player.position(), handledCenters)) {
                continue;
            }
            if (!manager.isForced() && !scanAround(level, player).isFresh()) {
                continue; // Protected home area
            }
            handledCenters.add(player.position());

            trySpawnTornado(level, player, manager.isForced());
        }
    }

    /**
     * Attempts to spawn a Disaster Tornado in the vicinity of the player on fresh ground.
     *
     * @return true if a tornado was spawned
     */
    public static boolean trySpawnTornado(ServerLevel level, ServerPlayer player, boolean ignoreSafety) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        long threshold = cfg.tornadoMaxInhabitedTicks.get();
        RandomSource random = level.random;

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = MIN_SPAWN_DIST + random.nextDouble() * (MAX_SPAWN_DIST - MIN_SPAWN_DIST);
            double spawnX = player.getX() + Math.cos(angle) * dist;
            double spawnZ = player.getZ() + Math.sin(angle) * dist;

            BlockPos roughPos = new BlockPos((int) Math.floor(spawnX), (int) player.getY(), (int) Math.floor(spawnZ));
            if (!level.isPositionEntityTicking(roughPos)) {
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(spawnX), (int) Math.floor(spawnZ));
            BlockPos ground = new BlockPos((int) Math.floor(spawnX), surfaceY, (int) Math.floor(spawnZ));

            // Don't spawn directly in water
            if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) {
                continue;
            }

            if (!ignoreSafety && !DisasterSafetyUtil.isImpactSiteFresh(level, ground, threshold)) {
                continue;
            }

            spawnTornadoEntity(level, spawnX, surfaceY, spawnZ, ignoreSafety);
            return true;
        }
        return false;
    }

    private static void spawnTornadoEntity(ServerLevel level, double x, double y, double z, boolean ignoreSafety) {
        DisasterTornadoEntity tornado = ModEntities.DISASTER_TORNADO.get().create(level);
        if (tornado == null) {
            return;
        }

        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        double intensity = cfg.tornado.intensity.get();

        int lifespan = (int) (cfg.tornado.duration.get() * (0.6 + level.random.nextDouble() * 0.4));
        float suctionRadius = (float) (Math.max(32.0, cfg.tornadoSuctionRadius.get()) * Math.min(2.0, intensity));
        float funnelHeight = 110.0f + (float) (intensity * 25.0f);
        float baseRadius = 8.5f + (float) (intensity * 2.0f);
        float topRadius = 45.0f + (float) (intensity * 12.0f);
        boolean destroyBlocks = cfg.tornadoDestroyBlocks.get();
        long threshold = cfg.tornadoMaxInhabitedTicks.get();
        float maxHardness = (float) (double) cfg.tornadoMaxBlockHardness.get();

        tornado.setPos(x, y, z);
        tornado.configure(lifespan, suctionRadius, funnelHeight, baseRadius, topRadius,
                destroyBlocks, ignoreSafety, threshold, maxHardness);
        level.addFreshEntity(tornado);
    }

    public static DisasterSafetyUtil.AreaScan scanAround(ServerLevel level, ServerPlayer player) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        return DisasterSafetyUtil.scan(level, player.blockPosition(),
                cfg.tornadoFreshAreaRadius.get(), cfg.tornadoMaxInhabitedTicks.get());
    }

    private static boolean isNearHandled(Vec3 pos, List<Vec3> centers) {
        for (Vec3 c : centers) {
            if (c.distanceToSqr(pos) < CLUSTER_RADIUS * CLUSTER_RADIUS) {
                return true;
            }
        }
        return false;
    }
}

