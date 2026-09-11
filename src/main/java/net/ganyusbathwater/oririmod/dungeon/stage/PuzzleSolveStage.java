package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.server.level.ServerLevel;

/** PUZZLE_SOLVE stub — placeholder that auto-completes. */
public class PuzzleSolveStage extends AbstractDungeonStage {
    public PuzzleSolveStage(StageDefinition definition) { super(definition); }
    @Override protected void doStart(ServerLevel level, DungeonInstance instance) {}
    @Override protected void doTick(ServerLevel level, DungeonInstance instance) { this.state = StageState.COMPLETE; }
    @Override public void onComplete(ServerLevel level, DungeonInstance instance) { applyCompletionEffects(level, instance); }
}
