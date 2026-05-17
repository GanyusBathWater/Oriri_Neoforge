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
        ItemElementRegistry.setElement(ModItems.ANCIENT_SCYTHE.get(), Element.NATURE);
        ItemElementRegistry.setElement(ModItems.BLACK_ICE_SCYTHE.get(), Element.WATER);
        ItemElementRegistry.setElement(ModItems.CRYSTAL_SCYTHE.get(), Element.LIGHT);
        ItemElementRegistry.setElement(ModItems.GILDED_NETHERITE_SCYTHE.get(), Element.EARTH);
        ItemElementRegistry.setElement(ModItems.PRISMARINE_SCYTHE.get(), Element.WATER);
        ItemElementRegistry.setElement(ModItems.MOLTEN_SCYTHE.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.THE_HARBINGER.get(), Element.TRUE_DAMAGE);
        ItemElementRegistry.setElement(ModItems.PANDORAS_BLADE.get(), Element.NATURE);
        ItemElementRegistry.setElement(ModItems.PIRATE_SABER.get(), Element.WATER);
        ItemElementRegistry.setElement(ModItems.ICE_SWORD.get(), Element.WATER);
        ItemElementRegistry.setElement(ModItems.MJOELNIR.get(), Element.LIGHT);
        ItemElementRegistry.setElement(ModItems.LAW_BREAKER.get(), Element.LIGHT);
        ItemElementRegistry.setElement(ModItems.SOLS_EMBRACE.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.ONE_THOUSAND_SCREAMS.get(), Element.DARKNESS);
        ItemElementRegistry.setElement(ModItems.STAFF_OF_HELL.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.STAFF_OF_COSMOS.get(), Element.EARTH);
        ItemElementRegistry.setElement(ModItems.STAFF_OF_ETERNAL_ICE.get(), Element.WATER);
        ItemElementRegistry.setElement(ModItems.STAFF_OF_HELL.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.BOOK_OF_AMATEUR.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.BOOK_OF_APPRENTICE.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.BOOK_OF_JOURNEYMAN.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.BOOK_OF_WISE.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.DODOCO.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.ZOMBIE_ENCYCLOPEDIA.get(), Element.NATURE);
        ItemElementRegistry.setElement(ModItems.SKELETON_ENCYCLOPEDIA.get(), Element.NATURE);
        ItemElementRegistry.setElement(ModItems.IRON_GOLEM_MANUAL.get(), Element.EARTH);
        ItemElementRegistry.setElement(ModItems.BLAZING_PYROMANIAC_GUIDE.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.MAGMA_COOKING_BOOK.get(), Element.FIRE);
        ItemElementRegistry.setElement(ModItems.SLIMY_COOKING_BOOK.get(), Element.WATER);
        ItemElementRegistry.setElement(ModItems.IVY_BOTANIC_GUIDE.get(), Element.NATURE);
        ItemElementRegistry.setElement(ModItems.EVENT_HORIZON_ARROW.get(), Element.DARKNESS);
        ItemElementRegistry.setElement(ModItems.TNT_ARROW.get(), Element.PHYSICAL);
        ItemElementRegistry.setElement(ModItems.DRAGON_IRON_ARROW.get(), Element.TRUE_DAMAGE);
        ItemElementRegistry.setElement(ModItems.FROST_ARROW.get(), Element.WATER);
        ItemElementRegistry.setElement(ModItems.COPPER_ARROW.get(), Element.EARTH);
        ItemElementRegistry.setElement(ModItems.SONIC_ARROW.get(), Element.DARKNESS);
    }
}