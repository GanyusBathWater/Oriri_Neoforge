package net.ganyusbathwater.oririmod.combat;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

public final class ElementInit {

    private ElementInit() {}

    public static void init() {
        // \= \= \= Entities (Mobs, Projektile) \= \= \=
        EntityElementRegistry.setElement(EntityType.BLAZE, Element.FIRE);

        // Projektile
        EntityElementRegistry.setElement(EntityType.SMALL_FIREBALL, Element.FIRE);
        // Beispiel: Pfeile
        // EntityElementRegistry.setElement(EntityType.ARROW, Element.LIGHT);

        // \= \= \= Items (Vanilla + Mod\-Items) \= \= \=
        // Vanilla\-Beispiele
        ItemElementRegistry.setElement(Items.DIAMOND_SWORD, Element.FIRE);
        ItemElementRegistry.setElement(Items.IRON_SWORD, Element.EARTH);
        ItemElementRegistry.setElement(Items.BOW, Element.LIGHT);
        ItemElementRegistry.setElement(Items.TRIDENT, Element.WATER);
        ItemElementRegistry.setElement(ModItems.STELLA_PERDITOR.get(), Element.DARKNESS);

        // Hier kannst du auch deine eigenen Mod\-Items registrieren, z\.B.:
        // ItemElementRegistry.setElement(ModItems.PANDORAS_BLADE.get(), Element.FIRE);
    }
}