package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
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



                blockWithItem(ModBlocks.MANA_CRYSTAL_BLOCK, 1);
                clusterBlockWithItem(ModBlocks.MANA_CRYSTAL_CLUSTER, 2);

                blockWithItem(ModBlocks.FLUORITE_BLOCK, 4);
                fluoriteClusterState(ModBlocks.FLUORITE_CLUSTER);

                blockWithItem(ModBlocks.ELDER_STEM_BLOCK, 1);
                // blockWithItem(ModBlocks.STRIPPED_ELDER_STEM_BLOCK, 1);
                // pillarBlockWithItem(ModBlocks.ELDER_LOG_BLOCK, 1);
                pillarBlockWithItem(ModBlocks.CRACKED_ELDER_LOG_BLOCK, 1);
                axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get()),
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                "block/stripped_elder_log_block_side"),
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                "block/stripped_elder_log_block_top_bottom"));
                blockWithItem(ModBlocks.ELDER_PLANKS, 1);
                axisBlock(((RotatedPillarBlock) ModBlocks.ELDER_LOG_BLOCK.get()),
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/elder_log_block_side"),
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                                                "block/elder_log_block_top_bottom"));
                blockWithItem(ModBlocks.STRIPPED_ELDER_STEM_BLOCK, 1);

                stairsBlock(ModBlocks.ELDER_STAIRS.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
                slabBlock(ModBlocks.ELDER_SLAB.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()),
                                blockTexture(ModBlocks.ELDER_PLANKS.get()));
                fenceBlock(ModBlocks.ELDER_FENCE.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
                fenceGateBlock(ModBlocks.ELDER_GATE.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));

                doorBlockWithRenderType((DoorBlock) ModBlocks.ELDER_DOOR.get(), modLoc("block/elder_door_bottom"), modLoc("block/elder_door_top"), "cutout");
                trapdoorBlockWithRenderType((TrapDoorBlock) ModBlocks.ELDER_TRAPDOOR.get(), modLoc("block/elder_trapdoor"), true, "cutout");

                buttonBlock((net.minecraft.world.level.block.ButtonBlock) ModBlocks.ELDER_BUTTON.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
                pressurePlateBlock((net.minecraft.world.level.block.PressurePlateBlock) ModBlocks.ELDER_PRESSURE_PLATE.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));

                signBlock((net.minecraft.world.level.block.StandingSignBlock) ModBlocks.ELDER_SIGN.get(), (net.minecraft.world.level.block.WallSignBlock) ModBlocks.ELDER_WALL_SIGN.get(), blockTexture(ModBlocks.ELDER_PLANKS.get()));
                hangingSignBlock(ModBlocks.ELDER_HANGING_SIGN.get(), ModBlocks.ELDER_WALL_HANGING_SIGN.get(), modLoc("block/elder_log_block_side"));

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

                doorBlockWithRenderType((DoorBlock) ModBlocks.SCARLET_DOOR.get(), modLoc("block/scarlet_door_bottom"), modLoc("block/scarlet_door_top"), "cutout");
                trapdoorBlockWithRenderType((TrapDoorBlock) ModBlocks.SCARLET_TRAPDOOR.get(), modLoc("block/scarlet_trapdoor"), true, "cutout");

                buttonBlock((net.minecraft.world.level.block.ButtonBlock) ModBlocks.SCARLET_BUTTON.get(), blockTexture(ModBlocks.SCARLET_PLANKS.get()));
                pressurePlateBlock((net.minecraft.world.level.block.PressurePlateBlock) ModBlocks.SCARLET_PRESSURE_PLATE.get(), blockTexture(ModBlocks.SCARLET_PLANKS.get()));

                signBlock((net.minecraft.world.level.block.StandingSignBlock) ModBlocks.SCARLET_SIGN.get(), (net.minecraft.world.level.block.WallSignBlock) ModBlocks.SCARLET_WALL_SIGN.get(), blockTexture(ModBlocks.SCARLET_PLANKS.get()));
                hangingSignBlock(ModBlocks.SCARLET_HANGING_SIGN.get(), ModBlocks.SCARLET_WALL_HANGING_SIGN.get(), modLoc("block/scarlet_log"));

                // --- NEW SCARLET STONE VARIANTS DATAGEN ---
                blockWithItem(ModBlocks.SCARLET_COBBLESTONE, 1);
                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.SCARLET_COBBLESTONE_STAIRS.get(), modLoc("block/scarlet_cobblestone"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.SCARLET_COBBLESTONE_SLAB.get(), modLoc("block/scarlet_cobblestone"), modLoc("block/scarlet_cobblestone"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.SCARLET_COBBLESTONE_WALL.get(), modLoc("block/scarlet_cobblestone"));

                blockWithItem(ModBlocks.MOSSY_SCARLET_COBBLESTONE, 1);
                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.MOSSY_SCARLET_COBBLESTONE_STAIRS.get(), modLoc("block/mossy_scarlet_cobblestone"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.MOSSY_SCARLET_COBBLESTONE_SLAB.get(), modLoc("block/mossy_scarlet_cobblestone"), modLoc("block/mossy_scarlet_cobblestone"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.MOSSY_SCARLET_COBBLESTONE_WALL.get(), modLoc("block/mossy_scarlet_cobblestone"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.SCARLET_STONE_STAIRS.get(), modLoc("block/scarlet_stone"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.SCARLET_STONE_SLAB.get(), modLoc("block/scarlet_stone"), modLoc("block/scarlet_stone"));
                buttonBlock((net.minecraft.world.level.block.ButtonBlock) ModBlocks.SCARLET_STONE_BUTTON.get(), modLoc("block/scarlet_stone"));
                pressurePlateBlock((net.minecraft.world.level.block.PressurePlateBlock) ModBlocks.SCARLET_STONE_PRESSURE_PLATE.get(), modLoc("block/scarlet_stone"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.SCARLET_STONE_BRICK_STAIRS.get(), modLoc("block/scarlet_stone_bricks"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.SCARLET_STONE_BRICK_SLAB.get(), modLoc("block/scarlet_stone_bricks"), modLoc("block/scarlet_stone_bricks"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.SCARLET_STONE_BRICK_WALL.get(), modLoc("block/scarlet_stone_bricks"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.MOSSY_SCARLET_STONE_BRICK_STAIRS.get(), modLoc("block/mossy_scarlet_stone_bricks"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.MOSSY_SCARLET_STONE_BRICK_SLAB.get(), modLoc("block/mossy_scarlet_stone_bricks"), modLoc("block/mossy_scarlet_stone_bricks"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.MOSSY_SCARLET_STONE_BRICK_WALL.get(), modLoc("block/mossy_scarlet_stone_bricks"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.COBBLED_SCARLET_DEEPSLATE_STAIRS.get(), modLoc("block/cobbled_scarlet_deepslate"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.COBBLED_SCARLET_DEEPSLATE_SLAB.get(), modLoc("block/cobbled_scarlet_deepslate"), modLoc("block/cobbled_scarlet_deepslate"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.COBBLED_SCARLET_DEEPSLATE_WALL.get(), modLoc("block/cobbled_scarlet_deepslate"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.POLISHED_SCARLET_DEEPSLATE_STAIRS.get(), modLoc("block/polished_scarlet_deepslate"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.POLISHED_SCARLET_DEEPSLATE_SLAB.get(), modLoc("block/polished_scarlet_deepslate"), modLoc("block/polished_scarlet_deepslate"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.POLISHED_SCARLET_DEEPSLATE_WALL.get(), modLoc("block/polished_scarlet_deepslate"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.SCARLET_DEEPSLATE_STAIRS.get(), modLoc("block/scarlet_deepslate"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.SCARLET_DEEPSLATE_SLAB.get(), modLoc("block/scarlet_deepslate"), modLoc("block/scarlet_deepslate"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.SCARLET_DEEPSLATE_WALL.get(), modLoc("block/scarlet_deepslate"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.SCARLET_DEEPSLATE_BRICK_STAIRS.get(), modLoc("block/scarlet_deepslate_bricks"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.SCARLET_DEEPSLATE_BRICK_SLAB.get(), modLoc("block/scarlet_deepslate_bricks"), modLoc("block/scarlet_deepslate_bricks"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.SCARLET_DEEPSLATE_BRICK_WALL.get(), modLoc("block/scarlet_deepslate_bricks"));

                stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.SCARLET_DEEPSLATE_TILE_STAIRS.get(), modLoc("block/scarlet_deepslate_tiles"));
                slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.SCARLET_DEEPSLATE_TILE_SLAB.get(), modLoc("block/scarlet_deepslate_tiles"), modLoc("block/scarlet_deepslate_tiles"));
                wallBlock((net.minecraft.world.level.block.WallBlock) ModBlocks.SCARLET_DEEPSLATE_TILE_WALL.get(), modLoc("block/scarlet_deepslate_tiles"));

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

                // Elderwoods Overgrowth

                simpleBlock(ModBlocks.HANGING_ELDER_MOSS.get(),
                                models().cross("hanging_elder_moss", modLoc("block/hanging_elder_moss"))
                                                .renderType("cutout"));

                simpleBlock(ModBlocks.HANGING_ELDER_MOSS_PLANT.get(),
                                models().cross("hanging_elder_moss_plant", modLoc("block/hanging_elder_moss_plant"))
                                                .renderType("cutout"));

                // ===== FORCEFIELD EMITTERS =====
                simpleBlock(ModBlocks.REPELLENT_FORCEFIELD_EMITTER.get(),
                                models().getBuilder("repellent_forcefield_emitter")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", modLoc("block/forcefield_emitter")));
                simpleBlock(ModBlocks.ATTRACTING_FORCEFIELD_EMITTER.get(),
                                models().getBuilder("attracting_forcefield_emitter")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", modLoc("block/forcefield_emitter")));
                simpleBlock(ModBlocks.PROTECTION_FORCEFIELD_EMITTER.get(),
                                models().getBuilder("protection_forcefield_emitter")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", modLoc("block/forcefield_emitter")));
                simpleBlock(ModBlocks.MODIFIER_FORCEFIELD_EMITTER.get(),
                                models().getBuilder("modifier_forcefield_emitter")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", modLoc("block/forcefield_emitter")));

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
                
                simpleBlock(ModBlocks.SOL_QUICKSAND.get(),
                                models().cubeAll("sol_quicksand", modLoc("block/sol_quicksand")));

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

                // ===== REVIVAL SHRINE =====
                horizontalBlock(ModBlocks.REVIVAL_SHRINE.get(),
                                models().getBuilder("revival_shrine")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", mcLoc("block/white_concrete")));

                // ===== GECKOLIB PLANTS =====
                simpleBlock(ModBlocks.BLOOD_CAP_BLOCK.get(),
                                models().getBuilder("blood_cap_block")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", mcLoc("block/red_concrete")));

                horizontalBlock(ModBlocks.MOONSHROOM_BLOCK.get(),
                                models().getBuilder("moonshroom_block")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", mcLoc("block/light_blue_concrete")));

                simpleBlock(ModBlocks.GLOWLINGS_BLOCK.get(),
                                models().getBuilder("glowlings_block")
                                        .parent(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile("minecraft:builtin/entity"))
                                        .texture("particle", mcLoc("block/lime_concrete")));

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

                paneBlockWithRenderType(
                                ((net.minecraft.world.level.block.IronBarsBlock) ModBlocks.SOL_GLASS_PANE.get()),
                                modLoc("block/sol_glass"),
                                modLoc("block/sol_glass_pane_top"), "minecraft:cutout");

                // ===== HARDENED MANASHROOM =====
                blockWithItem(ModBlocks.HARDENED_MANASHROOM, 1);

                // ===== ABYSS CROWN WOOD SET =====
                axisBlock(((RotatedPillarBlock) ModBlocks.ABYSS_CROWN_LOG.get()),
                                modLoc("block/abyss_crown_log"),
                                modLoc("block/abyss_crown_log_top"));
                axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_ABYSS_CROWN_LOG.get()),
                                modLoc("block/stripped_abyss_crown_log"),
                                modLoc("block/stripped_abyss_crown_log_top"));

                // Stems use log texture on all sides (standard for many mods)
                simpleBlockWithItem(ModBlocks.ABYSS_CROWN_STEM.get(),
                                models().cubeAll("abyss_crown_stem", modLoc("block/abyss_crown_log")));
                simpleBlockWithItem(ModBlocks.STRIPPED_ABYSS_CROWN_STEM.get(),
                                models().cubeAll("stripped_abyss_crown_stem",
                                                modLoc("block/stripped_abyss_crown_log")));

                blockWithItem(ModBlocks.ABYSS_CROWN_PLANKS, 1);
                stairsBlock(ModBlocks.ABYSS_CROWN_STAIRS.get(), blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()));
                slabBlock(ModBlocks.ABYSS_CROWN_SLAB.get(), blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()),
                                blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()));
                fenceBlock(ModBlocks.ABYSS_CROWN_FENCE.get(), blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()));
                fenceGateBlock(ModBlocks.ABYSS_CROWN_GATE.get(), blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()));

                doorBlockWithRenderType((DoorBlock) ModBlocks.ABYSS_CROWN_DOOR.get(), modLoc("block/abyss_crown_door_bottom"), modLoc("block/abyss_crown_door_top"), "cutout");
                trapdoorBlockWithRenderType((TrapDoorBlock) ModBlocks.ABYSS_CROWN_TRAPDOOR.get(), modLoc("block/abyss_crown_trapdoor"), true, "cutout");

                buttonBlock((net.minecraft.world.level.block.ButtonBlock) ModBlocks.ABYSS_CROWN_BUTTON.get(), blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()));
                pressurePlateBlock((net.minecraft.world.level.block.PressurePlateBlock) ModBlocks.ABYSS_CROWN_PRESSURE_PLATE.get(), blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()));

                signBlock((net.minecraft.world.level.block.StandingSignBlock) ModBlocks.ABYSS_CROWN_SIGN.get(), (net.minecraft.world.level.block.WallSignBlock) ModBlocks.ABYSS_CROWN_WALL_SIGN.get(), blockTexture(ModBlocks.ABYSS_CROWN_PLANKS.get()));
                hangingSignBlock(ModBlocks.ABYSS_CROWN_HANGING_SIGN.get(), ModBlocks.ABYSS_CROWN_WALL_HANGING_SIGN.get(), modLoc("block/abyss_crown_log"));
                blockWithItem(ModBlocks.ABYSS_CROWN_LEAVES, 3);
                
                simpleBlockWithItem(ModBlocks.ABYSS_CROWN_SAPLING.get(),
                                models().cross("abyss_crown_sapling", modLoc("block/abyss_crown_sapling"))
                                                .renderType("cutout"));

                simpleBlockWithItem(ModBlocks.UPGRADED_ABYSS_CROWN_SAPLING.get(),
                                models().cross("upgraded_abyss_crown_sapling", modLoc("block/abyss_crown_sapling"))
                                                .renderType("cutout"));

                // ===== AETHER BLOCKS =====
                simpleBlockWithItem(ModBlocks.AETHER_MAGMA_BLOCK.get(),
                                models().cubeAll("aether_magma_block", modLoc("block/aether_magma")));
                
                aetherFireBlock();

                generateScarletVineBlock();
        }

        private void aetherFireBlock() {
                BlockModelBuilder floor = models().withExistingParent("aether_fire_floor", mcLoc("block/template_fire_floor"))
                                .texture("fire", modLoc("block/aether_fire")).renderType("cutout");
                BlockModelBuilder side = models().withExistingParent("aether_fire_side", mcLoc("block/template_fire_side"))
                                .texture("fire", modLoc("block/aether_fire")).renderType("cutout");
                BlockModelBuilder sideAlt = models().withExistingParent("aether_fire_side_alt", mcLoc("block/template_fire_side_alt"))
                                .texture("fire", modLoc("block/aether_fire")).renderType("cutout");
                BlockModelBuilder up = models().withExistingParent("aether_fire_up", mcLoc("block/template_fire_up"))
                                .texture("fire", modLoc("block/aether_fire")).renderType("cutout");
                BlockModelBuilder upAlt = models().withExistingParent("aether_fire_up_alt", mcLoc("block/template_fire_up_alt"))
                                .texture("fire", modLoc("block/aether_fire")).renderType("cutout");

                net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder builder = getMultipartBuilder(ModBlocks.AETHER_FIRE_BLOCK.get());

                // Floor
                builder.part().modelFile(floor).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.UP, false).end();

                // North
                builder.part().modelFile(side).nextModel().modelFile(sideAlt).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH, true).end();
                builder.part().modelFile(side).nextModel().modelFile(sideAlt).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.UP, false).end();

                // East
                builder.part().modelFile(side).rotationY(90).nextModel().modelFile(sideAlt).rotationY(90).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, true).end();
                builder.part().modelFile(side).rotationY(90).nextModel().modelFile(sideAlt).rotationY(90).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.UP, false).end();

                // South
                builder.part().modelFile(side).rotationY(180).nextModel().modelFile(sideAlt).rotationY(180).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH, true).end();
                builder.part().modelFile(side).rotationY(180).nextModel().modelFile(sideAlt).rotationY(180).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.UP, false).end();

                // West
                builder.part().modelFile(side).rotationY(270).nextModel().modelFile(sideAlt).rotationY(270).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST, true).end();
                builder.part().modelFile(side).rotationY(270).nextModel().modelFile(sideAlt).rotationY(270).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST, false)
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.UP, false).end();

                // Up
                builder.part().modelFile(up).nextModel().modelFile(upAlt).addModel()
                        .condition(net.minecraft.world.level.block.state.properties.BlockStateProperties.UP, true).end();
        }

        private void generateScarletVineBlock() {
                ResourceLocation texture = modLoc("block/scarlet_vine");
                BlockModelBuilder model = models().withExistingParent("scarlet_vine", mcLoc("block/vine"))
                                .texture("vine", texture)
                                .texture("particle", texture)
                                .renderType("cutout");

                getMultipartBuilder(ModBlocks.SCARLET_VINE.get())
                                .part().modelFile(model).addModel().condition(BlockStateProperties.NORTH, true).end()
                                .part().modelFile(model).rotationY(90).addModel().condition(BlockStateProperties.EAST, true).end()
                                .part().modelFile(model).rotationY(180).addModel().condition(BlockStateProperties.SOUTH, true).end()
                                .part().modelFile(model).rotationY(270).addModel().condition(BlockStateProperties.WEST, true).end()
                                .part().modelFile(model).rotationX(270).addModel().condition(BlockStateProperties.UP, true).end();
                
                simpleBlockItem(ModBlocks.SCARLET_VINE.get(), model);
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

                BlockModelBuilder post = models()
                                .withExistingParent(name + "_post", mcLoc("block/template_glass_pane_post"))
                                .texture("pane", pane).texture("edge", edge).renderType(renderType);
                BlockModelBuilder side = models()
                                .withExistingParent(name + "_side", mcLoc("block/template_glass_pane_side"))
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
                builder.part().modelFile(nosideAlt).rotationY(90).addModel()
                                .condition(BlockStateProperties.SOUTH, false)
                                .end();

                builder.part().modelFile(sideAlt).rotationY(90).addModel().condition(BlockStateProperties.WEST, true)
                                .end();
                builder.part().modelFile(noside).rotationY(270).addModel().condition(BlockStateProperties.WEST, false)
                                .end();

                builder.part().modelFile(side).rotationY(90).addModel().condition(BlockStateProperties.EAST, true)
                                .end();
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
