package net.ganyusbathwater.oririmod.combat;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

public final class ElementInit {

    private ElementInit() {
    }

    public static void init() {
        // Mobs
        EntityElementRegistry.setElement(EntityType.BLAZE, Element.FIRE);
        EntityElementRegistry.setElement(EntityType.ZOMBIE, Element.NATURE);

        // Projectiles
        EntityElementRegistry.setElement(EntityType.SMALL_FIREBALL, Element.FIRE);

        // Items/Weapons
        ItemElementRegistry.setElement(Items.DIAMOND_SWORD, Element.PHYSICAL);
        ItemElementRegistry.setElement(Items.IRON_SWORD, Element.EARTH);
        ItemElementRegistry.setElement(Items.TRIDENT, Element.WATER);
        ItemElementRegistry.setElement(ModItems.STELLA_PERDITOR.get(), Element.DARKNESS);
        ItemElementRegistry.setElement(ModItems.QILINS_WRATH.get(), Element.TRUE_DAMAGE);
    }
}