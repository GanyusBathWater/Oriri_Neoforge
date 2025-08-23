package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, OririMod.MOD_ID, exFileHelper);
    }

    private String getRenderType(int renderType) {
        return switch (renderType) {
            case 2 -> "minecraft:cutout";
            case 3 -> "minecraft:cutout_mipped";
            case 4 -> "minecraft:translucent";
            case 5 -> "minecraft:tripwire";
            default -> "minecraft:solid";
        };
    }


    @Override
    protected void registerStatesAndModels() {
        pillarBlockWithItem(ModBlocks.ELDERBUSH_BLOCK, 2);

        blockWithItem(ModBlocks.MAGIC_BARRIER_BLOCK, 4);
        blockWithItem(ModBlocks.MAGIC_BARRIER_CORE_BLOCK, 4);

        grassBlockWithItem(ModBlocks.DARK_SOIL, 1);

    }

    private void blockWithItem(DeferredBlock<?> deferredBlock, int renderType) {
        String name = (deferredBlock.getId().getPath());
        ResourceLocation path = modLoc("block/" + name);

        BlockModelBuilder model = models().cubeAll(name, path);

        if (renderType > 1) {
            model.texture("particle", path);
            model.renderType(getRenderType(renderType));
        }

        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("oririmod:block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("oririmod:block/" + deferredBlock.getId().getPath() + appendix));
    }

    private void grassBlockWithItem(DeferredBlock<?> deferredBlock, int renderType){
        String name = (deferredBlock.getId().getPath());
        ResourceLocation top = modLoc("block/" + name + "_top");
        ResourceLocation bottom = modLoc("block/" + name + "_bottom");
        ResourceLocation side = modLoc("block/" + name + "_side");

        BlockModelBuilder model = models().cubeBottomTop(name, side, bottom, top);
        if (renderType > 1) {
            model.texture("particle", side);
            model.renderType(getRenderType(renderType));
        }

        simpleBlockWithItem(
                deferredBlock.get(),
                model
        );
    }

    private void pillarBlockWithItem(DeferredBlock<?> deferredBlock, int renderType) {
        String name = (deferredBlock.getId().getPath());
        ResourceLocation side = modLoc("block/" + name + "_side");
        ResourceLocation top = modLoc("block/" + name + "_top_bottom");

        BlockModelBuilder model = models().cubeColumn(name, side, top);

        if (renderType > 1) {
            model.texture("particle", side);
            model.renderType(getRenderType(renderType));
        }

        simpleBlockWithItem(
                deferredBlock.get(),
                model
        );
    }
}
