package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, OririMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        pillarBlockWithItem(ModBlocks.ELDERBUSH_BLOCK);

        blockWithItem(ModBlocks.MAGIC_BARRIER_BLOCK);
        blockWithItem(ModBlocks.MAGIC_BARRIER_CORE_BLOCK);

        grassBlockWithItem(ModBlocks.DARK_SOIL);

    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("oririmod:block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("oririmod:block/" + deferredBlock.getId().getPath() + appendix));
    }

    private void grassBlockWithItem(DeferredBlock<?> deferredBlock){
        String name = (deferredBlock.getId().getPath()); // Blockname -> z.B. "my_grass_block"
        ResourceLocation top = modLoc("block/" + name + "_top");
        ResourceLocation bottom = modLoc("block/" + name + "_bottom");
        ResourceLocation side = modLoc("block/" + name + "_side");

        simpleBlockWithItem(
                deferredBlock.get(),
                models().cubeBottomTop(name, side, bottom, top)
        );
    }

    private void pillarBlockWithItem(DeferredBlock<?> deferredBlock) {
        String name = (deferredBlock.getId().getPath()); // z.B. "magic_log"
        ResourceLocation side = modLoc("block/" + name + "_side");
        ResourceLocation top = modLoc("block/" + name + "_top_bottom");

        simpleBlockWithItem(
                deferredBlock.get(),
                models().cubeColumn(name, side, top)
        );
    }
}
