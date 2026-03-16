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
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                "block/elder_log_block_top_bottom"));
                axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get()),
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                "block/stripped_elder_log_block_side"),
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                "block/elder_log_block_top_bottom"));

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
                                models().getBuilder("blood_water_block").texture("particle",
                                                modLoc("block/blood_water_still")));

                // Saplings
                simpleBlockWithItem(ModBlocks.SCARLET_SAPLING.get(),
                                models().cross("scarlet_sapling", modLoc("block/scarlet_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_SCARLET_SAPLING.get(),
                                models().cross("upgraded_scarlet_sapling", modLoc("block/scarlet_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_ELDER_SAPLING.get(),
                                models().cross("upgraded_elder_sapling", modLoc("block/elder_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_OAK_SAPLING.get(),
                                models().cross("upgraded_oak_sapling", mcLoc("block/oak_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_SPRUCE_SAPLING.get(),
                                models().cross("upgraded_spruce_sapling", mcLoc("block/spruce_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_BIRCH_SAPLING.get(),
                                models().cross("upgraded_birch_sapling", mcLoc("block/birch_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_JUNGLE_SAPLING.get(),
                                models().cross("upgraded_jungle_sapling", mcLoc("block/jungle_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_ACACIA_SAPLING.get(),
                                models().cross("upgraded_acacia_sapling", mcLoc("block/acacia_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_DARK_OAK_SAPLING.get(),
                                models().cross("upgraded_dark_oak_sapling", mcLoc("block/dark_oak_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_CHERRY_SAPLING.get(),
                                models().cross("upgraded_cherry_sapling", mcLoc("block/cherry_sapling"))
                                                .renderType("cutout"));

                // Vegetation
                simpleBlockWithItem(ModBlocks.SCARLET_GRASS.get(),
                                models().cross("scarlet_grass", modLoc("block/scarlet_grass")).renderType("cutout"));

                // Tooth Leaves - cross model
                simpleBlockWithItem(ModBlocks.SCARLET_TOOTH_LEAVES.get(),
                                models().cross("scarlet_tooth_leaves", modLoc("block/scarlet_tooth_leaves"))
                                                .renderType("cutout"));

                // Lily - flat model facing up
                simpleBlockWithItem(ModBlocks.SCARLET_LILY.get(),
                                models().getExistingFile(modLoc("block/scarlet_lily")));

                // ===== JADE BLOCKS =====
                blockWithItem(ModBlocks.JADE_BLOCK, 1);
                blockWithItem(ModBlocks.JADE_ORE, 1);
                blockWithItem(ModBlocks.DEEPSLATE_JADE_ORE, 1);
                blockWithItem(ModBlocks.DRAGON_IRON_ORE, 1);
                blockWithItem(ModBlocks.DEEPSLATE_DRAGON_IRON_ORE, 1);
                stairsBlock(ModBlocks.JADE_STAIRS.get(), blockTexture(ModBlocks.JADE_BLOCK.get()));
                slabBlock(ModBlocks.JADE_SLAB.get(), blockTexture(ModBlocks.JADE_BLOCK.get()),
                                blockTexture(ModBlocks.JADE_BLOCK.get()));
                wallBlock(ModBlocks.JADE_WALL.get(), blockTexture(ModBlocks.JADE_BLOCK.get()));

                // ===== SCARLET DRIPSTONE =====
                // Scarlet Dripstone Block — simple cube using scarlet_stone texture
                simpleBlockWithItem(ModBlocks.SCARLET_DRIPSTONE_BLOCK.get(),
                                models().cubeAll("scarlet_dripstone_block", modLoc("block/scarlet_dripstone_block")));

                // Pointed Scarlet Dripstone — direction × thickness variants
                pointedDripstoneBlock();

                // ===== SOL SAND BLOCKS =====
                blockWithItem(ModBlocks.SOL_SAND, 1);

                simpleBlockWithItem(ModBlocks.SOL_SANDSTONE.get(),
                                models().cubeBottomTop("sol_sandstone", modLoc("block/sol_sandstone"),
                                                modLoc("block/sol_sandstone_bottom"),
                                                modLoc("block/sol_sandstone_top")));

                simpleBlockWithItem(ModBlocks.CUT_SOL_SANDSTONE.get(),
                                models().cubeBottomTop("cut_sol_sandstone", modLoc("block/cut_sol_sandstone"),
                                                modLoc("block/sol_sandstone_top"), modLoc("block/sol_sandstone_top")));

                simpleBlockWithItem(ModBlocks.CHISELED_SOL_SANDSTONE.get(),
                                models().cubeBottomTop("chiseled_sol_sandstone", modLoc("block/chiseled_sol_sandstone"),
                                                modLoc("block/sol_sandstone_top"), modLoc("block/sol_sandstone_top")));

                // ===== SWORD BLOCKS =====
                horizontalBlock(ModBlocks.BROKEN_SWORD_BLOCK.get(),
                                models().getExistingFile(modLoc("block/broken_sword_block")));
                horizontalBlock(ModBlocks.TILTED_BROKEN_SWORD_BLOCK.get(),
                                models().getExistingFile(modLoc("block/tilted_broken_sword_block")));

                // ===== EQUINOX TABLE =====
                simpleBlockWithItem(ModBlocks.EQUINOX_TABLE.get(),
                                models().cubeBottomTop("equinox_table",
                                                modLoc("block/equinox_table_side"),
                                                modLoc("block/equinox_table_bottom"),
                                                modLoc("block/equinox_table_top")));

                // ===== BLOOD SLUDGE =====
                slimeBlock(ModBlocks.BLOOD_SLUDGE);

                // ===== GLASS & PANES =====
                blockWithItem(ModBlocks.SOL_GLASS, 2); // Cutout

                paneBlockWithRenderType(((net.minecraft.world.level.block.IronBarsBlock) ModBlocks.SOL_GLASS_PANE.get()),
                                modLoc("block/sol_glass"),
                                modLoc("block/sol_glass_pane_top"), "minecraft:cutout");
        }

        private void slimeBlock(DeferredBlock<?> deferredBlock) {
                String name = deferredBlock.getId().getPath();
                ResourceLocation texture = modLoc("block/" + name);

                net.neoforged.neoforge.client.model.generators.BlockModelBuilder model = models()
                                .withExistingParent(name, mcLoc("block/block"))
                                .texture("all", texture)
                                .texture("particle", texture)
                                .renderType("minecraft:translucent");

                model.element()
                                .from(0, 0, 0).to(16, 16, 16)
                                .face(Direction.DOWN).texture("#all").uvs(0, 0, 16, 16).end()
                                .face(Direction.UP).texture("#all").uvs(0, 0, 16, 16).end()
                                .face(Direction.NORTH).texture("#all").uvs(0, 0, 16, 16).end()
                                .face(Direction.SOUTH).texture("#all").uvs(0, 0, 16, 16).end()
                                .face(Direction.WEST).texture("#all").uvs(0, 0, 16, 16).end()
                                .face(Direction.EAST).texture("#all").uvs(0, 0, 16, 16).end()
                                .end();

                model.element()
                                .from(3, 3, 3).to(13, 13, 13)
                                .face(Direction.DOWN).texture("#all").uvs(3, 3, 13, 13).end()
                                .face(Direction.UP).texture("#all").uvs(3, 3, 13, 13).end()
                                .face(Direction.NORTH).texture("#all").uvs(3, 3, 13, 13).end()
                                .face(Direction.SOUTH).texture("#all").uvs(3, 3, 13, 13).end()
                                .face(Direction.WEST).texture("#all").uvs(3, 3, 13, 13).end()
                                .face(Direction.EAST).texture("#all").uvs(3, 3, 13, 13).end()
                                .end();

                simpleBlockWithItem(deferredBlock.get(), model);
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
                                        .modelFile(models().cross(modelName, modLoc("block/" + textureName))
                                                        .renderType("cutout"))
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
                simpleBlockWithItem(deferredBlock.get(), model);
        }

        public void paneBlockWithRenderType(net.minecraft.world.level.block.IronBarsBlock block, ResourceLocation pane,
                        ResourceLocation edge, String renderType) {
                String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
                net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder builder = getMultipartBuilder(
                                block);

                BlockModelBuilder post = models().withExistingParent(name + "_post", mcLoc("block/template_glass_pane_post"))
                                .texture("pane", pane).texture("edge", edge).renderType(renderType);
                BlockModelBuilder side = models().withExistingParent(name + "_side", mcLoc("block/template_glass_pane_side"))
                                .texture("pane", pane).texture("edge", edge).renderType(renderType);
                BlockModelBuilder sideAlt = models()
                                .withExistingParent(name + "_side_alt", mcLoc("block/template_glass_pane_side_alt"))
                                .texture("pane", pane).texture("edge", edge).renderType(renderType);
                BlockModelBuilder noside = models()
                                .withExistingParent(name + "_noside", mcLoc("block/template_glass_pane_noside"))
                                .texture("pane", pane).renderType(renderType);
                BlockModelBuilder nosideAlt = models()
                                .withExistingParent(name + "_noside_alt", mcLoc("block/template_glass_pane_noside_alt"))
                                .texture("pane", pane).renderType(renderType);

                builder.part().modelFile(post).addModel().end();

                builder.part().modelFile(side).addModel().condition(BlockStateProperties.NORTH, true).end();
                builder.part().modelFile(noside).addModel().condition(BlockStateProperties.NORTH, false).end();

                builder.part().modelFile(sideAlt).addModel().condition(BlockStateProperties.SOUTH, true).end();
                builder.part().modelFile(nosideAlt).rotationY(90).addModel().condition(BlockStateProperties.SOUTH, false)
                                .end();

                builder.part().modelFile(sideAlt).rotationY(90).addModel().condition(BlockStateProperties.WEST, true).end();
                builder.part().modelFile(noside).rotationY(270).addModel().condition(BlockStateProperties.WEST, false)
                                .end();

                builder.part().modelFile(side).rotationY(90).addModel().condition(BlockStateProperties.EAST, true).end();
                builder.part().modelFile(nosideAlt).addModel().condition(BlockStateProperties.EAST, false).end();
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
                String name = deferredBlock.getId().getPath();
                ResourceLocation texture = modLoc("block/fluorite_crystal_cluster");
                net.neoforged.neoforge.client.model.generators.BlockModelBuilder model = models()
                                .cross(name, texture).renderType("cutout");

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
                simpleBlockItem(deferredBlock.get(), model);
        }

}
