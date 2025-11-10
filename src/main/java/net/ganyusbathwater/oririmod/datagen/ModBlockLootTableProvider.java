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

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(ModBlocks.DARK_SOIL_BLOCK.get(), block -> createSilkTouchOnlyTable(ModBlocks.DARK_SOIL_BLOCK.get()));
        add(ModBlocks.ELDERBUSH_BLOCK.get(), item -> createSingleItemTable(ModItems.ELDERBERRY.get()));
        add(ModBlocks.MANA_CRYSTAL_BLOCK.get(), block -> createSilkTouchOnlyTable(ModBlocks.MANA_CRYSTAL_BLOCK.get()));
        add(ModBlocks.MANA_CRYSTAL_CLUSTER.get(), item -> createSingleItemTable(ModItems.MANA_MANIFESTATION.get()));
        add(ModBlocks.ELDER_LEAVES.get(), block -> createShearsOnlyDrop(ModBlocks.ELDER_LEAVES.get()));
        add(ModBlocks.ELDER_LEAVES_FLOWERING.get(), block -> createShearsOnlyDrop(ModBlocks.ELDER_LEAVES_FLOWERING.get()));

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


        this.add(ModBlocks.ELDER_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.ELDER_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));

        this.add(ModBlocks.ELDER_LEAVES_FLOWERING.get(), block ->
                createLeavesDrops(block, ModBlocks.ELDER_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));

    }

    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
