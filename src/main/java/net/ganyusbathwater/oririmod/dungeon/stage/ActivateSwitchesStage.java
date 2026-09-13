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

    public ActivateSwitchesStage(StageDefinition definition) {
        super(definition);
        this.requiredCount = definition.getSwitches().size();
    }

    @Override
    protected void doStart(ServerLevel level, DungeonInstance instance) {
        activatedSwitchIds.clear();
        // Spawn initial wave of enemies if any SPAWN_POINT markers exist
        for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
            spawnWave(level, entry);
        }
    }

    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {

        // Win check
        if (activatedSwitchIds.size() >= requiredCount) {
            this.state = StageState.COMPLETE;
        }
    }

    /** Called by the event handler when the player interacts with a switch block. */
    public void notifySwitchActivated(String switchId) {
        activatedSwitchIds.add(switchId);
    }

    private void spawnWave(ServerLevel level, StageDefinition.SpawnEntry entry) {
        var typeOpt = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entityType());
        if (typeOpt.isEmpty()) return;
        var entityType = typeOpt.get();
        for (int i = 0; i < entry.count(); i++) {
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
