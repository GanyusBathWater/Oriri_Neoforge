package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, OririMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(net.ganyusbathwater.oririmod.util.ModTags.Items.MANA_WEAPONS)
                .add(
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_WISE.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_EARTH.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_FOREST.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.ONE_THOUSAND_SCREAMS.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_HELL.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_COSMOS.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_VOID.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_ETERNAL_ICE.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.DODOCO.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.BOOK_OF_AMATEUR.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.BOOK_OF_APPRENTICE.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.BOOK_OF_JOURNEYMAN.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.BOOK_OF_WISE.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.STAFF_OF_ALMIGHTY.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.ZOMBIE_ENCYCLOPEDIA.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.SKELETON_ENCYCLOPEDIA.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.IRON_GOLEM_MANUAL.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.BLAZING_PYROMANIAC_GUIDE.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.MAGMA_COOKING_BOOK.get(),
                        net.ganyusbathwater.oririmod.item.ModItems.SLIMY_COOKING_BOOK.get());

        this.copy(BlockTags.LOGS, ItemTags.LOGS);
        this.copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
        this.copy(BlockTags.PLANKS, ItemTags.PLANKS);
        this.copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
        this.copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
        this.copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
        this.copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
        this.copy(BlockTags.LEAVES, ItemTags.LEAVES);
    }
}
