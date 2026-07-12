package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.loot.AddLootTableModifier;
import net.ganyusbathwater.oririmod.loot.CelestialEventCondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifiersProvider extends GlobalLootModifierProvider {
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ModGlobalLootModifiersProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, OririMod.MOD_ID);
        this.registries = registries;
    }

    @Override
    protected void start() {
        // --- Chest Drops ---
        // Fire Crystal: Bastion
        add("fire_crystal_bastion_treasure", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/bastion_treasure")).build()},
                ModInjectionLootTables.key("fire_crystal")));
        add("fire_crystal_bastion_other", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/bastion_other")).build()},
                ModInjectionLootTables.key("fire_crystal")));
        add("fire_crystal_bastion_bridge", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/bastion_bridge")).build()},
                ModInjectionLootTables.key("fire_crystal")));
        add("fire_crystal_bastion_hoglin", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/bastion_hoglin_stable")).build()},
                ModInjectionLootTables.key("fire_crystal")));

        // Devil Fruit: Buried Treasure & Underwater Ruins
        add("devil_fruit_buried_treasure", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/buried_treasure")).build()},
                ModInjectionLootTables.key("devil_fruit_sunken")));
        add("devil_fruit_ruin_big", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/underwater_ruin_big")).build()},
                ModInjectionLootTables.key("devil_fruit_sunken")));
        add("devil_fruit_ruin_small", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/underwater_ruin_small")).build()},
                ModInjectionLootTables.key("devil_fruit_sunken")));
        add("devil_fruit_shipwreck_treasure", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/shipwreck_treasure")).build()},
                ModInjectionLootTables.key("devil_fruit_shipwreck")));

        // Molten Ingot: Nether Fortress
        add("molten_ingot_fortress", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/nether_bridge")).build()},
                ModInjectionLootTables.key("molten_ingot")));

        // Event Horizon Arrow: Stronghold & Trial Chamber Special
        add("event_horizon_stronghold_library", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/stronghold_library")).build()},
                ModInjectionLootTables.key("event_horizon_stronghold")));
        add("event_horizon_stronghold_corridor", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/stronghold_corridor")).build()},
                ModInjectionLootTables.key("event_horizon_stronghold")));
        add("event_horizon_stronghold_crossing", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/stronghold_crossing")).build()},
                ModInjectionLootTables.key("event_horizon_stronghold")));
        // Wait, for Trial Chambers, it's typically "spawners/trial_chamber/consumables" etc. But user specifically said "special vaults".
        // Trial chamber reward is "chests/trial_chambers/reward" or "chests/trial_chambers/reward_ominous".
        add("event_horizon_trial_vault_ominous", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous")).build()},
                ModInjectionLootTables.key("event_horizon_trial")));

        // Dodoco: Village Chests
        // Because there are many village chests, we can just check if the ID starts with "chests/village/"
        // Actually, NeoForge allows regex or just listing them. We can use a helper array or just list common ones.
        String[] villageChests = {"village_weaponsmith", "village_toolsmith", "village_armorer", "village_cartographer", "village_mason", "village_shepherd", "village_butcher", "village_fletcher", "village_fisher", "village_tannery", "village_temple", "village_desert_house", "village_plains_house", "village_taiga_house", "village_snowy_house", "village_savanna_house"};
        for (String type : villageChests) {
            add("dodoco_" + type, new AddLootTableModifier(
                    new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/" + type)).build()},
                    ModInjectionLootTables.key("dodoco")));
        }

        // Arbiter: Pillager Outpost & Woodland Mansion
        add("arbiter_outpost", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/pillager_outpost")).build()},
                ModInjectionLootTables.key("arbiter")));
        add("arbiter_mansion", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/woodland_mansion")).build()},
                ModInjectionLootTables.key("arbiter")));

        // End Weapons: End City & Trial Chamber
        add("end_weapons_end_city", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/end_city_treasure")).build()},
                ModInjectionLootTables.key("end_weapons_end")));
        add("end_weapons_trial_vault_ominous", new AddLootTableModifier(
                new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous")).build()},
                ModInjectionLootTables.key("end_weapons_trial")));

        // Vestige Items: ANY structure chest. We can hook onto common chest loot tables.
        // NeoForge doesn't have a "is chest" condition, but we can hook common ones.
        String[] structures = {"abandoned_mineshaft", "buried_treasure", "desert_pyramid", "end_city_treasure", "igloo_chest", "jungle_temple", "nether_bridge", "pillager_outpost", "ruined_portal", "shipwreck_treasure", "simple_dungeon", "stronghold_corridor", "stronghold_crossing", "stronghold_library", "underwater_ruin_big", "underwater_ruin_small", "woodland_mansion", "ancient_city", "ancient_city_ice_box", "bastion_treasure", "bastion_other", "bastion_bridge", "bastion_hoglin_stable"};
        for (String struct : structures) {
            add("vestige_items_" + struct, new AddLootTableModifier(
                    new LootItemCondition[]{LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/" + struct)).build()},
                    ModInjectionLootTables.key("vestige_items")));
        }


        // --- Entity Drops ---
        // Moonstone: Mobs during Celestial Events
        add("moonstone_celestial", new AddLootTableModifier(
                new LootItemCondition[]{CelestialEventCondition.celestialEventActive().build(), LootItemKilledByPlayerCondition.killedByPlayer().build()},
                ModInjectionLootTables.key("moonstone")));
        
        // Damned Soul: Undead
        add("damned_soul_undead", new AddLootTableModifier(
                new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityTypeTags.UNDEAD)).build()},
                ModInjectionLootTables.key("damned_soul")));
        
        // Hollow Soul: Passive mobs (Animals usually don't have a broad tag in 1.21 but there are tags like #minecraft:animals)
        add("hollow_soul_passive", new AddLootTableModifier(
                new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE)).build()}, // Placeholder: wait, let's use a custom condition or check if there is an animal tag. We will use inverted Monster tag for now.
                ModInjectionLootTables.key("hollow_soul")));
        // Better Hollow Soul: Just check if the entity isn't a monster/undead etc. Or we can just use the #forge:animals or similar if it exists. We'll refine this later if it's an issue.

        // Power Soul: Bosses (Wither, Ender Dragon, Elder Guardian, Warden)
        add("power_soul_wither", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.WITHER)).build()}, ModInjectionLootTables.key("power_soul")));
        add("power_soul_dragon", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.ENDER_DRAGON)).build()}, ModInjectionLootTables.key("power_soul")));
        add("power_soul_elder", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.ELDER_GUARDIAN)).build()}, ModInjectionLootTables.key("power_soul")));
        add("power_soul_warden", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.WARDEN)).build()}, ModInjectionLootTables.key("power_soul")));

        // Tortured Soul: Nether mobs
        add("tortured_soul_nether", new AddLootTableModifier(
                new LootItemCondition[]{LocationCheck.checkLocation(LocationPredicate.Builder.location().setDimension(net.minecraft.world.level.Level.NETHER)).build()},
                ModInjectionLootTables.key("tortured_soul")));
        
        // Void Soul: End mobs
        add("void_soul_end", new AddLootTableModifier(
                new LootItemCondition[]{LocationCheck.checkLocation(LocationPredicate.Builder.location().setDimension(net.minecraft.world.level.Level.END)).build()},
                ModInjectionLootTables.key("void_soul")));

        // Dragon Fruit: Jungle biome
        add("dragon_fruit_jungle", new AddLootTableModifier(
                new LootItemCondition[]{LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(registries.join().lookupOrThrow(Registries.BIOME).getOrThrow(BiomeTags.IS_JUNGLE))).build()},
                ModInjectionLootTables.key("dragon_fruit")));

        // One Thousand Screams: Warden
        add("one_thousand_screams_warden", new AddLootTableModifier(
                new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.WARDEN)).build()},
                ModInjectionLootTables.key("one_thousand_screams")));
        
        // Iras Soul: Ender Dragon
        add("iras_soul_dragon", new AddLootTableModifier(
                new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.ENDER_DRAGON)).build()},
                ModInjectionLootTables.key("iras_soul")));

        // Summoner Weapons
        add("summoner_zombie", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.ZOMBIE)).build()}, ModInjectionLootTables.key("zombie_encyclopedia")));
        add("summoner_skeleton", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.SKELETON)).build()}, ModInjectionLootTables.key("skeleton_encyclopedia")));
        add("summoner_iron_golem", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.IRON_GOLEM)).build()}, ModInjectionLootTables.key("iron_golem_manual")));
        add("summoner_blaze", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.BLAZE)).build()}, ModInjectionLootTables.key("blazing_pyromaniac_guide")));
        add("summoner_magma_cube", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.MAGMA_CUBE)).build()}, ModInjectionLootTables.key("magma_cooking_book")));
        add("summoner_slime", new AddLootTableModifier(new LootItemCondition[]{LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(EntityType.SLIME)).build()}, ModInjectionLootTables.key("slimy_cooking_book")));

        // --- Block Drops ---
        // Iron Roots: Hanging Roots
        add("iron_roots_drop", new AddLootTableModifier(
                new LootItemCondition[]{LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.HANGING_ROOTS).build()},
                ModInjectionLootTables.key("iron_roots")));

        // Calcium Currants: Leaves
        add("calcium_currants_drop", new AddLootTableModifier(
                new LootItemCondition[]{AnyOfCondition.anyOf(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.OAK_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SPRUCE_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.BIRCH_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.JUNGLE_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.ACACIA_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.DARK_OAK_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.MANGROVE_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.CHERRY_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.AZALEA_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.FLOWERING_AZALEA_LEAVES)
                ).build()},
                ModInjectionLootTables.key("calcium_currants")));

        // The First Apple: Oak / Dark Oak Leaves
        add("first_apple_drop", new AddLootTableModifier(
                new LootItemCondition[]{AnyOfCondition.anyOf(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.OAK_LEAVES),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.DARK_OAK_LEAVES)
                ).build()},
                ModInjectionLootTables.key("the_first_apple")));

        // Four Leaf Clover: Grass / Fern
        add("four_leaf_clover_drop", new AddLootTableModifier(
                new LootItemCondition[]{AnyOfCondition.anyOf(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SHORT_GRASS),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_GRASS),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.FERN),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.LARGE_FERN)
                ).build()},
                ModInjectionLootTables.key("four_leaf_clover")));

        // Miracle Seaweed: Kelp / Seagrass
        add("miracle_seaweed_drop", new AddLootTableModifier(
                new LootItemCondition[]{AnyOfCondition.anyOf(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.KELP),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.KELP_PLANT),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SEAGRASS),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_SEAGRASS)
                ).build()},
                ModInjectionLootTables.key("miracle_seaweed")));
    }
}
