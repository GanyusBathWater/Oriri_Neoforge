package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lightning Storm natural disaster.
 *
 * <p>Forces severe thunderstorm weather across the Overworld and unleashes a heavy barrage
 * of lightning strikes in the surrounding area of travelling players.
 *
 * <p>Player bases and inhabited chunks are strictly protected: strikes only target genuinely
 * fresh land outside of bases, and vanilla lightning rods are respected and prioritized.
 */
public final class LightningStormDisaster {

    private static final double MIN_STRIKE_DIST = 10.0;
    private static final double MAX_STRIKE_DIST = 55.0;
    private static final double CLUSTER_RADIUS = 48.0;

    private static int strikeCountdown = 0;

    private LightningStormDisaster() {
    }

    static void onStart(ServerLevel level, NaturalDisasterManager manager) {
        strikeCountdown = 20; // 1s grace period before the first bolt

        // Synchronize world weather to severe thunderstorm for the disaster's duration
        int weatherDuration = manager.getTicksRemaining() + 200;
        level.setWeatherParameters(0, weatherDuration, true, true);

        if (!OririConfig.COMMON.naturalDisasters.disasterAnnouncements.get()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            boolean targeted = manager.isForced() || scanAround(level, player).isFresh();
            Component msg = targeted
                    ? Component.translatable("message.oririmod.disaster.lightning_storm.start").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.oririmod.disaster.lightning_storm.start_sheltered").withStyle(ChatFormatting.YELLOW);
            player.sendSystemMessage(msg);
        }
    }

    static void onStop(ServerLevel level) {
        strikeCountdown = 0;
        // Calm weather: clear sky for 5 minutes
        level.setWeatherParameters(6000, 0, false, false);
    }

    static void tick(ServerLevel level, NaturalDisasterManager manager) {
        // Keep weather forcibly locked to thunderstorm while event is active
        if (manager.getTicksRemaining() % 100 == 0) {
            level.setWeatherParameters(0, manager.getTicksRemaining() + 200, true, true);
        }

        if (--strikeCountdown > 0) {
            return;
        }

        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        double intensity = cfg.lightningStorm.intensity.get();
        RandomSource random = level.random;

        int baseInterval = cfg.lightningStormStrikeInterval.get();
        int interval = (int) Math.round((baseInterval / Math.max(0.1, intensity)) * (0.35 + random.nextDouble() * 0.35));
        strikeCountdown = Math.max(3, interval);

        List<Vec3> handledCenters = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || isNearHandled(player.position(), handledCenters)) {
                continue;
            }
            if (!manager.isForced() && !scanAround(level, player).isFresh()) {
                continue; // Protected home area
            }
            handledCenters.add(player.position());

            // Heavy multi-strike salvo: 2 to 5 lightning strikes per burst
            int strikeCount = 2 + (int) Math.round(intensity * 1.5) + random.nextInt(2);

            for (int i = 0; i < strikeCount; i++) {
                tryTriggerStrike(level, player, manager.isForced());
            }
        }
    }

    /**
     * Picks a strike location around the player, redirects to lightning rods if present,
     * verifies safety boundaries, and spawns the lightning bolt.
     *
     * @return true if a lightning strike was successfully spawned
     */
    public static boolean tryTriggerStrike(ServerLevel level, ServerPlayer player, boolean ignoreSafety) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        long threshold = cfg.lightningStormMaxInhabitedTicks.get();
        RandomSource random = level.random;

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = MIN_STRIKE_DIST + random.nextDouble() * (MAX_STRIKE_DIST - MIN_STRIKE_DIST);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);

            BlockPos roughPos = new BlockPos(x, (int) player.getY(), z);
            if (!level.isPositionEntityTicking(roughPos)) {
                continue;
            }

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos groundPos = new BlockPos(x, surfaceY, z);

            // Disaster lightning is an apocalyptic, wild phenomenon: strikes hit directly around the player and cannot be dodged with lightning rods!
            BlockPos finalStrikePos = groundPos;

            if (!ignoreSafety && !DisasterSafetyUtil.isImpactSiteFresh(level, finalStrikePos, threshold)) {
                continue; // Skip home chunks
            }

            boolean createFire = cfg.lightningStormStrikesCreateFire.get();
            spawnLightningBolt(level, finalStrikePos, !createFire);
            return true;
        }
        return false;
    }

    private static void spawnLightningBolt(ServerLevel level, BlockPos pos, boolean visualOnly) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(Vec3.atBottomCenterOf(pos));
        bolt.setVisualOnly(visualOnly);
        level.addFreshEntity(bolt);
    }

    public static DisasterSafetyUtil.AreaScan scanAround(ServerLevel level, ServerPlayer player) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        return DisasterSafetyUtil.scan(level, player.blockPosition(),
                cfg.lightningStormFreshAreaRadius.get(), cfg.lightningStormMaxInhabitedTicks.get());
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
