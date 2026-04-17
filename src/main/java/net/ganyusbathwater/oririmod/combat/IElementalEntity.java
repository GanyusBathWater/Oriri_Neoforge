package net.ganyusbathwater.oririmod.combat;

/**
 * Interface for entities that have a specific element associated with them.
 * This allows dynamic elements for a single entity type (like projectiles).
 */
public interface IElementalEntity {
    Element getElement();
}
