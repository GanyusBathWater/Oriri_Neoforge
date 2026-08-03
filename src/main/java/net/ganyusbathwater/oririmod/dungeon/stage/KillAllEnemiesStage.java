package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Stage: Kill all spawned enemies.
 * Spawns every enemy defined by SPAWN_POINT markers, then waits until they are all dead.
 */
public class KillAllEnemiesStage extends AbstractDungeonStage {

    private final Set<UUID> spawnedEntities = new HashSet<>();

    public KillAllEnemiesStage(StageDefinition definition) {
        super(definition);
    }

    @Override
    public void onStart(ServerLevel level, DungeonInstance instance) {
        spawnedEntities.clear();

        for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
            Optional<EntityType<?>> typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entityType());
            if (typeOpt.isEmpty()) {
                System.err.println("[OririMod] KillAllEnemiesStage: unknown entity type " + entry.entityType());
                continue;
            }
            EntityType<?> entityType = typeOpt.get();
            for (int i = 0; i < entry.count(); i++) {
                var entity = entityType.create(level);
                if (entity instanceof LivingEntity living) {
                    living.moveTo(entry.pos().getX() + 0.5, entry.pos().getY(), entry.pos().getZ() + 0.5,
                            level.getRandom().nextFloat() * 360f, 0f);
                    level.addFreshEntity(living);
                    // finalizeSpawn is called via addFreshEntity naturally
                    spawnedEntities.add(living.getUUID());
                }
            }
        }
    }

    @Override
    public void tick(ServerLevel level, DungeonInstance instance) {
        if (complete) return;

        // Check every 20 ticks (1 second) for performance
        if (level.getGameTime() % 20 != 0) return;

        // Remove any UUIDs that are no longer in the world (killed / despawned)
        spawnedEntities.removeIf(uuid -> {
            var entity = level.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });

        if (spawnedEntities.isEmpty()) {
            complete = true;
        }
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        applyCompletionEffects(level, instance);
    }
}
