package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import java.util.Random;

/**
 * A stage that immediately evaluates spawns and area modifiers, and instantly completes.
 * Useful for ambient corridors where you want mobs but don't want them blocking progression.
 */
public class PassThroughStage extends AbstractDungeonStage {

    private final Random random = new Random();

    public PassThroughStage(StageDefinition definition) {
        super(definition);
    }

    @Override
    protected void doStart(ServerLevel level, DungeonInstance instance) {
        // Spawn the required entities
        for (StageDefinition.SpawnEntry spawn : definition.getSpawnEntries()) {
            EntityType<?> type = resolveEntityType(level, spawn.entityType(), spawn.isTag());
            if (type == null) {
                continue; // entity not found
            }

            for (int i = 0; i < spawn.count(); i++) {
                if (random.nextFloat() <= spawn.chance()) {
                    BlockPos p = spawn.pos();
                    var entity = type.create(level);
                    if (entity != null) {
                        entity.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360f, 0f);
                        if (entity instanceof Mob mob) {
                            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.SPAWNER, null);
                            mob.setPersistenceRequired(); // Ensure ambient mobs don't despawn naturally
                        }
                        level.addFreshEntity(entity);
                    }
                }
            }
        }
    }

    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {
        // Instantly complete on the first tick!
        this.state = StageState.COMPLETE;
    }

    @Override
    public boolean isComplete() {
        return this.state == StageState.COMPLETE;
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        // Apply area modifiers (bridge building etc)
        applyCompletionEffects(level, instance);
    }

    @Override
    public boolean shouldClearMobsOnComplete() {
        return false;
    }
}
