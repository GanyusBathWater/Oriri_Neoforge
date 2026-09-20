package net.ganyusbathwater.oririmod.dialogue;

import net.minecraft.server.level.ServerPlayer;

/**
 * Functional interface for actions that can be triggered by a dialogue ACTION node.
 * Actions are registered via DialogueRegistry.registerAction().
 */
@FunctionalInterface
public interface DialogueAction {
    /**
     * Executes the action.
     * @param player The player triggering the action.
     * @param entity The entity the player is talking to.
     */
    void execute(ServerPlayer player, ConversableEntity entity);
}
