package net.ganyusbathwater.oririmod.combat;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ItemElementRegistry {

    private static final Map<Item, Element> ITEM_ELEMENTS = new IdentityHashMap<>();

    private ItemElementRegistry() {}

    public static void setElement(Item item, Element element) {
        ITEM_ELEMENTS.put(item, element);
    }

    public static Element getElement(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Element.PHYSICAL;
        }
        return ITEM_ELEMENTS.getOrDefault(stack.getItem(), Element.PHYSICAL);
    }
}
