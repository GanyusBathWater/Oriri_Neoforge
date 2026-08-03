package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Stage: Survive for a fixed duration (timerTicks from the definition).
 * Enemies may also spawn infinitely via SPAWN_POINT markers during this time.
 */
public class SurviveTimerStage extends AbstractDungeonStage {

    private int ticksElapsed = 0;
    private int spawnTimer = 0;
    private static final int SPAWN_INTERVAL_TICKS = 100;
    private static final int NOTIFY_INTERVAL_TICKS = 200; // Announce every 10 seconds

    public SurviveTimerStage(StageDefinition definition) {
        super(definition);
    }

    @Override
    public void onStart(ServerLevel level, DungeonInstance instance) {
        ticksElapsed = 0;
        spawnTimer = 0;

        int seconds = definition.getTimerTicks() / 20;
        for (UUID playerId : instance.getPlayers()) {
            ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
            if (sp != null) {
                sp.displayClientMessage(
                        Component.literal("Survive for " + seconds + " seconds!")
                                .withStyle(ChatFormatting.YELLOW),
                        true);
            }
        }
    }

    @Override
    public void tick(ServerLevel level, DungeonInstance instance) {
        if (complete) return;
        ticksElapsed++;

        if (!definition.getSpawnEntries().isEmpty()) {
            spawnTimer++;
            if (spawnTimer >= SPAWN_INTERVAL_TICKS) {
                spawnTimer = 0;
                spawnWave(level);
            }
        }

        if (ticksElapsed % NOTIFY_INTERVAL_TICKS == 0) {
            int secondsLeft = (definition.getTimerTicks() - ticksElapsed) / 20;
            if (secondsLeft > 0) {
                for (UUID playerId : instance.getPlayers()) {
                    ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                    if (sp != null) {
                        sp.displayClientMessage(
                                Component.literal(secondsLeft + "s remaining!")
                                        .withStyle(ChatFormatting.AQUA),
                                true);
                    }
                }
            }
        }

        if (ticksElapsed >= definition.getTimerTicks()) {
            complete = true;
        }
    }

    private void spawnWave(ServerLevel level) {
        for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
            var typeOpt = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entityType());
            if (typeOpt.isEmpty()) continue;
            var entity = typeOpt.get().create(level);
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.moveTo(entry.pos().getX() + 0.5, entry.pos().getY(), entry.pos().getZ() + 0.5,
                        level.getRandom().nextFloat() * 360f, 0f);
                level.addFreshEntity(living);
            }
        }
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        applyCompletionEffects(level, instance);
    }
}
