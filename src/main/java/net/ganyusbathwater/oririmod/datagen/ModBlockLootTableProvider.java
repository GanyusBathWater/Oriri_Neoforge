package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;
import net.minecraft.world.level.block.Blocks;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
        protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
                super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
                add(ModBlocks.DARK_SOIL_BLOCK.get(),
                                block -> createSilkTouchOnlyTable(ModBlocks.DARK_SOIL_BLOCK.get()));
                add(ModBlocks.ELDERBUSH_BLOCK.get(), item -> createSingleItemTable(ModItems.ELDERBERRY.get()));
                add(ModBlocks.MANA_CRYSTAL_BLOCK.get(),
                                block -> createSilkTouchOnlyTable(ModBlocks.MANA_CRYSTAL_BLOCK.get()));
                add(ModBlocks.MANA_CRYSTAL_CLUSTER.get(),
                                item -> createSingleItemTable(ModItems.MANA_MANIFESTATION.get()));
                add(ModBlocks.MANA_CRYSTAL_CLUSTER.get(),
                                item -> createSingleItemTable(ModItems.MANA_MANIFESTATION.get()));

                add(ModBlocks.FLUORITE_BLOCK.get(),
                                block -> createSilkTouchOnlyTable(ModBlocks.FLUORITE_BLOCK.get()));
                add(ModBlocks.FLUORITE_CLUSTER.get(),
                                block -> createSilkTouchDispatchTable(block,
                                                LootItem.lootTableItem(ModItems.FLUORITE_CRYSTAL.get())
                                                                .apply(SetItemCountFunction
                                                                                .setCount(UniformGenerator.between(1.0F,
                                                                                                4.0F)))
                                                                .apply(ApplyBonusCount.addOreBonusCount(
                                                                                this.registries.lookupOrThrow(
                                                                                                Registries.ENCHANTMENT)
                                                                                                .getOrThrow(Enchantments.FORTUNE)))));

                add(ModBlocks.ELDER_LEAVES.get(), block -> createShearsOnlyDrop(ModBlocks.ELDER_LEAVES.get()));
                add(ModBlocks.ELDER_LEAVES_FLOWERING.get(),
                                block -> createShearsOnlyDrop(ModBlocks.ELDER_LEAVES_FLOWERING.get()));

                this.dropSelf(ModBlocks.ELDER_LOG_BLOCK.get());
                this.dropSelf(ModBlocks.CRACKED_ELDER_LOG_BLOCK.get());
                this.dropSelf(ModBlocks.ELDER_PLANKS.get());
                this.dropSelf(ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get());
                this.dropSelf(ModBlocks.ELDER_SAPLING.get());
                this.dropSelf(ModBlocks.ELDER_STEM_BLOCK.get());
                this.dropSelf(ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get());
                this.dropSelf(ModBlocks.ELDER_SPORE_BLOSSOM.get());
                this.dropSelf(ModBlocks.ELDER_STAIRS.get());
                this.dropSelf(ModBlocks.ELDER_SLAB.get());
                this.dropSelf(ModBlocks.ELDER_FENCE.get());
                this.dropSelf(ModBlocks.ELDER_GATE.get());
                this.dropSelf(ModBlocks.STAR_HERB.get());

                this.add(ModBlocks.ELDER_LEAVES.get(),
                                block -> createLeavesDrops(block, ModBlocks.ELDER_SAPLING.get(),
                                                NORMAL_LEAVES_SAPLING_CHANCES));

                this.add(ModBlocks.ELDER_LEAVES_FLOWERING.get(),
                                block -> createLeavesDrops(block, ModBlocks.ELDER_SAPLING.get(),
                                                NORMAL_LEAVES_SAPLING_CHANCES));

                // ===== SCARLET STONE VARIANTS =====
                this.dropSelf(ModBlocks.SCARLET_STONE.get());
                this.dropSelf(ModBlocks.COBBLED_SCARLET_DEEPSLATE.get());
                this.dropSelf(ModBlocks.SMOOTH_SCARLET_STONE.get());
                this.add(ModBlocks.SMOOTH_SCARLET_STONE_SLAB.get(), this::createSlabItemTable);
                this.dropSelf(ModBlocks.SCARLET_STONE_BRICKS.get());
                this.dropSelf(ModBlocks.CHISELED_SCARLET_STONE_BRICKS.get());
                this.dropSelf(ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get());
                this.dropSelf(ModBlocks.MOSSY_SCARLET_STONE_BRICKS.get());

                // ===== SCARLET DEEPSLATE VARIANTS =====
                this.dropSelf(ModBlocks.SCARLET_DEEPSLATE.get());
                this.dropSelf(ModBlocks.POLISHED_SCARLET_DEEPSLATE.get());
                this.dropSelf(ModBlocks.CHISELED_SCARLET_DEEPSLATE.get());
                this.dropSelf(ModBlocks.SCARLET_DEEPSLATE_BRICKS.get());
                this.dropSelf(ModBlocks.CRACKED_SCARLET_DEEPSLATE_BRICKS.get());
                this.dropSelf(ModBlocks.SCARLET_DEEPSLATE_TILES.get());
                this.dropSelf(ModBlocks.CRACKED_SCARLET_DEEPSLATE_TILES.get());

                // ===== SCARLET WOOD TYPES =====
                this.dropSelf(ModBlocks.SCARLET_LOG.get());
                this.dropSelf(ModBlocks.STRIPPED_SCARLET_LOG.get());
                this.dropSelf(ModBlocks.SCARLET_PLANKS.get());
                this.dropSelf(ModBlocks.SCARLET_STAIRS.get());
                this.add(ModBlocks.SCARLET_SLAB.get(), this::createSlabItemTable);
                this.dropSelf(ModBlocks.SCARLET_FENCE.get());
                this.dropSelf(ModBlocks.SCARLET_GATE.get());
                this.dropSelf(ModBlocks.SCARLET_SAPLING.get());

                // ===== SCARLET VEGETATION =====
                this.add(ModBlocks.SCARLET_LEAVES.get(),
                                block -> createLeavesDrops(block, ModBlocks.SCARLET_SAPLING.get(),
                                                NORMAL_LEAVES_SAPLING_CHANCES));
                this.dropSelf(ModBlocks.SCARLET_MOSS.get());
                this.add(ModBlocks.SCARLET_VINE.get(), block -> createShearsOnlyDrop(ModBlocks.SCARLET_VINE.get()));

                // ===== JADE BLOCKS =====
                this.dropSelf(ModBlocks.JADE_BLOCK.get());
                this.add(ModBlocks.JADE_ORE.get(),
                                block -> createMultipleOreDrops(block, ModItems.JADE.get(), 2.0F, 5.0F));
                this.add(ModBlocks.DEEPSLATE_JADE_ORE.get(),
                                block -> createMultipleOreDrops(block, ModItems.JADE.get(), 2.0F, 5.0F));
                this.dropSelf(ModBlocks.JADE_STAIRS.get());
                this.add(ModBlocks.JADE_SLAB.get(), this::createSlabItemTable);
                this.dropSelf(ModBlocks.JADE_WALL.get());

                // ===== NEW SCARLET BLOCKS =====
                this.add(ModBlocks.SCARLET_GRASS_BLOCK.get(),
                                block -> createSingleItemTableWithSilkTouch(block, Blocks.DIRT));
                this.dropSelf(ModBlocks.SCARLET_STEM.get());
                this.dropSelf(ModBlocks.STRIPPED_SCARLET_STEM.get());
                this.add(ModBlocks.SCARLET_GRASS.get(), block -> createShearsOnlyDrop(block));
                this.dropSelf(ModBlocks.SCARLET_TOOTH_LEAVES.get()); // Instabreak, so drops self
                this.dropSelf(ModBlocks.SCARLET_LILY.get());
                this.dropSelf(ModBlocks.SCARLET_DRIPSTONE_BLOCK.get());
                this.dropSelf(ModBlocks.POINTED_SCARLET_DRIPSTONE.get());

                // Upgraded Saplings
                this.dropSelf(ModBlocks.UPGRADED_SCARLET_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_ELDER_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_OAK_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_SPRUCE_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_BIRCH_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_JUNGLE_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_ACACIA_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_DARK_OAK_SAPLING.get());
                this.dropSelf(ModBlocks.UPGRADED_CHERRY_SAPLING.get());

        }

        protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
                HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries
                                .lookupOrThrow(Registries.ENCHANTMENT);
                return this.createSilkTouchDispatchTable(pBlock,
                                this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                                                .apply(SetItemCountFunction
                                                                .setCount(UniformGenerator.between(minDrops, maxDrops)))
                                                .apply(ApplyBonusCount.addOreBonusCount(
                                                                registrylookup.getOrThrow(Enchantments.FORTUNE)))));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
                return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
        }
}
