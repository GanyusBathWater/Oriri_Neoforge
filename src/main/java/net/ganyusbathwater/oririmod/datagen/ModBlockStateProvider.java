package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

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
        pillarBlockWithItem(ModBlocks.ELDERBUSH_BLOCK, 3);
        blockWithItem(ModBlocks.ELDER_LEAVES, 3);
        blockWithItem(ModBlocks.ELDER_LEAVES_FLOWERING, 3);

        blockWithItem(ModBlocks.MAGIC_BARRIER_BLOCK, 4);
        blockWithItem(ModBlocks.MAGIC_BARRIER_CORE_BLOCK, 4);

        grassBlockWithItem(ModBlocks.DARK_SOIL_BLOCK, 1);

        blockWithItem(ModBlocks.MANA_CRYSTAL_BLOCK, 1);
        clusterBlockWithItem(ModBlocks.MANA_CRYSTAL_CLUSTER, 2);


        blockWithItem(ModBlocks.ELDER_STEM_BLOCK, 1);
        //blockWithItem(ModBlocks.STRIPPED_ELDER_STEM_BLOCK, 1);
        //pillarBlockWithItem(ModBlocks.ELDER_LOG_BLOCK, 1);
        pillarBlockWithItem(ModBlocks.CRACKED_ELDER_LOG_BLOCK, 1);
        pillarBlockWithItem(ModBlocks.STRIPPED_ELDER_LOG_BLOCK, 1);
        blockWithItem(ModBlocks.ELDER_PLANKS, 1);
        axisBlock(((RotatedPillarBlock) ModBlocks.ELDER_LOG_BLOCK.get()), ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/elder_log_block_side"), ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/elder_log_block_top_bottom"));
        axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get()), ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/stripped_elder_log_block_side"), ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/elder_log_block_top_bottom"));


        stairsBlock(ModBlocks.ELDER_STAIRS.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
        slabBlock(ModBlocks.ELDER_SLAB.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()), blockTexture(ModBlocks.ELDER_PLANKS.get()));
        fenceBlock(ModBlocks.ELDER_FENCE.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
        fenceGateBlock(ModBlocks.ELDER_GATE.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));

        saplingBlock(ModBlocks.ELDER_SAPLING);
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

    private void clusterBlockWithItem(DeferredBlock<?> deferredBlock, int renderType) {
        String name = deferredBlock.getId().getPath();
        ResourceLocation texture = modLoc("block/" + name);

        BlockModelBuilder model = models().cross(name, texture);

        if (renderType > 1) {
            model.texture("particle", texture);
            model.renderType(getRenderType(renderType));
        } else {
            model.renderType("cutout");
        }

        // BlockStates for all FACING variants
        getVariantBuilder(deferredBlock.get()).forAllStates(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            int rotationX = 0;
            int rotationY = 0;

            switch (dir) {
                case UP -> {
                    rotationX = 0;      // stands on the ground (vertical)
                    rotationY = 0;
                }
                case DOWN -> {
                    rotationX = 180;    // hangs from the ceiling (vertical, inverted)
                    rotationY = 0;
                }
                case NORTH -> {
                    rotationX = 90;     // on the north wall
                    rotationY = 0;
                }
                case SOUTH -> {
                    rotationX = 90;     // on the south wall
                    rotationY = 180;
                }
                case WEST -> {
                    rotationX = 90;     // on the west wall
                    rotationY = 270;
                }
                case EAST -> {
                    rotationX = 90;     // on the east wall
                    rotationY = 90;
                }
            }

            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(rotationX)
                    .rotationY(rotationY)
                    .build();
        });
        simpleBlockItem(deferredBlock.get(), model);
    }

    private void saplingBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(),
                models().cross(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }


}
