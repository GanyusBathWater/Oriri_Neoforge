package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModChestLootTableProvider implements LootTableSubProvider {
    private final HolderLookup.Provider registries;

    public ModChestLootTableProvider(HolderLookup.Provider provider) {
        this.registries = provider;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        
        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost/storage")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.ELDER_LOG_BLOCK).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.ELDER_PLANKS).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.COBBLESTONE).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.ELDER_SAPLING).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost/training")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_AMATEUR).setWeight(6))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_APPRENTICE).setWeight(5))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_JOURNEYMAN).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_WISE).setWeight(1))
                        .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(10).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND_SWORD).setWeight(5).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F))))
                        .add(LootItem.lootTableItem(Items.SHIELD).setWeight(10).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F))))
                        .add(LootItem.lootTableItem(Items.POTION).setWeight(5).apply(SetPotionFunction.setPotion(Potions.SWIFTNESS)))
                        .add(LootItem.lootTableItem(Items.POTION).setWeight(5).apply(SetPotionFunction.setPotion(Potions.STRENGTH)))
                        .add(LootItem.lootTableItem(Items.POTION).setWeight(5).apply(SetPotionFunction.setPotion(Potions.HEALING)))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost/kitchen")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.POTATO).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.CARROT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.COOKED_BEEF).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.COOKED_PORKCHOP).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.PUMPKIN_PIE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(Items.APPLE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.WHEAT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.PORKCHOP).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.BEEF).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(ModItems.ELDERBERRY).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.MUSHROOM_STEW).setWeight(5))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost/library")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(registries)))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.WRITABLE_BOOK).setWeight(5))
                        .add(LootItem.lootTableItem(Items.FEATHER).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost_ruins/storage")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.COBWEB).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.DIRT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GRAVEL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.ELDER_LOG_BLOCK).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.ELDER_PLANKS).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.COBBLESTONE).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.ELDER_SAPLING).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost_ruins/training")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.COBWEB).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.DIRT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GRAVEL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_AMATEUR).setWeight(6))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_APPRENTICE).setWeight(5))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_JOURNEYMAN).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOK_OF_WISE).setWeight(1))
                        .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(10).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND_SWORD).setWeight(5).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F))))
                        .add(LootItem.lootTableItem(Items.SHIELD).setWeight(10).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.9F))))
                        .add(LootItem.lootTableItem(Items.POTION).setWeight(5).apply(SetPotionFunction.setPotion(Potions.SWIFTNESS)))
                        .add(LootItem.lootTableItem(Items.POTION).setWeight(5).apply(SetPotionFunction.setPotion(Potions.STRENGTH)))
                        .add(LootItem.lootTableItem(Items.POTION).setWeight(5).apply(SetPotionFunction.setPotion(Potions.HEALING)))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost_ruins/kitchen")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.COBWEB).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.DIRT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GRAVEL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.POISONOUS_POTATO).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(40).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.WHEAT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                        .add(LootItem.lootTableItem(ModItems.ELDERBERRY).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.SUSPICIOUS_STEW).setWeight(10))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost_ruins/library")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.COBWEB).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.DIRT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GRAVEL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(15).apply(EnchantRandomlyFunction.randomApplicableEnchantment(registries)))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                        .add(LootItem.lootTableItem(Items.WRITABLE_BOOK).setWeight(5))
                        .add(LootItem.lootTableItem(Items.FEATHER).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));

        output.accept(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "chests/outpost_ruins/scarlet_storage")),
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(4.0F, 8.0F))
                        .add(LootItem.lootTableItem(Items.COBWEB).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        .add(LootItem.lootTableItem(Items.DIRT).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.GRAVEL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.SCARLET_LOG).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.SCARLET_PLANKS).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.SCARLET_COBBLESTONE).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 16.0F))))
                        .add(LootItem.lootTableItem(ModBlocks.SCARLET_SAPLING).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));
    }
}
