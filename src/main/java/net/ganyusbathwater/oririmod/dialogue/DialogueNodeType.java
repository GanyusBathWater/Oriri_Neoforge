package net.ganyusbathwater.oririmod.dialogue;

/**
 * The type of a dialogue node in a conversation tree.
 */
public enum DialogueNodeType {
    /** NPC speaks a line of text. */
    NPC_LINE,
    /** Player is presented with choices. */
    PLAYER_CHOICE,
    /** An action is executed (e.g. open a menu, close dialogue). */
    ACTION
}
