package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;

/**
 * Stage: Activate a required number of switches (levers/pressure plates/buttons).
 * Also supports infinite enemy spawning as a side effect while the stage is active.
 *
 * The switch interaction itself is handled by DungeonEventHandler.onSwitchActivated(),
 * which calls DungeonInstance.notifySwitchActivated(). This stage just reads the count.
 */
public class ActivateSwitchesStage extends AbstractDungeonStage {

    private final Set<String> activatedSwitchIds = new HashSet<>();
    private final int requiredCount;

    // For infinite spawning side-effect
    private int spawnTimer = 0;
    private static final int SPAWN_INTERVAL_TICKS = 100; // Spawn every 5 seconds

    public ActivateSwitchesStage(StageDefinition definition) {
        super(definition);
        this.requiredCount = definition.getSwitches().size();
    }

    @Override
    public void onStart(ServerLevel level, DungeonInstance instance) {
        activatedSwitchIds.clear();
        spawnTimer = 0;

        // Spawn initial wave of enemies if any SPAWN_POINT markers exist
        for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
            spawnWave(level, entry, 1);
        }
    }

    @Override
    public void tick(ServerLevel level, DungeonInstance instance) {
        if (complete) return;

        // Infinite spawner side-effect
        if (!definition.getSpawnEntries().isEmpty()) {
            spawnTimer++;
            if (spawnTimer >= SPAWN_INTERVAL_TICKS) {
                spawnTimer = 0;
                for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
                    // Spawn 1 of each type per wave (not 'count', that's for initial burst)
                    spawnWave(level, entry, 1);
                }
            }
        }

        // Win check
        if (activatedSwitchIds.size() >= requiredCount) {
            complete = true;
        }
    }

    /** Called by the event handler when the player interacts with a switch block. */
    public void notifySwitchActivated(String switchId) {
        activatedSwitchIds.add(switchId);
    }

    private void spawnWave(ServerLevel level, StageDefinition.SpawnEntry entry, int countOverride) {
        var typeOpt = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entityType());
        if (typeOpt.isEmpty()) return;
        var entityType = typeOpt.get();
        int count = countOverride > 0 ? countOverride : entry.count();
        for (int i = 0; i < count; i++) {
            var entity = entityType.create(level);
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
