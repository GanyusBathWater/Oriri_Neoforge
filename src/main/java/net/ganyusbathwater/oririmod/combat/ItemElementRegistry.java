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
        
        // 1. Check if the item has an innate element
        Element innate = ITEM_ELEMENTS.get(stack.getItem());
        if (innate != null) {
            return innate;
        }

        // 2. Check for element enchantments (both regular and stored in books)
        net.minecraft.world.item.enchantment.ItemEnchantments enchantments = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        net.minecraft.world.item.enchantment.ItemEnchantments storedEnchantments = stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        
        if (!enchantments.isEmpty() || !storedEnchantments.isEmpty()) {
            // Helper method to check a set of enchantments
            Element el = checkEnchantmentsForElement(enchantments);
            if (el != null) return el;
            
            el = checkEnchantmentsForElement(storedEnchantments);
            if (el != null) return el;
        }

        return Element.PHYSICAL;
    }

    private static Element checkEnchantmentsForElement(net.minecraft.world.item.enchantment.ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) return null;
        for (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchant : enchantments.keySet()) {
            if (enchant.unwrapKey().isPresent()) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key = enchant.unwrapKey().get();
                if (key.equals(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_FIRE)) return Element.FIRE;
                if (key.equals(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_WATER)) return Element.WATER;
                if (key.equals(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_NATURE)) return Element.NATURE;
                if (key.equals(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_EARTH)) return Element.EARTH;
                if (key.equals(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_LIGHT)) return Element.LIGHT;
                if (key.equals(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_DARKNESS)) return Element.DARKNESS;
            }
        }
        return null;
    }
}
