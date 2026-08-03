package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.server.level.ServerLevel;

/** PUZZLE_SOLVE stub — placeholder that auto-completes. */
public class PuzzleSolveStage extends AbstractDungeonStage {
    public PuzzleSolveStage(StageDefinition definition) { super(definition); }
    @Override public void onStart(ServerLevel level, DungeonInstance instance) {}
    @Override public void tick(ServerLevel level, DungeonInstance instance) { complete = true; }
    @Override public void onComplete(ServerLevel level, DungeonInstance instance) { applyCompletionEffects(level, instance); }
}
