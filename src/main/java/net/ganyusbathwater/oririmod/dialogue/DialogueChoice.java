package net.ganyusbathwater.oririmod.dialogue;

/**
 * A single selectable choice in a PLAYER_CHOICE dialogue node.
 *
 * @param text       Translation key for the choice button text.
 * @param nextNodeId The dialogue node ID to jump to when this choice is selected.
 */
public record DialogueChoice(String text, String nextNodeId) {
}
