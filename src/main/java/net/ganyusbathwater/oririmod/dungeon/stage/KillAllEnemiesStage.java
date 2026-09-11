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
    protected void doStart(ServerLevel level, DungeonInstance instance) {
        spawnedEntities.clear();

        for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
            Optional<EntityType<?>> typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entityType());
            if (typeOpt.isEmpty()) {
                System.err.println("[OririMod] KillAllEnemiesStage: unknown entity type " + entry.entityType());
                continue;
            }
            EntityType<?> entityType = typeOpt.get();
            for (int i = 0; i < entry.count(); i++) {
                if (level.getRandom().nextFloat() <= entry.chance()) {
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
    }

    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {

        // Check every 20 ticks (1 second) for performance
        if (level.getGameTime() % 20 != 0) return;

        // Remove any UUIDs that are no longer in the world (killed / despawned)
        spawnedEntities.removeIf(uuid -> {
            var entity = level.getEntity(uuid);
            boolean dead = entity == null || !entity.isAlive();
            if (dead && spawnedEntities.size() == 1) {
                String keyDrop = definition.getKeyDropStageId();
                if (keyDrop != null) {
                    net.minecraft.world.item.ItemStack keyStack = new net.minecraft.world.item.ItemStack(net.ganyusbathwater.oririmod.item.ModItems.MANA_DESTABILIZER.get());
                    
                    net.minecraft.world.phys.Vec3 dropPos;
                    if (entity != null) {
                        dropPos = entity.position();
                    } else if (!definition.getTriggers().isEmpty()) {
                        dropPos = net.minecraft.world.phys.Vec3.atCenterOf(definition.getTriggers().get(0).pos());
                    } else {
                        dropPos = net.minecraft.world.phys.Vec3.atCenterOf(instance.getOrigin());
                    }
                    
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, keyStack);
                    level.addFreshEntity(itemEntity);
                }
            }
            return dead;
        });

        if (spawnedEntities.isEmpty()) {
            this.state = StageState.COMPLETE;
        }
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        applyCompletionEffects(level, instance);
    }
}
