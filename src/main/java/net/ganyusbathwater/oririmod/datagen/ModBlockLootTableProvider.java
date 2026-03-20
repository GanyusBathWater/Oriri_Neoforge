package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    public ModBlockLootTableProvider(HolderLookup.Provider provider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.DARK_SOIL_BLOCK.get());
        dropSelf(ModBlocks.BLOOD_SLUDGE.get());
        add(ModBlocks.ELDERBUSH_BLOCK.get(), block -> createShearsOnlyDrop(ModBlocks.ELDERBUSH_BLOCK.get()));
        dropSelf(ModBlocks.MANA_CRYSTAL_BLOCK.get());
        add(ModBlocks.MANA_CRYSTAL_CLUSTER.get(), block -> createSilkTouchOnlyTable(ModBlocks.MANA_CRYSTAL_CLUSTER.get()));
        dropSelf(ModBlocks.ELDER_LOG_BLOCK.get());
        dropSelf(ModBlocks.CRACKED_ELDER_LOG_BLOCK.get());
        dropSelf(ModBlocks.ELDER_PLANKS.get());
        dropSelf(ModBlocks.ELDER_STAIRS.get());
        add(ModBlocks.ELDER_SLAB.get(), block -> createSlabItemTable(ModBlocks.ELDER_SLAB.get()));
        dropSelf(ModBlocks.ELDER_FENCE.get());
        dropSelf(ModBlocks.ELDER_GATE.get());
        dropSelf(ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get());
        dropSelf(ModBlocks.ELDER_STEM_BLOCK.get());
        dropSelf(ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get());
        add(ModBlocks.ELDER_LEAVES.get(), block -> createLeavesDrops(block, ModBlocks.ELDER_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        add(ModBlocks.ELDER_LEAVES_FLOWERING.get(), block -> createLeavesDrops(block, ModBlocks.ELDER_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropSelf(ModBlocks.ELDER_SPORE_BLOSSOM.get());
        dropSelf(ModBlocks.ELDER_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_ELDER_SAPLING.get());

        // Scarlet Blocks
        dropSelf(ModBlocks.SCARLET_STONE.get());
        dropSelf(ModBlocks.COBBLED_SCARLET_DEEPSLATE.get());
        dropSelf(ModBlocks.SMOOTH_SCARLET_STONE.get());
        add(ModBlocks.SMOOTH_SCARLET_STONE_SLAB.get(), block -> createSlabItemTable(ModBlocks.SMOOTH_SCARLET_STONE_SLAB.get()));
        dropSelf(ModBlocks.SCARLET_STONE_BRICKS.get());
        dropSelf(ModBlocks.CHISELED_SCARLET_STONE_BRICKS.get());
        dropSelf(ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get());
        dropSelf(ModBlocks.MOSSY_SCARLET_STONE_BRICKS.get());

        dropSelf(ModBlocks.SCARLET_DEEPSLATE.get());
        dropSelf(ModBlocks.POLISHED_SCARLET_DEEPSLATE.get());
        dropSelf(ModBlocks.CHISELED_SCARLET_DEEPSLATE.get());
        dropSelf(ModBlocks.SCARLET_DEEPSLATE_BRICKS.get());
        dropSelf(ModBlocks.CRACKED_SCARLET_DEEPSLATE_BRICKS.get());
        dropSelf(ModBlocks.SCARLET_DEEPSLATE_TILES.get());
        dropSelf(ModBlocks.CRACKED_SCARLET_DEEPSLATE_TILES.get());

        dropSelf(ModBlocks.SCARLET_LOG.get());
        dropSelf(ModBlocks.STRIPPED_SCARLET_LOG.get());
        dropSelf(ModBlocks.SCARLET_PLANKS.get());
        dropSelf(ModBlocks.SCARLET_STAIRS.get());
        add(ModBlocks.SCARLET_SLAB.get(), block -> createSlabItemTable(ModBlocks.SCARLET_SLAB.get()));
        dropSelf(ModBlocks.SCARLET_FENCE.get());
        dropSelf(ModBlocks.SCARLET_GATE.get());
        dropSelf(ModBlocks.SCARLET_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_SCARLET_SAPLING.get());

        add(ModBlocks.SCARLET_LEAVES.get(), block -> createLeavesDrops(block, ModBlocks.SCARLET_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropSelf(ModBlocks.SCARLET_MOSS.get());
        add(ModBlocks.SCARLET_VINE.get(), block -> createShearsOnlyDrop(block));
        dropSelf(ModBlocks.STAR_HERB.get());
        dropSelf(ModBlocks.SCARLET_GRASS_BLOCK.get());
        dropSelf(ModBlocks.SCARLET_STEM.get());
        dropSelf(ModBlocks.STRIPPED_SCARLET_STEM.get());
        dropSelf(ModBlocks.SCARLET_GRASS.get());
        dropSelf(ModBlocks.SCARLET_TOOTH_LEAVES.get());
        dropSelf(ModBlocks.SCARLET_LILY.get());

        dropSelf(ModBlocks.FLUORITE_BLOCK.get());
        add(ModBlocks.FLUORITE_CLUSTER.get(), block -> createSilkTouchOnlyTable(ModBlocks.FLUORITE_CLUSTER.get()));

        dropSelf(ModBlocks.SCARLET_DRIPSTONE_BLOCK.get());
        dropSelf(ModBlocks.POINTED_SCARLET_DRIPSTONE.get());

        dropSelf(ModBlocks.SOL_SAND.get());
        dropSelf(ModBlocks.SOL_SANDSTONE.get());
        dropSelf(ModBlocks.CUT_SOL_SANDSTONE.get());
        dropSelf(ModBlocks.CHISELED_SOL_SANDSTONE.get());

        dropSelf(ModBlocks.BROKEN_SWORD_BLOCK.get());
        dropSelf(ModBlocks.TILTED_BROKEN_SWORD_BLOCK.get());

        dropSelf(ModBlocks.SOL_GLASS.get());
        dropSelf(ModBlocks.SOL_GLASS_PANE.get());

        dropSelf(ModBlocks.JADE_BLOCK.get());
        add(ModBlocks.JADE_ORE.get(), block -> createOreDrop(ModBlocks.JADE_ORE.get(), net.minecraft.world.item.Items.EMERALD));
        add(ModBlocks.DEEPSLATE_JADE_ORE.get(), block -> createOreDrop(ModBlocks.DEEPSLATE_JADE_ORE.get(), net.minecraft.world.item.Items.EMERALD));
        add(ModBlocks.DRAGON_IRON_ORE.get(), block -> createOreDrop(ModBlocks.DRAGON_IRON_ORE.get(), net.minecraft.world.item.Items.RAW_IRON));
        add(ModBlocks.DEEPSLATE_DRAGON_IRON_ORE.get(), block -> createOreDrop(ModBlocks.DEEPSLATE_DRAGON_IRON_ORE.get(), net.minecraft.world.item.Items.RAW_IRON));
        
        dropSelf(ModBlocks.JADE_STAIRS.get());
        add(ModBlocks.JADE_SLAB.get(), block -> createSlabItemTable(ModBlocks.JADE_SLAB.get()));
        dropSelf(ModBlocks.JADE_WALL.get());


        dropSelf(ModBlocks.UPGRADED_OAK_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_SPRUCE_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_BIRCH_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_JUNGLE_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_ACACIA_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_DARK_OAK_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_CHERRY_SAPLING.get());

        dropSelf(ModBlocks.EQUINOX_TABLE.get());

        dropSelf(ModBlocks.HARDENED_MANASHROOM.get());
        dropSelf(ModBlocks.ABYSS_CROWN_LOG.get());
        dropSelf(ModBlocks.STRIPPED_ABYSS_CROWN_LOG.get());
        dropSelf(ModBlocks.ABYSS_CROWN_STEM.get());
        dropSelf(ModBlocks.STRIPPED_ABYSS_CROWN_STEM.get());
        dropSelf(ModBlocks.ABYSS_CROWN_PLANKS.get());
        dropSelf(ModBlocks.ABYSS_CROWN_STAIRS.get());
        add(ModBlocks.ABYSS_CROWN_SLAB.get(), block -> createSlabItemTable(ModBlocks.ABYSS_CROWN_SLAB.get()));
        dropSelf(ModBlocks.ABYSS_CROWN_FENCE.get());
        dropSelf(ModBlocks.ABYSS_CROWN_GATE.get());
        dropSelf(ModBlocks.ABYSS_CROWN_SAPLING.get());
        dropSelf(ModBlocks.UPGRADED_ABYSS_CROWN_SAPLING.get());
        add(ModBlocks.ABYSS_CROWN_LEAVES.get(), block -> createLeavesDrops(block, ModBlocks.ABYSS_CROWN_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(e -> (Block)e.get())
                .filter(block -> block.getLootTable() != net.minecraft.world.level.storage.loot.BuiltInLootTables.EMPTY)::iterator;
    }
}
