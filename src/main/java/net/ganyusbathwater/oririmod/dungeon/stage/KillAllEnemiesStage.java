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
    private final Set<String> stageKeys = new HashSet<>();
    private int totalEnemies = 0;
    private int ticksSinceLastKill = 0;
    private int lastSize = -1;
    private boolean keysDropped = false;

    public KillAllEnemiesStage(StageDefinition definition) {
        super(definition);
    }

    public Set<UUID> getSpawnedEntities() {
        return spawnedEntities;
    }

    @Override
    protected void doStart(ServerLevel level, DungeonInstance instance) {
        spawnedEntities.clear();
        stageKeys.clear();
        keysDropped = false;

        for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
            EntityType<?> entityType = resolveEntityType(level, entry.entityType(), entry.isTag());
            if (entityType == null) {
                System.err.println("[OririMod] KillAllEnemiesStage: unknown entity type or empty tag " + entry.entityType());
                continue;
            }
            for (int i = 0; i < entry.count(); i++) {
                if (level.getRandom().nextFloat() <= entry.chance()) {
                    var entity = entityType.create(level);
                    if (entity instanceof LivingEntity living) {
                        living.moveTo(entry.pos().getX() + 0.5, entry.pos().getY(), entry.pos().getZ() + 0.5,
                                level.getRandom().nextFloat() * 360f, 0f);
                        if (living instanceof net.minecraft.world.entity.Mob mob) {
                            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWNER, null);
                        }
                        level.addFreshEntity(living);
                        spawnedEntities.add(living.getUUID());
                        if (entry.keyDropItem() != null) {
                            stageKeys.add(entry.keyDropItem());
                        }
                    }
                }
            }
        }
        totalEnemies = spawnedEntities.size();
    }

    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {

        // Check every 20 ticks (1 second) for performance
        if (level.getGameTime() % 20 != 0) return;

        // Remove any UUIDs that are no longer in the world (killed / despawned)
        spawnedEntities.removeIf(uuid -> {
            var entity = level.getEntity(uuid);
            boolean dead = entity == null || !entity.isAlive();
            if (dead) {
                if (spawnedEntities.size() == 1 && !keysDropped) {
                    dropAllStageKeys(level, entity, instance);
                }
            }
            return dead;
        });

        int currentSize = spawnedEntities.size();
        if (currentSize != lastSize) {
            ticksSinceLastKill = 0;
            lastSize = currentSize;
        } else if (currentSize > 0) {
            ticksSinceLastKill += 20;
            if (ticksSinceLastKill >= 2400) { // 2 minutes
                for (UUID uuid : new java.util.ArrayList<>(spawnedEntities)) {
                    var entity = level.getEntity(uuid);
                    if (entity instanceof LivingEntity living) {
                        living.kill();
                    }
                }
            }
        }

        if (spawnedEntities.isEmpty()) {
            if (!keysDropped) {
                dropAllStageKeys(level, null, instance);
            }
            this.state = StageState.COMPLETE;
        }
    }

    private void dropAllStageKeys(ServerLevel level, net.minecraft.world.entity.Entity lastEntity, DungeonInstance instance) {
        if (keysDropped) return;
        keysDropped = true;
        for (String key : stageKeys) {
            dropKey(level, lastEntity, instance, key);
        }
        String globalKey = definition.getKeyDropStageId();
        if (globalKey != null) {
            dropKey(level, lastEntity, instance, globalKey);
        }
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        if (!keysDropped) {
            // Ensure keys still drop when force-completed via Dungeon Skipper
            net.minecraft.world.entity.Entity dropTarget = null;
            for (UUID pId : instance.getPlayers()) {
                dropTarget = level.getServer().getPlayerList().getPlayer(pId);
                if (dropTarget != null) break;
            }
            dropAllStageKeys(level, dropTarget, instance);
        }
        applyCompletionEffects(level, instance);
    }

    @Override
    public void cleanup(ServerLevel level, DungeonInstance instance) {
        for (UUID uuid : spawnedEntities) {
            var ent = level.getEntity(uuid);
            if (ent != null) ent.discard();
        }
        spawnedEntities.clear();
        super.cleanup(level, instance);
    }

    @Override
    public java.util.List<String> getDiagnosticLines(ServerLevel level, DungeonInstance instance) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("§7Enemies Remaining: §e" + spawnedEntities.size() + " §7/ §f" + totalEnemies);
        for (UUID uuid : spawnedEntities) {
            var ent = level.getEntity(uuid);
            if (ent != null) {
                lines.add(String.format("  §c• %s §7at §f[%d, %d, %d] §7(HP: %.1f)",
                        ent.getType().getDescription().getString(),
                        ent.blockPosition().getX(), ent.blockPosition().getY(), ent.blockPosition().getZ(),
                        ent instanceof LivingEntity le ? le.getHealth() : 0f));
            } else {
                lines.add("  §8• Unloaded/Missing Entity UUID: " + uuid);
            }
        }
        if (!stageKeys.isEmpty() || definition.getKeyDropStageId() != null) {
            lines.add("§7Key Drops Configured: §b" + stageKeys + (definition.getKeyDropStageId() != null ? " + " + definition.getKeyDropStageId() : "") + " §7(Dropped: " + keysDropped + ")");
        }
        return lines;
    }

    private void dropKey(ServerLevel level, net.minecraft.world.entity.Entity entity, DungeonInstance instance, String keyDrop) {
        String cleanKeyDrop = keyDrop.toLowerCase().replace(' ', '_');
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(cleanKeyDrop);
        net.minecraft.world.item.Item dropItem = net.minecraft.world.item.Items.AIR;
        if (rl != null) {
            dropItem = BuiltInRegistries.ITEM.get(rl);
        }
        if (dropItem == net.minecraft.world.item.Items.AIR) {
            dropItem = net.ganyusbathwater.oririmod.item.ModItems.MANA_DESTABILIZER.get();
        }
        net.minecraft.world.item.ItemStack keyStack = new net.minecraft.world.item.ItemStack(dropItem);
        
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

    @Override
    public String getProgressText() {
        if (state != StageState.ACTIVE || totalEnemies == 0) return null;
        int defeated = totalEnemies - spawnedEntities.size();
        return "Enemies Defeated: " + defeated + " / " + totalEnemies;
    }
}
