package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Stage: Defeat the boss.
 * Spawns the boss entity at the BOSS_SPAWN marker position and tracks its death.
 */
public class BossFightStage extends AbstractDungeonStage {

    private UUID bossEntityUUID = null;

    public BossFightStage(StageDefinition definition) {
        super(definition);
    }

    @Override
    public void onStart(ServerLevel level, DungeonInstance instance) {
        if (definition.getBossEntityType() == null || definition.getBossSpawnPos() == null) {
            System.err.println("[OririMod] BossFightStage: missing boss_type or boss_spawn in stage " + definition.getStageId());
            complete = true; // Skip broken stage
            return;
        }

        var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(definition.getBossEntityType());
        if (typeOpt.isEmpty()) {
            System.err.println("[OririMod] BossFightStage: unknown boss entity type " + definition.getBossEntityType());
            complete = true;
            return;
        }

        var entity = typeOpt.get().create(level);
        if (entity instanceof LivingEntity boss) {
            var spawnPos = definition.getBossSpawnPos();
            boss.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(boss);
            bossEntityUUID = boss.getUUID();
        }
    }

    @Override
    public void tick(ServerLevel level, DungeonInstance instance) {
        if (complete) return;
        if (bossEntityUUID == null) return;

        // Check every 20 ticks
        if (level.getGameTime() % 20 != 0) return;

        var entity = level.getEntity(bossEntityUUID);
        if (entity == null || !entity.isAlive()) {
            complete = true;
        }
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        applyCompletionEffects(level, instance);
    }
}
