package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public class ReachGoalStage extends AbstractDungeonStage {

    public ReachGoalStage(StageDefinition definition) {
        super(definition);
    }

    @Override
    protected void doStart(ServerLevel level, DungeonInstance instance) {
        // Nothing special to spawn for a reach goal stage, 
        // but infinite spawners (if any) are handled by super.
    }

    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {
        // Check for goal reached every 10 ticks to save performance
        if (level.getGameTime() % 10 != 0) return;

        for (StageDefinition.TriggerEntry goal : definition.getGoals()) {
            BlockPos pos = goal.pos();
            double r = goal.radius();
            AABB box = new AABB(
                pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                pos.getX() + r, pos.getY() + r, pos.getZ() + r
            );
            
            boolean playerInside = level.players().stream().anyMatch(p -> 
                instance.hasPlayer(p.getUUID()) && box.contains(p.position())
            );

            if (playerInside) {
                this.state = StageState.COMPLETE;
                return;
            }
        }
    }

    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        applyCompletionEffects(level, instance);
    }
}
