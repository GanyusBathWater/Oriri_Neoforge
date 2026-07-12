package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

import java.util.function.BiConsumer;

public class ModInjectionLootTables {
    public static ResourceKey<LootTable> key(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "inject/" + name));
    }

    private static LootTable.Builder createSimpleTable(HolderLookup.Provider registries, Item item, float chance, boolean useLuck) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F));
        var entry = LootItem.lootTableItem(item);
        if (chance < 1.0f) {
            if (useLuck) {
                entry.when(LootItemRandomChanceCondition.randomChance(chance));
            } else {
                if (registries != null) {
                    entry.when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(registries, chance, chance * 0.5f));
                } else {
                    entry.when(LootItemRandomChanceCondition.randomChance(chance));
                }
            }
        }
        pool.add(entry);
        return LootTable.lootTable().withPool(pool);
    }

    private static LootTable.Builder createMultiTable(float chance, boolean useLuck, Item... items) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F));
        for (Item item : items) {
            var entry = LootItem.lootTableItem(item);
            if (chance < 1.0f) {
                entry.when(LootItemRandomChanceCondition.randomChance(chance));
            }
            pool.add(entry);
        }
        return LootTable.lootTable().withPool(pool);
    }

    public static class Chests implements LootTableSubProvider {
        public Chests(HolderLookup.Provider registries) {}
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(key("fire_crystal"), createSimpleTable(null, ModItems.FIRE_CRYSTAL.get(), 0.005f, true));
            output.accept(key("devil_fruit_sunken"), createSimpleTable(null, ModItems.DEVIL_FRUIT.get(), 0.05f, true));
            output.accept(key("devil_fruit_shipwreck"), createSimpleTable(null, ModItems.DEVIL_FRUIT.get(), 0.005f, true));
            output.accept(key("molten_ingot"), createSimpleTable(null, ModItems.MOLTEN_INGOT.get(), 0.05f, true));
            output.accept(key("event_horizon_stronghold"), createSimpleTable(null, ModItems.EVENT_HORIZON_ARROW.get(), 0.01f, true));
            output.accept(key("event_horizon_trial"), createSimpleTable(null, ModItems.EVENT_HORIZON_ARROW.get(), 0.005f, true));
            output.accept(key("dodoco"), createSimpleTable(null, ModItems.DODOCO.get(), 0.0025f, false));
            output.accept(key("arbiter"), createSimpleTable(null, ModItems.ARBITER_CROSSBOW.get(), 0.01f, true));
            output.accept(key("end_weapons_end"), createMultiTable(0.005f, true, ModItems.LAW_BREAKER.get(), ModItems.QILINS_WRATH.get(), ModItems.STELLA_PERDITOR.get()));
            output.accept(key("end_weapons_trial"), createMultiTable(0.01f, true, ModItems.LAW_BREAKER.get(), ModItems.QILINS_WRATH.get(), ModItems.STELLA_PERDITOR.get()));

            CompoundTag levelTag = new CompoundTag();
            levelTag.putInt("oriri_level", 1);
            LootPool.Builder vestigePool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f));
            for (var item : ModItems.ITEMS.getEntries()) {
                if (item.get() instanceof VestigeItem) {
                    vestigePool.add(LootItem.lootTableItem(item.get())
                            .apply(SetComponentsFunction.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(levelTag)))
                            .when(LootItemRandomChanceCondition.randomChance(0.001f)));
                }
            }
            output.accept(key("vestige_items"), LootTable.lootTable().withPool(vestigePool));
        }
    }

    public static class Entities implements LootTableSubProvider {
        private final HolderLookup.Provider registries;
        public Entities(HolderLookup.Provider registries) { this.registries = registries; }
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(key("moonstone"), createSimpleTable(registries, ModItems.MOON_STONE.get(), 0.02f, false));
            output.accept(key("damned_soul"), createSimpleTable(registries, ModItems.DAMNED_SOUL.get(), 0.05f, false));
            output.accept(key("hollow_soul"), createSimpleTable(registries, ModItems.HOLLOW_SOUL.get(), 0.05f, false));
            output.accept(key("tortured_soul"), createSimpleTable(registries, ModItems.TORTURED_SOUL.get(), 0.05f, false));
            output.accept(key("void_soul"), createSimpleTable(registries, ModItems.VOID_SOUL.get(), 0.05f, false));
            output.accept(key("power_soul"), createSimpleTable(registries, ModItems.POWER_SOUL.get(), 1.0f, false));
            output.accept(key("dragon_fruit"), createSimpleTable(registries, ModItems.DRAGON_FRUIT.get(), 0.025f, false));
            output.accept(key("one_thousand_screams"), createSimpleTable(registries, ModItems.ONE_THOUSAND_SCREAMS.get(), 0.25f, false));
            output.accept(key("iras_soul"), createSimpleTable(registries, ModItems.IRAS_SOUL_FRAGMENT.get(), 1.0f, false));

            output.accept(key("zombie_encyclopedia"), createSimpleTable(registries, ModItems.ZOMBIE_ENCYCLOPEDIA.get(), 0.05f, false));
            output.accept(key("skeleton_encyclopedia"), createSimpleTable(registries, ModItems.SKELETON_ENCYCLOPEDIA.get(), 0.05f, false));
            output.accept(key("iron_golem_manual"), createSimpleTable(registries, ModItems.IRON_GOLEM_MANUAL.get(), 0.05f, false));
            output.accept(key("blazing_pyromaniac_guide"), createSimpleTable(registries, ModItems.BLAZING_PYROMANIAC_GUIDE.get(), 0.05f, false));
            output.accept(key("magma_cooking_book"), createSimpleTable(registries, ModItems.MAGMA_COOKING_BOOK.get(), 0.05f, false));
            output.accept(key("slimy_cooking_book"), createSimpleTable(registries, ModItems.SLIMY_COOKING_BOOK.get(), 0.05f, false));
        }
    }

    public static class Blocks implements LootTableSubProvider {
        public Blocks(HolderLookup.Provider registries) {}
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(key("iron_roots"), createSimpleTable(null, ModItems.IRON_ROOTS.get(), 0.05f, false));
            output.accept(key("calcium_currants"), createSimpleTable(null, ModItems.CALCIUM_CURRANT.get(), 0.0001f, false));
            output.accept(key("the_first_apple"), createSimpleTable(null, ModItems.THE_FIRST_APPLE.get(), 0.001f, false));
            output.accept(key("four_leaf_clover"), createSimpleTable(null, ModItems.FOUR_LEAF_CLOVER.get(), 0.0001f, false));
            output.accept(key("miracle_seaweed"), createSimpleTable(null, ModItems.MIRACLE_SEAWEED.get(), 0.001f, false));
        }
    }
}
