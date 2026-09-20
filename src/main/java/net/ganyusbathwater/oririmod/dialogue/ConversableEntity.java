package net.ganyusbathwater.oririmod.dialogue;

/**
 * Interface for entities that support the NPC conversation system.
 * Implement this on any entity that the player should be able to talk to.
 */
public interface ConversableEntity {

    /**
     * @return The dialogue tree ID to load for this entity (e.g. "dungeon_keeper").
     *         Must match a JSON file in data/oririmod/dialogue/.
     */
    String getDialogueTreeId();
}
