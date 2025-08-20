package net.ganyusbathwater.oririmod.item;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.*;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OririMod.MOD_ID);

    //-------------------------------------Crafting Items---------------------------------------------------------

    public static final DeferredItem<Item> FIRE_CRYSTAL = ITEMS.register("fire_crystal", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MANA_MANIFESTATION = ITEMS.register("mana_manifestation", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MOON_STONE = ITEMS.register("moon_stone", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DAMNED_SOUL = ITEMS.register("damned_soul", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> HOLLOW_SOUL = ITEMS.register("hollow_soul", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> IRAS_SOUL = ITEMS.register("iras_soul", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> IRON_STICK = ITEMS.register("iron_stick", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> POWER_SOUL = ITEMS.register("power_soul", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TORTURED_SOUL = ITEMS.register("tortured_soul", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> VOID_SOUL = ITEMS.register("void_soul", () -> new Item(new Item.Properties()));

    //---------------------------------------Artifacts---------------------------------------------------------

    public static final DeferredItem<Item> BOUND_OF_THE_CELESTIAL_SISTERS = ITEMS.register("bound_of_the_celestial_sisters", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CRIT_GLOVE = ITEMS.register("crit_glove", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DUELLANT_CORTEX = ITEMS.register("duellant_cortex", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> HEART_OF_THE_TANK = ITEMS.register("heart_of_the_tank", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> HERMES_GIFT = ITEMS.register("hermes_gift", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ICE_SKATES = ITEMS.register("ice_skates", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MANA_HEART = ITEMS.register("mana_heart", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PESTDOCTORS_MASK = ITEMS.register("pestdoctors_mask", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MIRROR_OF_THE_BLACK_SUN = ITEMS.register("mirror_of_the_black_sun", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PHOENIX_FEATHER = ITEMS.register("phoenix_feather", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PLUSHIE_OF_THE_MINE = ITEMS.register("plushie_of_the_mine", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PYRO_VISION = ITEMS.register("pyro_vision", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RELIC_OF_THE_PAST = ITEMS.register("relic_of_the_past", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SOLIS_BROOCH = ITEMS.register("solis_brooch", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> STIGMA_OF_THE_ARCHITECT = ITEMS.register("stigma_of_the_architect", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SOUL_BIND = ITEMS.register("soul_bind", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SPRING = ITEMS.register("spring", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> STRIDERS_SCALE = ITEMS.register("striders_scale", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> STRANGE_ENDER_EYE = ITEMS.register("strange_ender_eye", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SWEETS_BAG = ITEMS.register("sweets_bag", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WITHER_ROSE = ITEMS.register("wither_rose", () -> new Item(new Item.Properties()));

    //---------------------------------------Plants/Foods---------------------------------------------------------

    public static final DeferredItem<Item> ELDERBERRY = ITEMS.register("elderberry", () -> new Item(new Item.Properties().food(ModFoods.ELDERBERRY)));

    //---------------------------------------Weapons---------------------------------------------------------

    public static final DeferredItem<SwordItem> PANDORAS_BLADE = ITEMS.register("pandoras_blade", () -> new CustomSwordItem(Tiers.DIAMOND, new Item.Properties(), ModRarity.MYTHIC));

    public static final DeferredItem<SwordItem> PIRATE_SABER = ITEMS.register("pirate_saber", () -> new CustomSwordItem(Tiers.IRON, new Item.Properties(), ModRarity.UNCOMMON));

    public static final DeferredItem<PickaxeItem> NEBULA_PICKAXE = ITEMS.register("nebula_pickaxe", () -> new CustomPickaxeItem(Tiers.NETHERITE, new Item.Properties(), ModRarity.LEGENDARY));

    public static final DeferredItem<SwordItem> ICE_SWORD = ITEMS.register("ice_sword", () -> new CustomSwordItem(Tiers.DIAMOND, new Item.Properties(), ModRarity.RARE));

    public static final DeferredItem<PickaxeItem> MOLTEN_PICKAXE = ITEMS.register("molten_pickaxe", () -> new CustomPickaxeItem(Tiers.DIAMOND, new Item.Properties(), ModRarity.RARE));

    public static final DeferredItem<MaceItem> MJOELNIR = ITEMS.register("mjoelnir", () -> new CustomHammerItem(new Item.Properties(), ModRarity.MYTHIC));

    public static final DeferredItem<MaceItem> LAW_BREAKER = ITEMS.register("law_breaker", () -> new CustomHammerItem(new Item.Properties(), ModRarity.GODLY));

    public static final DeferredItem<SwordItem> STELLA_PERDITOR = ITEMS.register("stella_perditor", () -> new CustomSwordItem(Tiers.NETHERITE, new Item.Properties(), ModRarity.UNIQUE));

    public static final DeferredItem<SwordItem> QILINS_WRATH = ITEMS.register("qilins_wrath", () -> new CustomSwordItem(Tiers.NETHERITE, new Item.Properties(),ModRarity.GODLY));

    public static final DeferredItem<SwordItem> SOLS_EMBRACE = ITEMS.register("sols_embrace", () -> new CustomSwordItem(Tiers.DIAMOND, new Item.Properties(), ModRarity.LEGENDARY));

    public static final DeferredItem<CrossbowItem> ARBITER_CROSSBOW = ITEMS.register("arbiter_crossbow", () -> new CustomCrossbowItem(new Item.Properties(), ModRarity.LEGENDARY));

    public static final DeferredItem<BowItem> ORAPHIM_BOW = ITEMS.register("oraphim_bow", () -> new CustomBowItemClass(new Item.Properties(), ModRarity.LEGENDARY));

    public static final DeferredItem<SwordItem> EMERALD_SWORD = ITEMS.register("emerald_sword", () -> new SwordItem(Tiers.DIAMOND, new Item.Properties().fireResistant()));

    public static final DeferredItem<AxeItem> EMERALD_AXE = ITEMS.register("emerald_axe", () -> new AxeItem(Tiers.DIAMOND, new Item.Properties().fireResistant()));

    public static final DeferredItem<PickaxeItem> EMERALD_PICKAXE = ITEMS.register("emerald_pickaxe", () -> new PickaxeItem(Tiers.DIAMOND, new Item.Properties().fireResistant()));

    public static final DeferredItem<ShovelItem> EMERALD_SHOVEL = ITEMS.register("emerald_shovel", () -> new ShovelItem(Tiers.DIAMOND, new Item.Properties().fireResistant()));

    public static final DeferredItem<HoeItem> EMERALD_HOE = ITEMS.register("emerald_hoe", () -> new HoeItem(Tiers.DIAMOND, new Item.Properties().fireResistant()));

    //------------------------------------------------------Armor---------------------------------------------------------

    public static final DeferredItem<ArmorItem> CRYSTAL_HELMET = ITEMS.register("crystal_helmet", () -> new ModArmorItem(ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(19)), ModRarity.LEGENDARY));

    public static final DeferredItem<ArmorItem> CRYSTAL_CHESTPLATE = ITEMS.register("crystal_chestplate", () -> new ModArmorItem(ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(19)),ModRarity.LEGENDARY));

    public static final DeferredItem<ArmorItem> CRYSTAL_LEGGINGS = ITEMS.register("crystal_leggings", () -> new ModArmorItem(ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(19)),ModRarity.LEGENDARY));

    public static final DeferredItem<ArmorItem> CRYSTAL_BOOTS = ITEMS.register("crystal_boots", () -> new ModArmorItem(ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(19)),ModRarity.LEGENDARY));

    public static final DeferredItem<ArmorItem> ANCIENT_HELMET = ITEMS.register("ancient_helmet", () -> new ModArmorItem(ModArmorMaterials.ANCIENT_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> ANCIENT_CHESTPLATE = ITEMS.register("ancient_chestplate", () -> new ModArmorItem(ModArmorMaterials.ANCIENT_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> ANCIENT_LEGGINGS = ITEMS.register("ancient_leggings", () -> new ModArmorItem(ModArmorMaterials.ANCIENT_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> ANCIENT_BOOTS = ITEMS.register("ancient_boots", () -> new ModArmorItem(ModArmorMaterials.ANCIENT_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> GILDED_NETHERRITE_HELMET = ITEMS.register("gilded_netherrite_helmet", () -> new ModArmorItem(ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> GILDED_NETHERRITE_CHESTPLATE = ITEMS.register("gilded_netherrite_chestplate", () -> new ModArmorItem(ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> GILDED_NETHERRITE_LEGGINGS = ITEMS.register("gilded_netherrite_leggings", () -> new ModArmorItem(ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> GILDED_NETHERRITE_BOOTS = ITEMS.register("gilded_netherrite_boots", () -> new ModArmorItem(ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(19)),ModRarity.MYTHIC));

    public static final DeferredItem<ArmorItem> BLUE_ICE_HELMET = ITEMS.register("blue_ice_helmet", () -> new ModArmorItem(ModArmorMaterials.BLUE_ICE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> BLUE_ICE_CHESTPLATE = ITEMS.register("blue_ice_chestplate", () -> new ModArmorItem(ModArmorMaterials.BLUE_ICE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> BLUE_ICE_LEGGINGS = ITEMS.register("blue_ice_leggings", () -> new ModArmorItem(ModArmorMaterials.BLUE_ICE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> BLUE_ICE_BOOTS = ITEMS.register("blue_ice_boots", () -> new ModArmorItem(ModArmorMaterials.BLUE_ICE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> MOLTEN_HELMET = ITEMS.register("molten_helmet", () -> new ModArmorItem(ModArmorMaterials.MOLTEN_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> MOLTEN_CHESTPLATE = ITEMS.register("molten_chestplate", () -> new ModArmorItem(ModArmorMaterials.MOLTEN_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> MOLTEN_LEGGINGS = ITEMS.register("molten_leggings", () -> new ModArmorItem(ModArmorMaterials.MOLTEN_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> MOLTEN_BOOTS = ITEMS.register("molten_boots", () -> new ModArmorItem(ModArmorMaterials.MOLTEN_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(19)),ModRarity.RARE));

    public static final DeferredItem<ArmorItem> PRISMARINE_HELMET = ITEMS.register("prismarine_helmet", () -> new ModArmorItem(ModArmorMaterials.PRISMARINE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(19)),ModRarity.UNCOMMON));

    public static final DeferredItem<ArmorItem> PRISMARINE_CHESTPLATE = ITEMS.register("prismarine_chestplate", () -> new ModArmorItem(ModArmorMaterials.PRISMARINE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(19)),ModRarity.UNCOMMON));

    public static final DeferredItem<ArmorItem> PRISMARINE_LEGGINGS = ITEMS.register("prismarine_leggings", () -> new ModArmorItem(ModArmorMaterials.PRISMARINE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(19)),ModRarity.UNCOMMON));

    public static final DeferredItem<ArmorItem> PRISMARINE_BOOTS = ITEMS.register("prismarine_boots", () -> new ModArmorItem(ModArmorMaterials.PRISMARINE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(19)),ModRarity.UNCOMMON));

    //------------------------------------------------------Arcana-----------------------------------------------------------

    public static final DeferredItem<Item> THE_FOOL = ITEMS.register("the_fool", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_MAGICIAN = ITEMS.register("the_magician", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_HIGH_PRIESTESS = ITEMS.register("the_high_priestess", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_EMPRESS = ITEMS.register("the_empress", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_EMPEROR = ITEMS.register("the_emperor", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_HIEROPHANT = ITEMS.register("the_hierophant", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_LOVERS = ITEMS.register("the_lovers", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_CHARIOT = ITEMS.register("the_chariot", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> STRENGTH = ITEMS.register("strength", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_HERMIT = ITEMS.register("the_hermit", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> WHEEL_OF_FORTUNE = ITEMS.register("wheel_of_fortune", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> JUSTICE = ITEMS.register("justice", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_HANGED_MAN = ITEMS.register("the_hanged_man", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> DEATH = ITEMS.register("death", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> TEMPERANCE = ITEMS.register("temperance", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_DEVIL = ITEMS.register("the_devil", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_TOWER = ITEMS.register("the_tower", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_STAR = ITEMS.register("the_star", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_MOON = ITEMS.register("the_moon", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_SUN = ITEMS.register("the_sun", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> JUDGEMENT = ITEMS.register("judgement", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    public static final DeferredItem<Item> THE_WORLD = ITEMS.register("the_world", () -> new ModRarityItem(new Item.Properties(), ModRarity.MAGICAL));

    private static void addItemsToIngredientTabItemGroup() {

    }

    public static void registerModItems(IEventBus eventBus) {
        ITEMS.register(eventBus);
        OririMod.LOGGER.info("Registering Mod Items for " + OririMod.MOD_ID);
    }
}
