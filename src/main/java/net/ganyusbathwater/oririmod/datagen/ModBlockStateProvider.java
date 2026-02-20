package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
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

        blockWithItem(ModBlocks.FLUORITE_BLOCK, 4);
        fluoriteClusterState(ModBlocks.FLUORITE_CLUSTER);

        blockWithItem(ModBlocks.ELDER_STEM_BLOCK, 1);
        // blockWithItem(ModBlocks.STRIPPED_ELDER_STEM_BLOCK, 1);
        // pillarBlockWithItem(ModBlocks.ELDER_LOG_BLOCK, 1);
        pillarBlockWithItem(ModBlocks.CRACKED_ELDER_LOG_BLOCK, 1);
        pillarBlockWithItem(ModBlocks.STRIPPED_ELDER_LOG_BLOCK, 1);
        blockWithItem(ModBlocks.ELDER_PLANKS, 1);
        axisBlock(((RotatedPillarBlock) ModBlocks.ELDER_LOG_BLOCK.get()),
                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/elder_log_block_side"),
                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/elder_log_block_top_bottom"));
        axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get()),
                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/stripped_elder_log_block_side"),
                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/elder_log_block_top_bottom"));

        stairsBlock(ModBlocks.ELDER_STAIRS.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
        slabBlock(ModBlocks.ELDER_SLAB.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()),
                blockTexture(ModBlocks.ELDER_PLANKS.get()));
        fenceBlock(ModBlocks.ELDER_FENCE.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
        fenceGateBlock(ModBlocks.ELDER_GATE.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));

        saplingBlock(ModBlocks.ELDER_SAPLING);
        saplingBlock(ModBlocks.STAR_HERB);

        // Scarlet Blocks
        grassBlockWithItem(ModBlocks.SCARLET_GRASS_BLOCK, 1);
        blockWithItem(ModBlocks.SCARLET_LEAVES, 3); // Cutout Mipped

        // Logs and Wood
        axisBlock(((RotatedPillarBlock) ModBlocks.SCARLET_LOG.get()),
                modLoc("block/scarlet_log"),
                modLoc("block/scarlet_log_top"));
        axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_SCARLET_LOG.get()),
                modLoc("block/stripped_scarlet_log"),
                modLoc("block/stripped_scarlet_log_top"));

        blockWithItem(ModBlocks.SCARLET_PLANKS, 1);
        stairsBlock(ModBlocks.SCARLET_STAIRS.get(), blockTexture(ModBlocks.SCARLET_PLANKS.get()));
        slabBlock(ModBlocks.SCARLET_SLAB.get(), blockTexture(ModBlocks.SCARLET_PLANKS.get()),
                blockTexture(ModBlocks.SCARLET_PLANKS.get()));
        fenceBlock(ModBlocks.SCARLET_FENCE.get(), blockTexture(ModBlocks.SCARLET_PLANKS.get()));
        fenceGateBlock(ModBlocks.SCARLET_GATE.get(), blockTexture(ModBlocks.SCARLET_PLANKS.get()));

        // Stems use log texture on all sides
        simpleBlockWithItem(ModBlocks.SCARLET_STEM.get(),
                models().cubeAll("scarlet_stem", modLoc("block/scarlet_log")));
        simpleBlockWithItem(ModBlocks.STRIPPED_SCARLET_STEM.get(),
                models().cubeAll("stripped_scarlet_stem", modLoc("block/stripped_scarlet_log")));

        simpleBlock(ModFluids.BLOOD_WATER_BLOCK.get(),
                models().getBuilder("blood_water_block").texture("particle", modLoc("block/blood_water_still")));

        // Saplings
        simpleBlockWithItem(ModBlocks.SCARLET_SAPLING.get(),
                models().cross("scarlet_sapling", modLoc("block/scarlet_sapling")).renderType("cutout"));

        // Vegetation
        simpleBlockWithItem(ModBlocks.SCARLET_GRASS.get(),
                models().cross("scarlet_grass", modLoc("block/scarlet_grass")).renderType("cutout"));

        // Tooth Leaves - cross model
        simpleBlockWithItem(ModBlocks.SCARLET_TOOTH_LEAVES.get(),
                models().cross("scarlet_tooth_leaves", modLoc("block/scarlet_tooth_leaves")).renderType("cutout"));

        // Lily - flat model facing up
        simpleBlockWithItem(ModBlocks.SCARLET_LILY.get(),
                models().getExistingFile(modLoc("block/scarlet_lily")));

        // ===== SCARLET DRIPSTONE =====
        // Scarlet Dripstone Block — simple cube using scarlet_stone texture
        simpleBlockWithItem(ModBlocks.SCARLET_DRIPSTONE_BLOCK.get(),
                models().cubeAll("scarlet_dripstone_block", modLoc("block/scarlet_dripstone_block")));

        // Pointed Scarlet Dripstone — direction × thickness variants
        pointedDripstoneBlock();
    }

    private void pointedDripstoneBlock() {
        Block block = ModBlocks.POINTED_SCARLET_DRIPSTONE.get();
        getVariantBuilder(block).forAllStates(state -> {
            Direction dir = state.getValue(BlockStateProperties.VERTICAL_DIRECTION);
            String dirStr = dir == Direction.UP ? "up" : "down";
            String thickness = state.getValue(BlockStateProperties.DRIPSTONE_THICKNESS).getSerializedName();
            String modelName = "pointed_scarlet_dripstone_" + dirStr + "_" + thickness;
            String textureName = "pointed_scarlet_stone_" + dirStr + "_" + thickness;

            return ConfiguredModel.builder()
                    .modelFile(models().cross(modelName, modLoc("block/" + textureName)).renderType("cutout"))
                    .build();
        });
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

    private void grassBlockWithItem(DeferredBlock<?> deferredBlock, int renderType) {
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
                model);
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
                model);
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
                    rotationX = 0; // stands on the ground (vertical)
                    rotationY = 0;
                }
                case DOWN -> {
                    rotationX = 180; // hangs from the ceiling (vertical, inverted)
                    rotationY = 0;
                }
                case NORTH -> {
                    rotationX = 90; // on the north wall
                    rotationY = 0;
                }
                case SOUTH -> {
                    rotationX = 90; // on the south wall
                    rotationY = 180;
                }
                case WEST -> {
                    rotationX = 90; // on the west wall
                    rotationY = 270;
                }
                case EAST -> {
                    rotationX = 90; // on the east wall
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
                models().cross(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(),
                        blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    private void fluoriteClusterState(DeferredBlock<?> deferredBlock) {
        // Use existing model file (manual OBJ loader json)
        net.neoforged.neoforge.client.model.generators.ModelFile model = models()
                .getExistingFile(modLoc("block/" + deferredBlock.getId().getPath()));

        getVariantBuilder(deferredBlock.get()).forAllStates(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            int rotationX = 0;
            int rotationY = 0;

            switch (dir) {
                case UP -> {
                    rotationX = 0;
                    rotationY = 0;
                }
                case DOWN -> {
                    rotationX = 180;
                    rotationY = 0;
                }
                case NORTH -> {
                    rotationX = 90;
                    rotationY = 0;
                }
                case SOUTH -> {
                    rotationX = 90;
                    rotationY = 180;
                }
                case WEST -> {
                    rotationX = 90;
                    rotationY = 270;
                }
                case EAST -> {
                    rotationX = 90;
                    rotationY = 90;
                }
            }

            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(rotationX)
                    .rotationY(rotationY)
                    .build();
        });
    }

}
