package net.ganyusbathwater.oririmod.item;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.*;
import net.ganyusbathwater.oririmod.item.custom.magic.MagicBoltItem;
import net.ganyusbathwater.oririmod.item.custom.magic.MagicStaffItem;
import net.ganyusbathwater.oririmod.item.custom.magic.OmniMagicItem;
import net.ganyusbathwater.oririmod.item.custom.vestiges.*;
import net.ganyusbathwater.oririmod.util.MagicBoltAbility;
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

    //---------------------------------------Vestiges---------------------------------------------------------

    public static final DeferredItem<VestigeItem> BOUND_OF_THE_CELESTIAL_SISTERS = ITEMS.register("bound_of_the_celestial_sisters", () -> new BoundOfTheCelestialSisters(new Item.Properties()));
    public static final DeferredItem<VestigeItem> CRIT_GLOVE = ITEMS.register("crit_glove", () -> new CritGlove(new Item.Properties()));
    public static final DeferredItem<VestigeItem> DUELLANT_CORTEX = ITEMS.register("duellant_cortex", () -> new DuellantCortex(new Item.Properties()));
    public static final DeferredItem<VestigeItem> HEART_OF_THE_TANK = ITEMS.register("heart_of_the_tank", () -> new HeartOfTheTank(new Item.Properties()));
    public static final DeferredItem<VestigeItem> SNOW_BOOTS = ITEMS.register("snow_boots", () -> new SnowBoots(new Item.Properties()));
    public static final DeferredItem<VestigeItem> MIRROR_OF_THE_BLACK_SUN = ITEMS.register("mirror_of_the_black_sun", () -> new MirrorOfTheBlackSun(new Item.Properties()));
    public static final DeferredItem<VestigeItem> PHOENIX_FEATHER = ITEMS.register("phoenix_feather", () -> new PhoenixFeather(new Item.Properties()));
    public static final DeferredItem<VestigeItem> MINERS_LANTERN = ITEMS.register("miners_lantern", () -> new MinersLantern(new Item.Properties()));
    public static final DeferredItem<VestigeItem> RELIC_OF_THE_PAST = ITEMS.register("relic_of_the_past", () -> new RelicOfThePast(new Item.Properties()));
    public static final DeferredItem<VestigeItem> SOLIS_BROOCH = ITEMS.register("solis_brooch", () -> new VestigeItem(new Item.Properties(), 3));
    public static final DeferredItem<VestigeItem> STIGMA_OF_THE_ARCHITECT = ITEMS.register("stigma_of_the_architect", () -> new VestigeItem(new Item.Properties(), 3));
    public static final DeferredItem<VestigeItem> SPRING = ITEMS.register("spring", () -> new Spring(new Item.Properties()));
    public static final DeferredItem<VestigeItem> STRIDERS_SCALE = ITEMS.register("striders_scale", () -> new VestigeItem(new Item.Properties(), 3));
    public static final DeferredItem<VestigeItem> STRANGE_ENDER_EYE = ITEMS.register("strange_ender_eye", () -> new StrangeEnderEye(new Item.Properties()));
    public static final DeferredItem<VestigeItem> CANDY_BAG = ITEMS.register("candy_bag", () -> new CandyBag(new Item.Properties()));
    public static final DeferredItem<VestigeItem> WITHER_ROSE = ITEMS.register("wither_rose", () -> new Witherrose(new Item.Properties()));

    //---------------------------------------Foods-----------------------------------------------------------

    public static final DeferredItem<Item> ELDERBERRY = ITEMS.register("elderberry", () -> new Item(new Item.Properties().food(ModFoods.ELDERBERRY)));

    //---------------------------------------Weapons---------------------------------------------------------

    public static final DeferredItem<SwordItem> PANDORAS_BLADE = ITEMS.register("pandoras_blade", () -> new CustomSwordItem(Tiers.DIAMOND, new Item.Properties().fireResistant().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F)), ModRarity.MYTHIC));
    public static final DeferredItem<SwordItem> PIRATE_SABER = ITEMS.register("pirate_saber", () -> new CustomSwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -1F)), ModRarity.UNCOMMON));
    public static final DeferredItem<SwordItem> ICE_SWORD = ITEMS.register("ice_sword", () -> new CustomSwordItem(Tiers.DIAMOND, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F)), ModRarity.RARE));
    public static final DeferredItem<MaceItem> LAW_BREAKER = ITEMS.register("law_breaker", () -> new CustomHammerItem(new Item.Properties().fireResistant().attributes(MaceItem.createAttributes()), ModRarity.GODLY));
    public static final DeferredItem<SwordItem> STELLA_PERDITOR = ITEMS.register("stella_perditor", () -> new CustomSwordItem(Tiers.NETHERITE, new Item.Properties().fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6,-1.4F)), ModRarity.UNIQUE));
    public static final DeferredItem<SwordItem> QILINS_WRATH = ITEMS.register("qilins_wrath", () -> new CustomSwordItem(Tiers.NETHERITE, new Item.Properties().fireResistant().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -1.4F)),ModRarity.GODLY));
    public static final DeferredItem<SwordItem> SOLS_EMBRACE = ITEMS.register("sols_embrace", () -> new CustomSwordItem(Tiers.DIAMOND, new Item.Properties().fireResistant().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F)), ModRarity.LEGENDARY));

    public static final DeferredItem<PickaxeItem> NEBULA_PICKAXE = ITEMS.register("nebula_pickaxe", () -> new CustomPickaxeItem(Tiers.NETHERITE, new Item.Properties().attributes(PickaxeItem.createAttributes(Tiers.NETHERITE, 1, -2.8F)), ModRarity.LEGENDARY));
    public static final DeferredItem<PickaxeItem> MOLTEN_PICKAXE = ITEMS.register("molten_pickaxe", () -> new CustomPickaxeItem(Tiers.DIAMOND, new Item.Properties().attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1, -2.8F)), ModRarity.RARE));
    public static final DeferredItem<MaceItem> MJOELNIR = ITEMS.register("mjoelnir", () -> new CustomHammerItem(new Item.Properties().attributes(MaceItem.createAttributes()), ModRarity.MYTHIC));

    public static final DeferredItem<CrossbowItem> ARBITER_CROSSBOW = ITEMS.register("arbiter_crossbow", () -> new CustomCrossbowItem(new Item.Properties(), ModRarity.LEGENDARY));
    public static final DeferredItem<BowItem> ORAPHIM_BOW = ITEMS.register("oraphim_bow", () -> new CustomBowItemClass(new Item.Properties(), ModRarity.LEGENDARY));

    public static final DeferredItem<SwordItem> EMERALD_SWORD = ITEMS.register("emerald_sword", () -> new SwordItem(Tiers.DIAMOND, new Item.Properties().fireResistant().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F))));
    public static final DeferredItem<AxeItem> EMERALD_AXE = ITEMS.register("emerald_axe", () -> new AxeItem(Tiers.DIAMOND, new Item.Properties().fireResistant().attributes(AxeItem.createAttributes(Tiers.DIAMOND, 5.0F, -3.0F))));
    public static final DeferredItem<PickaxeItem> EMERALD_PICKAXE = ITEMS.register("emerald_pickaxe", () -> new PickaxeItem(Tiers.DIAMOND, new Item.Properties().fireResistant().attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1.0F, -2.8F))));
    public static final DeferredItem<ShovelItem> EMERALD_SHOVEL = ITEMS.register("emerald_shovel", () -> new ShovelItem(Tiers.DIAMOND, new Item.Properties().fireResistant().attributes(ShovelItem.createAttributes(Tiers.DIAMOND, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> EMERALD_HOE = ITEMS.register("emerald_hoe", () -> new HoeItem(Tiers.DIAMOND, new Item.Properties().fireResistant().attributes(HoeItem.createAttributes(Tiers.DIAMOND, -3.0F, 0.0F))));

    public static final DeferredItem<MagicStaffItem> STAFF_OF_WISE = ITEMS.register("staff_of_wise", () -> new MagicStaffItem(new Item.Properties(), MagicStaffItem.StaffAction.REGEN, 400, 0,500, 20, ModRarity.MYTHIC));
    public static final DeferredItem<MagicStaffItem> STAFF_OF_EARTH = ITEMS.register("staff_of_earth", () -> new MagicStaffItem(new Item.Properties(), MagicStaffItem.StaffAction.HASTE, 400, 1,600, 12, ModRarity.RARE));
    public static final DeferredItem<MagicStaffItem> STAFF_OF_FOREST = ITEMS.register("staff_of_forest", () -> new MagicStaffItem(new Item.Properties(), MagicStaffItem.StaffAction.GROW, 0, 0, 20, 6, ModRarity.UNCOMMON));

    public static final DeferredItem<MagicBoltItem> ONE_THOUSAND_SCREAMS = ITEMS.register("one_thousand_screams", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.SONIC, 2, 12, ModRarity.MYTHIC));
    public static final DeferredItem<MagicBoltItem> STAFF_OF_HELL = ITEMS.register("staff_of_hell", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.BLAZE, 2, 8, ModRarity.RARE));
    public static final DeferredItem<MagicBoltItem> STAFF_OF_COSMOS = ITEMS.register("staff_of_cosmos", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.METEOR, 2, 40, ModRarity.LEGENDARY));
    public static final DeferredItem<MagicBoltItem> STAFF_OF_VOID = ITEMS.register("staff_of_void", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.ENDER, 2, 18, ModRarity.MYTHIC));
    public static final DeferredItem<MagicBoltItem> DODOCO = ITEMS.register("dodoco", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.EXPLOSIVE, 2, 20, ModRarity.RARE));
    public static final DeferredItem<MagicBoltItem> BOOK_OF_AMATEUR = ITEMS.register("book_of_amateur", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.NORMAL, 2, 4, ModRarity.COMMON));
    public static final DeferredItem<MagicBoltItem> BOOK_OF_APPRENTICE = ITEMS.register("book_of_apprentice", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.NORMAL, 2, 6, ModRarity.UNCOMMON));
    public static final DeferredItem<MagicBoltItem> BOOK_OF_JOURNEYMAN = ITEMS.register("book_of_journeyman", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.NORMAL, 2, 10, ModRarity.RARE));
    public static final DeferredItem<MagicBoltItem> BOOK_OF_WISE = ITEMS.register("book_of_wise", () -> new MagicBoltItem(new Item.Properties(), MagicBoltAbility.NORMAL, 2, 16, ModRarity.MYTHIC));
    public static final DeferredItem<OmniMagicItem> STAFF_OF_ALMIGHTY = ITEMS.register("staff_of_almighty", () -> new OmniMagicItem(new Item.Properties(), ModRarity.GODLY, 2, 500, 1));

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
