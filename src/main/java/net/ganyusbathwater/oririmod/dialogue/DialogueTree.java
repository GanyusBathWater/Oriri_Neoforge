package net.ganyusbathwater.oririmod.dialogue;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * A complete dialogue tree for an NPC conversation.
 * Contains all nodes, the starting node ID, and optional metadata
 * like portrait texture path and talk sound event.
 */
public class DialogueTree {

    private final String id;
    private final String startNodeId;
    private final Map<String, DialogueNode> nodes;
    /** ResourceLocation path for the 2D portrait texture (e.g. "oririmod:textures/gui/portrait/dungeon_keeper.png"). Null = use placeholder. */
    @Nullable
    private final String portrait;
    /** Sound event ID to play as dialogue blip (e.g. "oririmod:dialogue_blip_dungeon_keeper"). Null = silent. */
    @Nullable
    private final String talkSound;

    public DialogueTree(String id, String startNodeId, Map<String, DialogueNode> nodes,
                        @Nullable String portrait, @Nullable String talkSound) {
        this.id = id;
        this.startNodeId = startNodeId;
        this.nodes = Collections.unmodifiableMap(nodes);
        this.portrait = portrait;
        this.talkSound = talkSound;
    }

    public String getId() { return id; }
    public String getStartNodeId() { return startNodeId; }
    public Map<String, DialogueNode> getNodes() { return nodes; }
    @Nullable public String getPortrait() { return portrait; }
    @Nullable public String getTalkSound() { return talkSound; }

    /**
     * Get a specific node by ID.
     * @return The node, or null if not found.
     */
    @Nullable
    public DialogueNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Get the starting node of the conversation.
     */
    @Nullable
    public DialogueNode getStartNode() {
        return nodes.get(startNodeId);
    }
}
