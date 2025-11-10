package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, OririMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(BlockTags.LOGS)
                .add(ModBlocks.ELDER_LOG_BLOCK.get())
                .add(ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get())
                .add(ModBlocks.ELDER_STEM_BLOCK.get())
                .add(ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get())
                .add(ModBlocks.CRACKED_ELDER_LOG_BLOCK.get());

        tag(ModTags.Blocks.ELDERWOODS_PROTECTED_STRUCTURE_BLOCKS)
                .add(Blocks.STONE_BRICKS)
                .add(Blocks.STONE_BRICK_SLAB)
                .add(Blocks.STONE_BRICK_STAIRS)
                .add(Blocks.CRACKED_STONE_BRICKS)
                .add(Blocks.MOSSY_STONE_BRICKS)
                .add(Blocks.MOSSY_STONE_BRICK_SLAB)
                .add(Blocks.MOSSY_STONE_BRICK_STAIRS)
                .add(Blocks.BRICK_STAIRS)
                .add(Blocks.BRICK_SLAB)
                .add(Blocks.BRICKS)
                .add(Blocks.LANTERN)
                .add(Blocks.SOUL_LANTERN);

        tag(BlockTags.LEAVES)
                .add(ModBlocks.ELDER_LEAVES.get())
                .add(ModBlocks.ELDER_LEAVES_FLOWERING.get());

        tag(BlockTags.FENCES)
                .add(ModBlocks.ELDER_FENCE.get());

        tag(BlockTags.FENCE_GATES)
                .add(ModBlocks.ELDER_GATE.get());
    }
}
