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
        // Keep any switch already pressed during the brief PENDING window before the trigger poll fired.
        // Spawn initial wave of enemies if any SPAWN_POINT markers exist
        for (StageDefinition.SpawnEntry entry : definition.getSpawnEntries()) {
            spawnWave(level, entry);
        }
    }

    @Override
    public void tick(ServerLevel level, DungeonInstance instance) {
        // If a player already activated a switch while the stage was still PENDING
        // (e.g. before the 10-tick trigger poll fired or from outside a tight trigger box),
        // transition to ACTIVE immediately so DungeonTickHandler runs its activation hooks
        // and the switch activation is counted on the next tick.
        if (this.state == StageState.PENDING && !activatedSwitchIds.isEmpty()) {
            this.state = StageState.ACTIVE;
            applyStartEffects(level, instance);
            doStart(level, instance);
            return;
        }
        super.tick(level, instance);
    }

    @Override
    public void cleanup(ServerLevel level, DungeonInstance instance) {
        activatedSwitchIds.clear();
        super.cleanup(level, instance);
    }

    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {

        // Win check
        if (activatedSwitchIds.size() >= requiredCount) {
            this.state = StageState.COMPLETE;
        }
    }

    /** Called by the event handler when the player interacts with a switch block. */
    public boolean notifySwitchActivated(String switchId) {
        return activatedSwitchIds.add(switchId);
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

    @Override
    public String getProgressText() {
        if (state != StageState.ACTIVE || requiredCount == 0) return null;
        if (activatedSwitchIds.isEmpty()) return null;
        return "Switches Activated: " + activatedSwitchIds.size() + " / " + requiredCount;
    }

    @Override
    public java.util.List<String> getDiagnosticLines(ServerLevel level, DungeonInstance instance) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("§7Switches Activated: §e" + activatedSwitchIds.size() + " §7/ §f" + requiredCount);
        for (StageDefinition.SwitchEntry sw : definition.getSwitches()) {
            boolean active = activatedSwitchIds.contains(sw.switchId());
            lines.add(String.format("  %s• Switch '%s' at [%s]: %s",
                    active ? "§a" : "§c", sw.switchId(), sw.pos().toShortString(), active ? "ACTIVATED" : "MISSING"));
        }
        return lines;
    }
}
