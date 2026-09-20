package net.ganyusbathwater.oririmod.dialogue;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A single node in a dialogue tree.
 *
 * @param id         Unique identifier within the tree (e.g. "greeting", "main_choice").
 * @param type       The node type (NPC_LINE, PLAYER_CHOICE, ACTION).
 * @param speaker    Translation key for the speaker name (only for NPC_LINE). Null otherwise.
 * @param text       Translation key for the dialogue text (only for NPC_LINE). Null otherwise.
 * @param nextNodeId The next node to advance to after this one (null = end of conversation).
 * @param choices    List of choices (only for PLAYER_CHOICE). Empty otherwise.
 * @param action     The action to execute (only for ACTION). Null otherwise.
 */
public record DialogueNode(
        String id,
        DialogueNodeType type,
        @Nullable String speaker,
        @Nullable String text,
        @Nullable String nextNodeId,
        List<DialogueChoice> choices,
        @Nullable String action
) {
}
