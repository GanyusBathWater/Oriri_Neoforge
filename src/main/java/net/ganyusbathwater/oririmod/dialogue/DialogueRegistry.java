package net.ganyusbathwater.oririmod.dialogue;

import com.google.gson.*;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that loads dialogue trees from JSON resources (data/<namespace>/dialogue/*.json)
 * and holds the action handlers.
 */
public class DialogueRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Server and Client side trees
    private static final Map<String, DialogueTree> TREES = new ConcurrentHashMap<>();
    
    // Server-side action registry
    private static final Map<String, DialogueAction> ACTION_REGISTRY = new ConcurrentHashMap<>();

    // Singleton instance for the event bus
    public static final DialogueRegistry INSTANCE = new DialogueRegistry();

    private DialogueRegistry() {
        super(GSON, "dialogue");
    }

    public static void init() {
        // Register default actions
        registerAction("OPEN_DUNGEON_SELECTION", (player, entity) -> {
            if (entity instanceof net.ganyusbathwater.oririmod.dungeon.entity.DungeonKeeperEntity keeper) {
                keeper.openDungeonScreenForPlayer(player);
            }
        });
        registerAction("OPEN_DUNGEON_PARTY", (player, entity) -> {
            if (entity instanceof net.ganyusbathwater.oririmod.dungeon.entity.DungeonKeeperEntity keeper) {
                keeper.openDungeonScreenForPlayer(player);
            }
        });
        // Modders can register more actions from their mods!
    }

    /**
     * Register a new dialogue action handler.
     */
    public static void registerAction(String actionId, DialogueAction action) {
        ACTION_REGISTRY.put(actionId, action);
    }

    @Nullable
    public static DialogueAction getAction(String actionId) {
        return ACTION_REGISTRY.get(actionId);
    }

    @Nullable
    public static DialogueTree get(String dialogueId) {
        return TREES.get(dialogueId);
    }

    public static Map<String, DialogueTree> getAll() {
        return TREES;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        TREES.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objectMap.entrySet()) {
            ResourceLocation id = entry.getKey();
            String stringId = id.getPath(); // e.g. "dungeon_keeper"
            try {
                DialogueTree tree = parseTree(stringId, entry.getValue().getAsJsonObject());
                TREES.put(stringId, tree);
                OririMod.LOGGER.debug("[Dialogue] Loaded dialogue tree: {}", stringId);
            } catch (Exception e) {
                OririMod.LOGGER.error("[Dialogue] Error loading {}: {}", id, e.getMessage());
            }
        }
        OririMod.LOGGER.info("[Dialogue] Reloaded {} dialogue trees.", TREES.size());
    }

    // ── Network Sync ──

    public static String serializeAllToJson() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, DialogueTree> entry : TREES.entrySet()) {
            root.add(entry.getKey(), serializeTree(entry.getValue()));
        }
        return GSON.toJson(root);
    }

    public static void loadFromJsonSync(String json) {
        try {
            TREES.clear();
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String id = entry.getKey();
                DialogueTree tree = parseTree(id, entry.getValue().getAsJsonObject());
                TREES.put(id, tree);
            }
            OririMod.LOGGER.info("[Dialogue] Synced {} dialogue trees from server.", TREES.size());
        } catch (Exception e) {
            OririMod.LOGGER.error("[Dialogue] Failed to parse synced dialogue JSON: {}", e.getMessage());
        }
    }

    // ── Internal Parsing ──

    private static DialogueTree parseTree(String id, JsonObject root) {
        String startNode = root.get("start_node").getAsString();
        String portrait = root.has("portrait") ? root.get("portrait").getAsString() : null;
        String talkSound = root.has("talk_sound") ? root.get("talk_sound").getAsString() : null;

        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        JsonObject nodesObj = root.getAsJsonObject("nodes");
        for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
            String nodeId = entry.getKey();
            JsonObject nodeObj = entry.getValue().getAsJsonObject();
            nodes.put(nodeId, parseNode(nodeId, nodeObj));
        }

        // Validate auto-close
        for (DialogueNode node : nodes.values()) {
            if (node.type() == DialogueNodeType.NPC_LINE && node.nextNodeId() == null) {
                OririMod.LOGGER.debug("[Dialogue] Node '{}' in '{}' has no next — will auto-close.", node.id(), id);
            }
        }

        return new DialogueTree(id, startNode, nodes, portrait, talkSound);
    }

    private static DialogueNode parseNode(String nodeId, JsonObject obj) {
        DialogueNodeType type = DialogueNodeType.valueOf(obj.get("type").getAsString());
        String speaker = obj.has("speaker") ? obj.get("speaker").getAsString() : null;
        String text = obj.has("text") ? obj.get("text").getAsString() : null;
        String nextNodeId = obj.has("next") && !obj.get("next").isJsonNull() ? obj.get("next").getAsString() : null;

        List<DialogueChoice> choices = new ArrayList<>();
        if (obj.has("choices")) {
            JsonArray arr = obj.getAsJsonArray("choices");
            for (JsonElement el : arr) {
                JsonObject choiceObj = el.getAsJsonObject();
                choices.add(new DialogueChoice(
                        choiceObj.get("text").getAsString(),
                        choiceObj.get("next").getAsString()
                ));
            }
        }

        String action = obj.has("action") ? obj.get("action").getAsString() : null;
        return new DialogueNode(nodeId, type, speaker, text, nextNodeId, choices, action);
    }

    // ── Internal Serialization ──

    private static JsonObject serializeTree(DialogueTree tree) {
        JsonObject root = new JsonObject();
        root.addProperty("start_node", tree.getStartNodeId());
        if (tree.getPortrait() != null) root.addProperty("portrait", tree.getPortrait());
        if (tree.getTalkSound() != null) root.addProperty("talk_sound", tree.getTalkSound());

        JsonObject nodesObj = new JsonObject();
        for (Map.Entry<String, DialogueNode> entry : tree.getNodes().entrySet()) {
            nodesObj.add(entry.getKey(), serializeNode(entry.getValue()));
        }
        root.add("nodes", nodesObj);
        return root;
    }

    private static JsonObject serializeNode(DialogueNode node) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", node.type().name());
        if (node.speaker() != null) obj.addProperty("speaker", node.speaker());
        if (node.text() != null) obj.addProperty("text", node.text());
        if (node.nextNodeId() != null) obj.addProperty("next", node.nextNodeId());

        if (!node.choices().isEmpty()) {
            JsonArray arr = new JsonArray();
            for (DialogueChoice choice : node.choices()) {
                JsonObject c = new JsonObject();
                c.addProperty("text", choice.text());
                c.addProperty("next", choice.nextNodeId());
                arr.add(c);
            }
            obj.add("choices", arr);
        }

        if (node.action() != null) obj.addProperty("action", node.action());
        return obj;
    }
}
