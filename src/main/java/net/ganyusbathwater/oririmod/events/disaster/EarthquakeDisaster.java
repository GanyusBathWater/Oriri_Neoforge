package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.custom.EarthquakeFaultWorkerEntity;
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
 * Earthquake natural disaster.
 *
 * <p>Spawns progressive, organic fissures tearing through the landscape around players
 * who are travelling across fresh land. Home chunks and previously inhabited bases are
 * strictly protected by the 128-block safety scan.
 */
public final class EarthquakeDisaster {

    private static final double MIN_SPAWN_DIST = 20.0;
    private static final double MAX_SPAWN_DIST = 55.0;
    private static final double CLUSTER_RADIUS = 64.0;
    private static final int BASE_FISSURE_INTERVAL = 24;

    private static int fissureCountdown = 0;

    private EarthquakeDisaster() {
    }

    static void onStart(ServerLevel level, NaturalDisasterManager manager) {
        fissureCountdown = 10; // Brief grace period
        if (!OririConfig.COMMON.naturalDisasters.disasterAnnouncements.get()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            boolean targeted = manager.isForced() || scanAround(level, player).isFresh();
            Component msg = targeted
                    ? Component.translatable("message.oririmod.disaster.earthquake.start").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.oririmod.disaster.earthquake.start_sheltered").withStyle(ChatFormatting.YELLOW);
            player.sendSystemMessage(msg);
        }
    }

    static void onStop(ServerLevel level) {
        fissureCountdown = 0;
        if (level.getServer() != null) {
            for (ServerLevel lvl : level.getServer().getAllLevels()) {
                for (net.minecraft.world.entity.Entity entity : lvl.getAllEntities()) {
                    if (entity instanceof net.ganyusbathwater.oririmod.entity.custom.EarthquakeFaultWorkerEntity worker) {
                        worker.discard();
                    }
                }
            }
        }
    }

    static void tick(ServerLevel level, NaturalDisasterManager manager) {
        if (--fissureCountdown > 0) {
            return;
        }
        OririConfig.DisasterSettings settings = NaturalDisasterType.EARTHQUAKE.settings();
        double intensity = settings.intensity.get();
        RandomSource random = level.random;

        int interval = (int) Math.round((BASE_FISSURE_INTERVAL / Math.max(0.2, intensity)) * (0.8 + random.nextDouble() * 0.4));
        fissureCountdown = Math.max(12, interval);

        List<Vec3> handledCenters = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || isNearHandled(player.position(), handledCenters)) {
                continue;
            }
            if (!manager.isForced() && !scanAround(level, player).isFresh()) {
                continue; // Protected home area
            }
            handledCenters.add(player.position());

            // Continuous tremor barrages: trigger 1-2 fault workers across multiple angles
            int count = random.nextFloat() < 0.5f ? 2 : 1;
            for (int i = 0; i < count; i++) {
                tryTriggerEarthquake(level, player, manager.isForced());
            }
        }
    }

    /**
     * Picks an epicenter around the player and spawns an organic fault line worker.
     *
     * @return true if an earthquake fissure was successfully started
     */
    public static boolean tryTriggerEarthquake(ServerLevel level, ServerPlayer player, boolean ignoreSafety) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        long threshold = cfg.earthquakeMaxInhabitedTicks.get();
        RandomSource random = level.random;

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = MIN_SPAWN_DIST + random.nextDouble() * (MAX_SPAWN_DIST - MIN_SPAWN_DIST);
            double startX = player.getX() + Math.cos(angle) * dist;
            double startZ = player.getZ() + Math.sin(angle) * dist;

            BlockPos originPos = new BlockPos((int) Math.floor(startX), (int) player.getY(), (int) Math.floor(startZ));
            if (!level.isPositionEntityTicking(originPos)) {
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(startX), (int) Math.floor(startZ));
            BlockPos ground = new BlockPos((int) Math.floor(startX), surfaceY - 1, (int) Math.floor(startZ));

            // Don't start fissures inside water bodies
            if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.above()).isEmpty()) {
                continue;
            }

            if (!ignoreSafety && !DisasterSafetyUtil.isImpactSiteFresh(level, ground, threshold)) {
                continue;
            }

            spawnFaultWorker(level, startX, startZ, ignoreSafety);
            return true;
        }
        return false;
    }

    private static void spawnFaultWorker(ServerLevel level, double startX, double startZ, boolean ignoreSafety) {
        EarthquakeFaultWorkerEntity worker = ModEntities.EARTHQUAKE_WORKER.get().create(level);
        if (worker == null) {
            return;
        }
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        RandomSource random = level.random;

        double initialAzimuth = random.nextDouble() * Math.PI * 2.0;
        double intensity = cfg.earthquake.intensity.get();

        float baseLength = cfg.earthquakeCrackLength.get();
        float targetLength = (float) (baseLength * intensity * (0.85 + random.nextDouble() * 0.3));

        int baseDepth = cfg.earthquakeCrackDepth.get();
        int maxDepth = (int) Math.round(baseDepth * Math.min(2.0, intensity) * (0.85 + random.nextDouble() * 0.3));

        float maxWidth = 3.8f + (float) (intensity * 1.4f);

        boolean destroyBlocks = cfg.earthquakeDestroyBlocks.get();
        long threshold = cfg.earthquakeMaxInhabitedTicks.get();
        long seed = random.nextLong();

        worker.configure(startX, startZ, initialAzimuth, targetLength, maxDepth, maxWidth,
                destroyBlocks, ignoreSafety, threshold, seed, false);
        level.addFreshEntity(worker);
    }

    public static DisasterSafetyUtil.AreaScan scanAround(ServerLevel level, ServerPlayer player) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        return DisasterSafetyUtil.scan(level, player.blockPosition(),
                cfg.earthquakeFreshAreaRadius.get(), cfg.earthquakeMaxInhabitedTicks.get());
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

