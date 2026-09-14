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
    private net.minecraft.server.level.ServerBossEvent bossEvent = null;

    public BossFightStage(StageDefinition definition) {
        super(definition);
    }

    @Override
    protected void doStart(ServerLevel level, DungeonInstance instance) {
        if (definition.getBossEntityType() == null || definition.getBossSpawnPos() == null) {
            System.err.println("[OririMod] BossFightStage: missing boss_type or boss_spawn in stage " + definition.getStageId());
            this.state = StageState.COMPLETE; // Skip broken stage
            return;
        }

        var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(definition.getBossEntityType());
        if (typeOpt.isEmpty()) {
            System.err.println("[OririMod] BossFightStage: unknown boss entity type " + definition.getBossEntityType());
            this.state = StageState.COMPLETE;
            return;
        }

        var entity = typeOpt.get().create(level);
        if (entity instanceof LivingEntity boss) {
            var spawnPos = definition.getBossSpawnPos();
            boss.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0f, 0f);
            if (boss instanceof net.minecraft.world.entity.Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWNER, null);
            }
            level.addFreshEntity(boss);
            bossEntityUUID = boss.getUUID();
            
            bossEvent = new net.minecraft.server.level.ServerBossEvent(
                    boss.getDisplayName(),
                    net.minecraft.world.BossEvent.BossBarColor.RED,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS
            );
            for (UUID playerId : instance.getPlayers()) {
                net.minecraft.server.level.ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                if (sp != null) bossEvent.addPlayer(sp);
            }
        }
    }

    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {
        if (bossEntityUUID == null) return;

        var entity = level.getEntity(bossEntityUUID);
        if (entity instanceof LivingEntity boss) {
            if (bossEvent != null) {
                bossEvent.setProgress(boss.getHealth() / boss.getMaxHealth());
                
                // Sync players occasionally
                if (level.getGameTime() % 20 == 0) {
                    for (UUID playerId : instance.getPlayers()) {
                        net.minecraft.server.level.ServerPlayer sp = level.getServer().getPlayerList().getPlayer(playerId);
                        if (sp != null && !bossEvent.getPlayers().contains(sp)) {
                            bossEvent.addPlayer(sp);
                        }
                    }
                }
            }
            if (!boss.isAlive()) {
                String keyDrop = definition.getBossKeyDropItem();
                if (keyDrop != null) {
                    dropKey(level, boss, instance, keyDrop);
                }
                this.state = StageState.COMPLETE;
            }
        } else {
            this.state = StageState.COMPLETE;
        }
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
            bossEvent = null;
        }
        applyCompletionEffects(level, instance);
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
        } else {
            dropPos = net.minecraft.world.phys.Vec3.atCenterOf(instance.getOrigin());
        }
        
        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, keyStack);
        level.addFreshEntity(itemEntity);
    }
}
