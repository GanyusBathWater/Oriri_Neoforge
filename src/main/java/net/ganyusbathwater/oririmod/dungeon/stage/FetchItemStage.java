package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.server.level.ServerLevel;

/**
 * FETCH_ITEM and PUZZLE_SOLVE stubs — to be fully implemented per dungeon need.
 * Currently auto-complete after 1 tick so the system doesn't get stuck.
 */
public class FetchItemStage extends AbstractDungeonStage {
    public FetchItemStage(StageDefinition definition) { super(definition); }
    @Override public void onStart(ServerLevel level, DungeonInstance instance) {}
    @Override public void tick(ServerLevel level, DungeonInstance instance) { complete = true; }
    @Override public void onComplete(ServerLevel level, DungeonInstance instance) { applyCompletionEffects(level, instance); }
}
