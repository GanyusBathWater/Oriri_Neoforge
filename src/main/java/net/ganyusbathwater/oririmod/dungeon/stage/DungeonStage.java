package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.server.level.ServerLevel;

/**
 * Interface for an active, running dungeon stage.
 * Each implementation represents one stage type (kill enemies, pull levers, etc.)
 * and is responsible for:
 *  1. Starting the stage (spawning enemies, locking doors, etc.)
 *  2. Ticking each game tick to check for completion.
 *  3. Completing the stage (opening doors, applying area modifiers, etc.)
 */
public interface DungeonStage {

    /** Called once when the stage begins. Should spawn enemies, lock doors, etc. */
    void onStart(ServerLevel level, DungeonInstance instance);

    /**
     * Called every server tick while this stage is active.
     * Implementations should check their win condition here.
     */
    void tick(ServerLevel level, DungeonInstance instance);

    /** @return true when this stage's win condition is satisfied. */
    boolean isComplete();

    /**
     * Called once when isComplete() returns true.
     * Should open doors, trigger area modifiers, play sounds, etc.
     */
    void onComplete(ServerLevel level, DungeonInstance instance);

    /** @return the definition this stage was created from. */
    StageDefinition getDefinition();
}
