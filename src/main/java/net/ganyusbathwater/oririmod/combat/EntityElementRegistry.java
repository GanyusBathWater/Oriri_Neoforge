package net.ganyusbathwater.oririmod.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.IdentityHashMap;
import java.util.Map;

public final class EntityElementRegistry {

    private static final Map<EntityType<?>, Element> ENTITY_ELEMENTS = new IdentityHashMap<>();

    private EntityElementRegistry() {}

    public static void setElement(EntityType<?> type, Element element) {
        ENTITY_ELEMENTS.put(type, element);
    }

    public static Element getElement(Entity entity) {
        return ENTITY_ELEMENTS.getOrDefault(entity.getType(), Element.PHYSICAL);
    }
}