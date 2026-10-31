package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Controller for the Snowstorm Natural Disaster.
 * Blankets cold biomes across the dimension in an extreme blizzard.
 * Features:
 * - Severe weather synchronization.
 * - Dynamic shelter evaluation via {@link SnowstormShelterEvaluator} (cave and 3D room enclosure).
 * - Full-screen whiteout fog and cascade blizzard overlay via {@link NetworkHandler#sendBlizzardOverlayToPlayer}.
 * - Progressive {@link ModEffects#COLD_AURA_EFFECT} and powder snow freezing on exposed entities.
 * - Calming safe-zone ambient atmosphere when sheltered.
 */
public final class SnowstormDisaster {

    private SnowstormDisaster() {}

    public static void onStart(ServerLevel level, int duration) {
        if (level.dimension() != Level.OVERWORLD) return;

        level.setWeatherParameters(0, duration, true, true);

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.oririmod.disaster.snowstorm.start")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                false
        );
    }

    public static void onStop(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;

        level.setWeatherParameters(6000, 0, false, false);

        // Clear blizzard overlays on stop
        for (ServerPlayer player : level.players()) {
            NetworkHandler.sendBlizzardOverlayToPlayer(player, false, false);
            player.getPersistentData().putInt("ColdAuraExposureTicks", 0);
        }
    }

    public static void tick(ServerLevel level, int ticksRemaining, int totalDuration) {
        if (level.dimension() != Level.OVERWORLD) return;

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        for (ServerPlayer player : players) {
            if (player.isSpectator() || !player.isAlive()) continue;

            boolean inColdBiome = SnowstormShelterEvaluator.isColdBiome(level, player.blockPosition());
            if (!inColdBiome) {
                NetworkHandler.sendBlizzardOverlayToPlayer(player, false, false);
                if (player.getPersistentData().getInt("ColdAuraExposureTicks") > 0) {
                    player.getPersistentData().putInt("ColdAuraExposureTicks", 0);
                }
                continue;
            }

            boolean sheltered = SnowstormShelterEvaluator.isSheltered(level, player.blockPosition());

            if (sheltered) {
                NetworkHandler.sendBlizzardOverlayToPlayer(player, false, true);
                player.getPersistentData().putInt("ColdAuraExposureTicks", 0);
            } else {
                NetworkHandler.sendBlizzardOverlayToPlayer(player, true, false);

                // Buildup Cold Aura exposure (gentle progression: 120 ticks = 6 seconds per amplifier, capped at amp 2)
                int speed = (int) Math.max(1, Math.round(OririConfig.COMMON.naturalDisasters.snowstormExposureSpeed.get()));
                int currentTicks = player.getPersistentData().getInt("ColdAuraExposureTicks");
                currentTicks += Math.max(1, speed);

                if (currentTicks >= 120) {
                    currentTicks = 0;
                    MobEffectInstance existing = player.getEffect(ModEffects.COLD_AURA_EFFECT);
                    int nextAmp = existing != null ? Math.min(existing.getAmplifier() + 1, 2) : 0;
                    player.removeEffect(ModEffects.COLD_AURA_EFFECT);
                    player.addEffect(new MobEffectInstance(ModEffects.COLD_AURA_EFFECT, 200, nextAmp, false, true, true));
                }
                player.getPersistentData().putInt("ColdAuraExposureTicks", currentTicks);

                // Ambient snowflake flurries
                level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        player.getX() + (level.random.nextDouble() - 0.5) * 12.0,
                        player.getY() + level.random.nextDouble() * 6.0,
                        player.getZ() + (level.random.nextDouble() - 0.5) * 12.0,
                        6,
                        -1.0, -0.2, 0.4, 0.1
                );

                if (ticksRemaining % 80 == 0) {
                    level.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.WEATHER_RAIN,
                            SoundSource.WEATHER,
                            0.7f,
                            0.3f
                    );
                }
            }
        }

        // Sub-tick freezing for exposed living mobs in cold biomes
        if (ticksRemaining % 40 == 0) {
            for (ServerPlayer player : players) {
                if (!SnowstormShelterEvaluator.isColdBiome(level, player.blockPosition())) continue;

                AABB searchBox = player.getBoundingBox().inflate(32.0);
                List<LivingEntity> mobs = level.getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        m -> !(m instanceof Player) && m.isAlive()
                );

                for (LivingEntity mob : mobs) {
                    if (!SnowstormShelterEvaluator.isSheltered(level, mob.blockPosition())) {
                        MobEffectInstance existing = mob.getEffect(ModEffects.COLD_AURA_EFFECT);
                        int nextAmp = existing != null ? Math.min(existing.getAmplifier() + 1, 2) : 0;
                        mob.addEffect(new MobEffectInstance(ModEffects.COLD_AURA_EFFECT, 140, nextAmp, false, true, true));
                    }
                }
            }
        }
    }
}
