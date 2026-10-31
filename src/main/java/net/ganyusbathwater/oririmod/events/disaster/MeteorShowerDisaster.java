package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Meteor Shower natural disaster.
 *
 * <p>Meteors only fall around players whose complete surroundings (default 128 blocks) consist of
 * freshly explored chunks - i.e. players who are travelling. Players at / near their base are never
 * targeted, see {@link DisasterSafetyUtil}.
 */
public final class MeteorShowerDisaster {

    /** Meteors land between these distances from the targeted player. */
    private static final double MIN_TARGET_DIST = 16.0;
    private static final double MAX_TARGET_DIST = 52.0;
    /** Players closer than this to an already handled player share their meteors (no double spawns). */
    private static final double CLUSTER_RADIUS = 48.0;
    private static final int BASE_WAVE_INTERVAL = 45;

    private static int waveCountdown = 0;

    private MeteorShowerDisaster() {
    }

    /** Size classes. Only BIG meteors trigger the black &amp; white anime impact frame. */
    public enum MeteorTier {
        //    scaleMin scaleMax powerMin powerMax fire gravMin gravMax overhaul flash
        SMALL(3.5f, 4.8f, 8.0f, 12.0f, 1, 0.10f, 0.14f, true, false),
        MEDIUM(6.0f, 8.5f, 15.0f, 22.0f, 3, 0.08f, 0.11f, true, false),
        BIG(12.0f, 16.0f, 28.0f, 38.0f, 6, 0.06f, 0.08f, true, true);

        final float scaleMin, scaleMax, powerMin, powerMax;
        final int fireRadius;
        final float gravityMin, gravityMax;
        final boolean visualOverhaul, impactFlash;

        MeteorTier(float scaleMin, float scaleMax, float powerMin, float powerMax, int fireRadius,
                   float gravityMin, float gravityMax, boolean visualOverhaul, boolean impactFlash) {
            this.scaleMin = scaleMin;
            this.scaleMax = scaleMax;
            this.powerMin = powerMin;
            this.powerMax = powerMax;
            this.fireRadius = fireRadius;
            this.gravityMin = gravityMin;
            this.gravityMax = gravityMax;
            this.visualOverhaul = visualOverhaul;
            this.impactFlash = impactFlash;
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    static void onStart(ServerLevel level, NaturalDisasterManager manager) {
        waveCountdown = 40; // 2s grace period after the announcement
        if (!OririConfig.COMMON.naturalDisasters.disasterAnnouncements.get()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            boolean targeted = manager.isForced() || scanAround(level, player).isFresh();
            Component msg = targeted
                    ? Component.translatable("message.oririmod.disaster.meteor_shower.start").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.oririmod.disaster.meteor_shower.start_sheltered").withStyle(ChatFormatting.YELLOW);
            player.sendSystemMessage(msg);
        }
    }

    static void onStop(ServerLevel level) {
        waveCountdown = 0;
    }

    static void tick(ServerLevel level, NaturalDisasterManager manager) {
        if (--waveCountdown > 0) {
            return;
        }
        OririConfig.DisasterSettings settings = NaturalDisasterType.METEOR_SHOWER.settings();
        double intensity = settings.intensity.get();
        RandomSource random = level.random;

        int interval = (int) Math.round(BASE_WAVE_INTERVAL / intensity * (0.6 + random.nextDouble() * 0.8));
        waveCountdown = Math.max(10, interval);

        List<Vec3> handledCenters = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || isNearHandled(player.position(), handledCenters)) {
                continue;
            }
            if (!manager.isForced() && !scanAround(level, player).isFresh()) {
                continue; // player is at / near a home area -> never targeted
            }
            handledCenters.add(player.position());

            int count = stochasticRound(intensity * (0.75 + random.nextDouble() * 0.75), random);
            for (int i = 0; i < count; i++) {
                trySpawnMeteor(level, player, manager.isForced(), rollTier(random));
            }
        }
    }

    // ── Spawning ─────────────────────────────────────────────────────────────

    /**
     * Picks a valid impact site around the player and spawns a meteor of the given tier.
     *
     * @return true if a meteor was spawned
     */
    public static boolean trySpawnMeteor(ServerLevel level, ServerPlayer player, boolean ignoreSafety, MeteorTier tier) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        long threshold = cfg.meteorShowerMaxInhabitedTicks.get();
        RandomSource random = level.random;

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = MIN_TARGET_DIST + random.nextDouble() * (MAX_TARGET_DIST - MIN_TARGET_DIST);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);

            BlockPos column = new BlockPos(x, (int) player.getY(), z);
            if (!level.isPositionEntityTicking(column)) {
                continue;
            }
            // MOTION_BLOCKING_NO_LEAVES: meteors punch through tree canopies onto the actual ground
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, surfaceY - 1, z);

            if (!level.getFluidState(ground).isEmpty()) {
                continue; // no craters in oceans / lakes
            }
            if (!ignoreSafety && !DisasterSafetyUtil.isImpactSiteFresh(level, ground, threshold)) {
                continue;
            }
            if (cfg.meteorShowerAvoidVillages.get() && isNearVillage(level, ground, (int) tier.powerMax + 2)) {
                continue;
            }

            spawnMeteor(level, ground, tier, cfg.meteorShowerDestroyBlocks.get());
            return true;
        }
        return false;
    }

    private static void spawnMeteor(ServerLevel level, BlockPos ground, MeteorTier tier, boolean destroyBlocks) {
        MeteorEntity meteor = ModEntities.METEOR.get().create(level);
        if (meteor == null) {
            return;
        }
        RandomSource random = level.random;
        float scale = lerp(random, tier.scaleMin, tier.scaleMax);
        float power = lerp(random, tier.powerMin, tier.powerMax);
        float gravity = lerp(random, tier.gravityMin, tier.gravityMax);

        meteor.configure(ground, power, tier.fireRadius);
        meteor.setMeteorScale(scale);
        meteor.setMeteorGravity(gravity);
        meteor.setDestroysBlocks(destroyBlocks);
        meteor.setVisualOverhaul(tier.visualOverhaul);
        meteor.setImpactFlash(tier.impactFlash);

        // Diagonal sky entry. The entry point may lie in a chunk that is not ticking (meteor would be
        // discarded / frozen), so retry a few azimuths and fall back to a vertical drop.
        boolean placed = false;
        for (int i = 0; i < 4 && !placed; i++) {
            meteor.spawnAtRandomAngle(ground, 80.0);
            placed = level.isPositionEntityTicking(meteor.blockPosition());
        }
        if (!placed) {
            meteor.setPos(ground.getX() + 0.5, ground.getY() + 80.0, ground.getZ() + 0.5);
            meteor.setDeltaMovement(0.0, -0.05, 0.0);
        }
        level.addFreshEntity(meteor);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public static DisasterSafetyUtil.AreaScan scanAround(ServerLevel level, ServerPlayer player) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        return DisasterSafetyUtil.scan(level, player.blockPosition(),
                cfg.meteorShowerFreshAreaRadius.get(), cfg.meteorShowerMaxInhabitedTicks.get());
    }

    /** Weighted roll using the configured small / medium / big weights (default 50 / 35 / 15). */
    public static MeteorTier rollTier(RandomSource random) {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        int small = cfg.meteorShowerSmallWeight.get();
        int medium = cfg.meteorShowerMediumWeight.get();
        int big = cfg.meteorShowerBigWeight.get();
        int total = small + medium + big;
        if (total <= 0) {
            return MeteorTier.SMALL;
        }
        int roll = random.nextInt(total);
        if (roll < small) return MeteorTier.SMALL;
        if (roll < small + medium) return MeteorTier.MEDIUM;
        return MeteorTier.BIG;
    }

    private static boolean isNearVillage(ServerLevel level, BlockPos pos, int radius) {
        BlockPos[] samples = {pos, pos.offset(radius, 0, 0), pos.offset(-radius, 0, 0), pos.offset(0, 0, radius), pos.offset(0, 0, -radius)};
        for (BlockPos p : samples) {
            if (level.structureManager().getStructureWithPieceAt(p, StructureTags.VILLAGE).isValid()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNearHandled(Vec3 pos, List<Vec3> centers) {
        for (Vec3 c : centers) {
            if (c.distanceToSqr(pos) < CLUSTER_RADIUS * CLUSTER_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private static int stochasticRound(double value, RandomSource random) {
        int base = (int) Math.floor(value);
        return base + (random.nextDouble() < value - base ? 1 : 0);
    }

    private static float lerp(RandomSource random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}

