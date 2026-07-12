package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.minecraft.tags.ItemTags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
        public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
                super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput recipeOutput) {
                /*
                 * ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRIT_GLOVE,1)
                 * .pattern("BAB")
                 * .pattern("AXA")
                 * .pattern("BAB")
                 * .define('B', Items.LEATHER)
                 * .define('X', ModItems.TORTURED_SOUL)
                 * .define('A', Items.RED_WOOL)
                 * .unlockedBy("has_tortured_soul",
                 * has(ModItems.TORTURED_SOUL)).save(recipeOutput);
                 * 
                 * 
                 */
                /*
                 * ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEART_OF_THE_TANK,1)
                 * .pattern("BBB")
                 * .pattern("BAB")
                 * .pattern("BBB")
                 * .define('B', Items.GOLDEN_APPLE)
                 * .define('A', ModItems.HOLLOW_SOUL)
                 * .unlockedBy("has_hollowed_soul",
                 * has(ModItems.HOLLOW_SOUL)).save(recipeOutput);
                 * 
                 * 
                 * 
                 * ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SNOW_BOOTS,1)
                 * .pattern("B  ")
                 * .pattern("ABS")
                 * .pattern("SSS")
                 * .define('B', Items.LEATHER)
                 * .define('A', ModItems.DAMNED_SOUL)
                 * .define('S', ModItems.IRON_STICK)
                 * .unlockedBy("has_damned_soul", has(ModItems.DAMNED_SOUL)).save(recipeOutput);
                 * 
                 * 
                 */
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IRON_STICK, 4)
                                .pattern("A")
                                .pattern("A")
                                .define('A', Items.IRON_BARS)
                                .unlockedBy("has_iron_bars", has(Items.IRON_BARS)).save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FLUORITE_BLOCK, 1)
                                .pattern("AAA")
                                .pattern("AAA")
                                .pattern("AAA")
                                .define('A', ModItems.FLUORITE_CRYSTAL)
                                .unlockedBy("has_fluorite_crystal", has(ModItems.FLUORITE_CRYSTAL)).save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.JADE_BLOCK, 1)
                                .pattern("AAA")
                                .pattern("AAA")
                                .pattern("AAA")
                                .define('A', ModItems.JADE)
                                .unlockedBy("has_jade", has(ModItems.JADE)).save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.JADE_SHIELD.get(), 1)
                                .pattern("WJW")
                                .pattern("WWW")
                                .pattern(" W ")
                                .define('W', ModItems.GILDED_NETHERRITE_INGOT.get())
                                .define('J', ModBlocks.JADE_BLOCK.get())
                                .unlockedBy("has_jade_block", has(ModBlocks.JADE_BLOCK.get())).save(recipeOutput);

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.JADE, 9)
                                .requires(ModBlocks.JADE_BLOCK)
                                .unlockedBy("has_jade_block", has(ModBlocks.JADE_BLOCK)).save(recipeOutput);

                stairBuilder(ModBlocks.JADE_STAIRS.get(), Ingredient.of(ModBlocks.JADE_BLOCK))
                                .unlockedBy("has_jade_block", has(ModBlocks.JADE_BLOCK)).save(recipeOutput);
                slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.JADE_SLAB.get(),
                                ModBlocks.JADE_BLOCK.get());
                wallBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.JADE_WALL.get(),
                                Ingredient.of(ModBlocks.JADE_BLOCK))
                                .unlockedBy("has_jade_block", has(ModBlocks.JADE_BLOCK)).save(recipeOutput);

                stairBuilder(ModBlocks.ELDER_STAIRS.get(), Ingredient.of(ModBlocks.ELDER_PLANKS))
                                .unlockedBy("has_elder_planks", has(ModBlocks.ELDER_PLANKS)).save(recipeOutput);
                slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.ELDER_SLAB.get(), ModBlocks.ELDER_PLANKS);
                fenceBuilder(ModBlocks.ELDER_FENCE.get(), Ingredient.of(ModBlocks.ELDER_PLANKS))
                                .unlockedBy("has_elder_planks", has(ModBlocks.ELDER_PLANKS)).save(recipeOutput);
                fenceGateBuilder(ModBlocks.ELDER_GATE.get(), Ingredient.of(ModBlocks.ELDER_PLANKS))
                                .unlockedBy("has_elder_planks", has(ModBlocks.ELDER_PLANKS)).save(recipeOutput);

                stairBuilder(ModBlocks.SCARLET_STAIRS.get(), Ingredient.of(ModBlocks.SCARLET_PLANKS))
                                .unlockedBy("has_scarlet_planks", has(ModBlocks.SCARLET_PLANKS)).save(recipeOutput);
                slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_SLAB.get(),
                                ModBlocks.SCARLET_PLANKS);
                fenceBuilder(ModBlocks.SCARLET_FENCE.get(), Ingredient.of(ModBlocks.SCARLET_PLANKS))
                                .unlockedBy("has_scarlet_planks", has(ModBlocks.SCARLET_PLANKS)).save(recipeOutput);
                fenceGateBuilder(ModBlocks.SCARLET_GATE.get(), Ingredient.of(ModBlocks.SCARLET_PLANKS))
                                .unlockedBy("has_scarlet_planks", has(ModBlocks.SCARLET_PLANKS)).save(recipeOutput);

                // Abyss Crown
                stairBuilder(ModBlocks.ABYSS_CROWN_STAIRS.get(), Ingredient.of(ModBlocks.ABYSS_CROWN_PLANKS))
                                .unlockedBy("has_abyss_crown_planks", has(ModBlocks.ABYSS_CROWN_PLANKS))
                                .save(recipeOutput);
                slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_SLAB.get(),
                                ModBlocks.ABYSS_CROWN_PLANKS);
                fenceBuilder(ModBlocks.ABYSS_CROWN_FENCE.get(), Ingredient.of(ModBlocks.ABYSS_CROWN_PLANKS))
                                .unlockedBy("has_abyss_crown_planks", has(ModBlocks.ABYSS_CROWN_PLANKS))
                                .save(recipeOutput);
                fenceGateBuilder(ModBlocks.ABYSS_CROWN_GATE.get(), Ingredient.of(ModBlocks.ABYSS_CROWN_PLANKS))
                                .unlockedBy("has_abyss_crown_planks", has(ModBlocks.ABYSS_CROWN_PLANKS))
                                .save(recipeOutput);

                makeWoodRecipes(recipeOutput, ModBlocks.ELDER_PLANKS.get(), ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get(), ModBlocks.ELDER_BUTTON.get(), ModBlocks.ELDER_PRESSURE_PLATE.get(), ModBlocks.ELDER_DOOR.get(), ModBlocks.ELDER_TRAPDOOR.get(), ModItems.ELDER_SIGN.get(), ModItems.ELDER_HANGING_SIGN.get(), ModItems.ELDER_BOAT.get(), ModItems.ELDER_CHEST_BOAT.get(), "elder");
                makeWoodRecipes(recipeOutput, ModBlocks.SCARLET_PLANKS.get(), ModBlocks.STRIPPED_SCARLET_LOG.get(), ModBlocks.SCARLET_BUTTON.get(), ModBlocks.SCARLET_PRESSURE_PLATE.get(), ModBlocks.SCARLET_DOOR.get(), ModBlocks.SCARLET_TRAPDOOR.get(), ModItems.SCARLET_SIGN.get(), ModItems.SCARLET_HANGING_SIGN.get(), ModItems.SCARLET_BOAT.get(), ModItems.SCARLET_CHEST_BOAT.get(), "scarlet");
                makeWoodRecipes(recipeOutput, ModBlocks.ABYSS_CROWN_PLANKS.get(), ModBlocks.STRIPPED_ABYSS_CROWN_LOG.get(), ModBlocks.ABYSS_CROWN_BUTTON.get(), ModBlocks.ABYSS_CROWN_PRESSURE_PLATE.get(), ModBlocks.ABYSS_CROWN_DOOR.get(), ModBlocks.ABYSS_CROWN_TRAPDOOR.get(), ModItems.ABYSS_CROWN_SIGN.get(), ModItems.ABYSS_CROWN_HANGING_SIGN.get(), ModItems.ABYSS_CROWN_BOAT.get(), ModItems.ABYSS_CROWN_CHEST_BOAT.get(), "abyss_crown");

                makeStoneRecipes(recipeOutput, "scarlet_cobblestone", ModBlocks.SCARLET_COBBLESTONE.get(), ModBlocks.SCARLET_COBBLESTONE_STAIRS.get(), ModBlocks.SCARLET_COBBLESTONE_SLAB.get(), ModBlocks.SCARLET_COBBLESTONE_WALL.get());
                makeStoneRecipes(recipeOutput, "mossy_scarlet_cobblestone", ModBlocks.MOSSY_SCARLET_COBBLESTONE.get(), ModBlocks.MOSSY_SCARLET_COBBLESTONE_STAIRS.get(), ModBlocks.MOSSY_SCARLET_COBBLESTONE_SLAB.get(), ModBlocks.MOSSY_SCARLET_COBBLESTONE_WALL.get());
                makeStoneRecipes(recipeOutput, "scarlet_stone_bricks", ModBlocks.SCARLET_STONE_BRICKS.get(), ModBlocks.SCARLET_STONE_BRICK_STAIRS.get(), ModBlocks.SCARLET_STONE_BRICK_SLAB.get(), ModBlocks.SCARLET_STONE_BRICK_WALL.get());
                makeStoneRecipes(recipeOutput, "mossy_scarlet_stone_bricks", ModBlocks.MOSSY_SCARLET_STONE_BRICKS.get(), ModBlocks.MOSSY_SCARLET_STONE_BRICK_STAIRS.get(), ModBlocks.MOSSY_SCARLET_STONE_BRICK_SLAB.get(), ModBlocks.MOSSY_SCARLET_STONE_BRICK_WALL.get());
                makeStoneRecipes(recipeOutput, "scarlet_deepslate", ModBlocks.SCARLET_DEEPSLATE.get(), ModBlocks.SCARLET_DEEPSLATE_STAIRS.get(), ModBlocks.SCARLET_DEEPSLATE_SLAB.get(), ModBlocks.SCARLET_DEEPSLATE_WALL.get());
                makeStoneRecipes(recipeOutput, "cobbled_scarlet_deepslate", ModBlocks.COBBLED_SCARLET_DEEPSLATE.get(), ModBlocks.COBBLED_SCARLET_DEEPSLATE_STAIRS.get(), ModBlocks.COBBLED_SCARLET_DEEPSLATE_SLAB.get(), ModBlocks.COBBLED_SCARLET_DEEPSLATE_WALL.get());
                makeStoneRecipes(recipeOutput, "polished_scarlet_deepslate", ModBlocks.POLISHED_SCARLET_DEEPSLATE.get(), ModBlocks.POLISHED_SCARLET_DEEPSLATE_STAIRS.get(), ModBlocks.POLISHED_SCARLET_DEEPSLATE_SLAB.get(), ModBlocks.POLISHED_SCARLET_DEEPSLATE_WALL.get());
                makeStoneRecipes(recipeOutput, "scarlet_deepslate_bricks", ModBlocks.SCARLET_DEEPSLATE_BRICKS.get(), ModBlocks.SCARLET_DEEPSLATE_BRICK_STAIRS.get(), ModBlocks.SCARLET_DEEPSLATE_BRICK_SLAB.get(), ModBlocks.SCARLET_DEEPSLATE_BRICK_WALL.get());
                makeStoneRecipes(recipeOutput, "scarlet_deepslate_tiles", ModBlocks.SCARLET_DEEPSLATE_TILES.get(), ModBlocks.SCARLET_DEEPSLATE_TILE_STAIRS.get(), ModBlocks.SCARLET_DEEPSLATE_TILE_SLAB.get(), ModBlocks.SCARLET_DEEPSLATE_TILE_WALL.get());

                // --- Planks from Logs/Stems ---
                // Elder
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ELDER_PLANKS.get(), 4)
                                .requires(ModBlocks.ELDER_LOG_BLOCK.get())
                                .unlockedBy("has_elder_log", has(ModBlocks.ELDER_LOG_BLOCK.get()))
                                .save(recipeOutput, "oririmod:elder_planks_from_log");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ELDER_PLANKS.get(), 4)
                                .requires(ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get())
                                .unlockedBy("has_stripped_elder_log", has(ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get()))
                                .save(recipeOutput, "oririmod:elder_planks_from_stripped_log");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ELDER_PLANKS.get(), 4)
                                .requires(ModBlocks.ELDER_STEM_BLOCK.get())
                                .unlockedBy("has_elder_stem", has(ModBlocks.ELDER_STEM_BLOCK.get()))
                                .save(recipeOutput, "oririmod:elder_planks_from_stem");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ELDER_PLANKS.get(), 4)
                                .requires(ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get())
                                .unlockedBy("has_stripped_elder_stem", has(ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get()))
                                .save(recipeOutput, "oririmod:elder_planks_from_stripped_stem");

                // Scarlet
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_PLANKS.get(), 4)
                                .requires(ModBlocks.SCARLET_LOG.get())
                                .unlockedBy("has_scarlet_log", has(ModBlocks.SCARLET_LOG.get()))
                                .save(recipeOutput, "oririmod:scarlet_planks_from_log");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_PLANKS.get(), 4)
                                .requires(ModBlocks.STRIPPED_SCARLET_LOG.get())
                                .unlockedBy("has_stripped_scarlet_log", has(ModBlocks.STRIPPED_SCARLET_LOG.get()))
                                .save(recipeOutput, "oririmod:scarlet_planks_from_stripped_log");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_PLANKS.get(), 4)
                                .requires(ModBlocks.SCARLET_STEM.get())
                                .unlockedBy("has_scarlet_stem", has(ModBlocks.SCARLET_STEM.get()))
                                .save(recipeOutput, "oririmod:scarlet_planks_from_stem");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_PLANKS.get(), 4)
                                .requires(ModBlocks.STRIPPED_SCARLET_STEM.get())
                                .unlockedBy("has_stripped_scarlet_stem", has(ModBlocks.STRIPPED_SCARLET_STEM.get()))
                                .save(recipeOutput, "oririmod:scarlet_planks_from_stripped_stem");

                // Abyss Crown
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_PLANKS.get(), 4)
                                .requires(ModBlocks.ABYSS_CROWN_LOG.get())
                                .unlockedBy("has_abyss_crown_log", has(ModBlocks.ABYSS_CROWN_LOG.get()))
                                .save(recipeOutput, "oririmod:abyss_crown_planks_from_log");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_PLANKS.get(), 4)
                                .requires(ModBlocks.STRIPPED_ABYSS_CROWN_LOG.get())
                                .unlockedBy("has_stripped_abyss_crown_log",
                                                has(ModBlocks.STRIPPED_ABYSS_CROWN_LOG.get()))
                                .save(recipeOutput, "oririmod:abyss_crown_planks_from_stripped_log");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_PLANKS.get(), 4)
                                .requires(ModBlocks.ABYSS_CROWN_STEM.get())
                                .unlockedBy("has_abyss_crown_stem", has(ModBlocks.ABYSS_CROWN_STEM.get()))
                                .save(recipeOutput, "oririmod:abyss_crown_planks_from_stem");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_PLANKS.get(), 4)
                                .requires(ModBlocks.STRIPPED_ABYSS_CROWN_STEM.get())
                                .unlockedBy("has_stripped_abyss_crown_stem",
                                                has(ModBlocks.STRIPPED_ABYSS_CROWN_STEM.get()))
                                .save(recipeOutput, "oririmod:abyss_crown_planks_from_stripped_stem");

                // --- Scarlet Stone Recipes ---
                // Scarlet Stone from Cobblestone
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModBlocks.SCARLET_COBBLESTONE), RecipeCategory.BUILDING_BLOCKS,
                                                ModBlocks.SCARLET_STONE.get(), 0.1f, 200)
                                .unlockedBy("has_scarlet_cobblestone", has(ModBlocks.SCARLET_COBBLESTONE))
                                .save(recipeOutput, "oririmod:scarlet_stone_from_smelting");

                // Scarlet Stone Bricks
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_STONE_BRICKS, 4)
                                .pattern("BB")
                                .pattern("BB")
                                .define('B', ModBlocks.SCARLET_STONE)
                                .unlockedBy("has_scarlet_stone", has(ModBlocks.SCARLET_STONE)).save(recipeOutput);

                // Chiseled Scarlet Stone Bricks
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_SCARLET_STONE_BRICKS, 1)
                                .pattern("B")
                                .pattern("B")
                                .define('B', ModBlocks.SCARLET_STONE_BRICKS)
                                .unlockedBy("has_scarlet_stone_bricks", has(ModBlocks.SCARLET_STONE_BRICKS))
                                .save(recipeOutput);

                // Cracked Scarlet Stone Bricks
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModBlocks.SCARLET_STONE_BRICKS), RecipeCategory.BUILDING_BLOCKS,
                                                ModBlocks.CRACKED_SCARLET_STONE_BRICKS.get(), 0.1f, 200)
                                .unlockedBy("has_scarlet_stone_bricks", has(ModBlocks.SCARLET_STONE_BRICKS))
                                .save(recipeOutput, "oririmod:cracked_scarlet_stone_bricks_from_smelting");

                // Mossy Scarlet Stone Bricks
                ShapelessRecipeBuilder
                                .shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MOSSY_SCARLET_STONE_BRICKS, 1)
                                .requires(ModBlocks.SCARLET_STONE_BRICKS)
                                .requires(ModBlocks.SCARLET_VINE)
                                .unlockedBy("has_scarlet_vine", has(ModBlocks.SCARLET_VINE)).save(recipeOutput);

                // Smooth Scarlet Stone
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModBlocks.SCARLET_STONE), RecipeCategory.BUILDING_BLOCKS,
                                                ModBlocks.SMOOTH_SCARLET_STONE.get(), 0.1f, 200)
                                .unlockedBy("has_scarlet_stone", has(ModBlocks.SCARLET_STONE))
                                .save(recipeOutput, "oririmod:smooth_scarlet_stone_from_smelting");

                // Smooth Scarlet Stone Slab
                slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SMOOTH_SCARLET_STONE_SLAB.get(),
                                ModBlocks.SMOOTH_SCARLET_STONE.get());

                // --- Scarlet Deepslate Recipes ---
                // Scarlet Deepslate from Cobbled Scarlet Deepslate
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModBlocks.COBBLED_SCARLET_DEEPSLATE), RecipeCategory.BUILDING_BLOCKS,
                                                ModBlocks.SCARLET_DEEPSLATE.get(), 0.1f, 200)
                                .unlockedBy("has_cobbled_scarlet_deepslate", has(ModBlocks.COBBLED_SCARLET_DEEPSLATE))
                                .save(recipeOutput, "oririmod:scarlet_deepslate_from_smelting");

                // Cobbled Scarlet Deepslate -> Polished Scarlet Deepslate
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.POLISHED_SCARLET_DEEPSLATE, 4)
                                .pattern("BB")
                                .pattern("BB")
                                .define('B', ModBlocks.COBBLED_SCARLET_DEEPSLATE)
                                .unlockedBy("has_cobbled_scarlet_deepslate", has(ModBlocks.COBBLED_SCARLET_DEEPSLATE))
                                .save(recipeOutput);

                // Polished Scarlet Deepslate -> Scarlet Deepslate Bricks
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_DEEPSLATE_BRICKS, 4)
                                .pattern("BB")
                                .pattern("BB")
                                .define('B', ModBlocks.POLISHED_SCARLET_DEEPSLATE)
                                .unlockedBy("has_polished_scarlet_deepslate", has(ModBlocks.POLISHED_SCARLET_DEEPSLATE))
                                .save(recipeOutput);

                // Scarlet Deepslate Bricks -> Scarlet Deepslate Tiles
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCARLET_DEEPSLATE_TILES, 4)
                                .pattern("BB")
                                .pattern("BB")
                                .define('B', ModBlocks.SCARLET_DEEPSLATE_BRICKS)
                                .unlockedBy("has_scarlet_deepslate_bricks", has(ModBlocks.SCARLET_DEEPSLATE_BRICKS))
                                .save(recipeOutput);

                // Chiseled Scarlet Deepslate
                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_SCARLET_DEEPSLATE, 1)
                                .pattern("B")
                                .pattern("B")
                                .define('B', ModBlocks.POLISHED_SCARLET_DEEPSLATE)
                                .unlockedBy("has_polished_scarlet_deepslate", has(ModBlocks.POLISHED_SCARLET_DEEPSLATE))
                                .save(recipeOutput);

                // Cracked Scarlet Deepslate Bricks
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModBlocks.SCARLET_DEEPSLATE_BRICKS),
                                                RecipeCategory.BUILDING_BLOCKS,
                                                ModBlocks.CRACKED_SCARLET_DEEPSLATE_BRICKS.get(), 0.1f, 200)
                                .unlockedBy("has_scarlet_deepslate_bricks", has(ModBlocks.SCARLET_DEEPSLATE_BRICKS))
                                .save(recipeOutput, "oririmod:cracked_scarlet_deepslate_bricks_from_smelting");

                // Cracked Scarlet Deepslate Tiles
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModBlocks.SCARLET_DEEPSLATE_TILES),
                                                RecipeCategory.BUILDING_BLOCKS,
                                                ModBlocks.CRACKED_SCARLET_DEEPSLATE_TILES.get(), 0.1f, 200)
                                .unlockedBy("has_scarlet_deepslate_tiles", has(ModBlocks.SCARLET_DEEPSLATE_TILES))
                                .save(recipeOutput, "oririmod:cracked_scarlet_deepslate_tiles_from_smelting");

                // Dragon Iron Ingot
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModItems.RAW_DRAGON_IRON), RecipeCategory.MISC,
                                                ModItems.DRAGON_IRON_INGOT.get(), 0.1f, 200)
                                .unlockedBy("has_raw_dragon_iron", has(ModItems.RAW_DRAGON_IRON))
                                .save(recipeOutput, "oririmod:dragon_iron_ingot_from_smelting");

                // --- Sol Sand Recipes ---
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModBlocks.SOL_SAND), RecipeCategory.BUILDING_BLOCKS,
                                                ModBlocks.SOL_GLASS.get(), 0.1f, 200)
                                .unlockedBy("has_sol_sand", has(ModBlocks.SOL_SAND))
                                .save(recipeOutput, "oririmod:sol_glass_from_smelting");

                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.SOL_GLASS_PANE.get(), 16)
                                .pattern("XXX")
                                .pattern("XXX")
                                .define('X', ModBlocks.SOL_GLASS)
                                .unlockedBy("has_sol_glass", has(ModBlocks.SOL_GLASS))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SOL_SANDSTONE, 1)
                                .pattern("BB")
                                .pattern("BB")
                                .define('B', ModBlocks.SOL_SAND)
                                .unlockedBy("has_sol_sand", has(ModBlocks.SOL_SAND)).save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_SOL_SANDSTONE, 4)
                                .pattern("BB")
                                .pattern("BB")
                                .define('B', ModBlocks.SOL_SANDSTONE)
                                .unlockedBy("has_sol_sandstone", has(ModBlocks.SOL_SANDSTONE)).save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CHISELED_SOL_SANDSTONE, 1)
                                .pattern("B")
                                .pattern("B")
                                .define('B', ModBlocks.SOL_SANDSTONE)
                                .unlockedBy("has_sol_sandstone", has(ModBlocks.SOL_SANDSTONE)).save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REVIVAL_SHRINE, 1)
                                .pattern("AAA")
                                .pattern("ABA")
                                .pattern("CCC")
                                .define('A', Blocks.WHITE_WOOL)
                                .define('B', ModItems.HOLLOW_SOUL)
                                .define('C', Blocks.GOLD_BLOCK)
                                .unlockedBy("has_hollow_soul", has(ModItems.HOLLOW_SOUL)).save(recipeOutput);

                // --- Sword Conversions ---
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.TILTED_BROKEN_SWORD_BLOCK, 1)
                                .requires(ModBlocks.BROKEN_SWORD_BLOCK)
                                .unlockedBy("has_broken_sword_block", has(ModBlocks.BROKEN_SWORD_BLOCK))
                                .save(recipeOutput);

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.BROKEN_SWORD_BLOCK, 1)
                                .requires(ModBlocks.TILTED_BROKEN_SWORD_BLOCK)
                                .unlockedBy("has_tilted_broken_sword_block", has(ModBlocks.TILTED_BROKEN_SWORD_BLOCK))
                                .save(recipeOutput, "oririmod:broken_sword_block_from_tilted");

                // --- Blood Sludge Conversions ---
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.SLIME_BALL, 4)
                                .requires(ModBlocks.BLOOD_SLUDGE.get())
                                .unlockedBy("has_blood_sludge", has(ModBlocks.BLOOD_SLUDGE.get()))
                                .save(recipeOutput, "oririmod:slime_balls_from_blood_sludge");

                /*
                 * ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BISMUTH.get(),
                 * 9)
                 * .requires(ModBlocks.BISMUTH_BLOCK)
                 * .unlockedBy("has_bismuth_block",
                 * has(ModBlocks.BISMUTH_BLOCK)).save(recipeOutput);
                 * 
                 * ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BISMUTH.get(),
                 * 18)
                 * .requires(ModBlocks.MAGIC_BLOCK)
                 * .unlockedBy("has_magic_block", has(ModBlocks.MAGIC_BLOCK))
                 * .save(recipeOutput, "oririmod:bismuth_from_magic_block");
                 * 
                 */
                makeArmorRecipes(recipeOutput, ModItems.PRISMARINE_IRON_INGOT.get(), ModItems.PRISMARINE_HELMET.get(),
                                ModItems.PRISMARINE_CHESTPLATE.get(), ModItems.PRISMARINE_LEGGINGS.get(),
                                ModItems.PRISMARINE_BOOTS.get());
                makeArmorRecipes(recipeOutput, ModItems.MOLTEN_INGOT.get(), ModItems.MOLTEN_HELMET.get(),
                                ModItems.MOLTEN_CHESTPLATE.get(), ModItems.MOLTEN_LEGGINGS.get(),
                                ModItems.MOLTEN_BOOTS.get());
                makeArmorRecipes(recipeOutput, ModItems.BLUE_ICE_INGOT.get(), ModItems.BLUE_ICE_HELMET.get(),
                                ModItems.BLUE_ICE_CHESTPLATE.get(), ModItems.BLUE_ICE_LEGGINGS.get(),
                                ModItems.BLUE_ICE_BOOTS.get());
                makeArmorRecipes(recipeOutput, ModItems.GILDED_NETHERRITE_INGOT.get(),
                                ModItems.GILDED_NETHERRITE_HELMET.get(), ModItems.GILDED_NETHERRITE_CHESTPLATE.get(),
                                ModItems.GILDED_NETHERRITE_LEGGINGS.get(), ModItems.GILDED_NETHERRITE_BOOTS.get());
                makeArmorRecipes(recipeOutput, ModItems.ANCIENT_INGOT.get(), ModItems.ANCIENT_HELMET.get(),
                                ModItems.ANCIENT_CHESTPLATE.get(), ModItems.ANCIENT_LEGGINGS.get(),
                                ModItems.ANCIENT_BOOTS.get());
                makeArmorRecipes(recipeOutput, ModItems.CRYSTAL_INGOT.get(), ModItems.CRYSTAL_HELMET.get(),
                                ModItems.CRYSTAL_CHESTPLATE.get(), ModItems.CRYSTAL_LEGGINGS.get(),
                                ModItems.CRYSTAL_BOOTS.get());

                makeSwordRecipe(recipeOutput, ModItems.PRISMARINE_IRON_INGOT.get(), ModItems.PIRATE_SABER.get());
                makeSwordRecipe(recipeOutput, ModItems.MOLTEN_INGOT.get(), ModItems.SOLS_EMBRACE.get());
                makeSwordRecipe(recipeOutput, ModItems.BLUE_ICE_INGOT.get(), ModItems.ICE_SWORD.get());
                makeSwordRecipe(recipeOutput, ModItems.ANCIENT_INGOT.get(), ModItems.PANDORAS_BLADE.get());

                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.ORAPHIM_BOW.get(), 1)
                                .pattern(" IS")
                                .pattern("I S")
                                .pattern(" IS")
                                .define('I', ModItems.CRYSTAL_INGOT.get())
                                .define('S', Items.STRING)
                                .unlockedBy("has_crystal_ingot", has(ModItems.CRYSTAL_INGOT.get())).save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MAGIC_UPGRADE_TEMPLATE.get(), 2)
                                .pattern("ITI")
                                .pattern("ISI")
                                .pattern("III")
                                .define('S', ModBlocks.SCARLET_STONE.get())
                                .define('I', ModItems.MANA_MANIFESTATION.get())
                                .define('T', ModItems.MAGIC_UPGRADE_TEMPLATE.get())
                                .unlockedBy("has_magic_upgrade_template", has(ModItems.MAGIC_UPGRADE_TEMPLATE.get()))
                                .save(recipeOutput);

                makeScytheRecipe(recipeOutput, Items.OAK_PLANKS, ModItems.WOOD_SCYTHE.get());
                makeScytheRecipe(recipeOutput, Items.COBBLESTONE, ModItems.STONE_SCYTHE.get());
                makeScytheRecipe(recipeOutput, Items.IRON_INGOT, ModItems.IRON_SCYTHE.get());
                makeScytheRecipe(recipeOutput, Items.GOLD_INGOT, ModItems.GOLD_SCYTHE.get());
                makeScytheRecipe(recipeOutput, Items.DIAMOND, ModItems.DIAMOND_SCYTHE.get());

                SmithingTransformRecipeBuilder.smithing(
                                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                                Ingredient.of(ModItems.DIAMOND_SCYTHE.get()),
                                Ingredient.of(Items.NETHERITE_INGOT),
                                RecipeCategory.COMBAT,
                                ModItems.NETHERITE_SCYTHE.get())
                        .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                        .save(recipeOutput, "oririmod:netherite_scythe_smithing");

                makeScytheRecipe(recipeOutput, ModItems.ANCIENT_INGOT.get(), ModItems.ANCIENT_SCYTHE.get());
                makeScytheRecipe(recipeOutput, ModItems.BLUE_ICE_INGOT.get(), ModItems.BLACK_ICE_SCYTHE.get());
                makeScytheRecipe(recipeOutput, ModItems.CRYSTAL_INGOT.get(), ModItems.CRYSTAL_SCYTHE.get());
                makeScytheRecipe(recipeOutput, ModItems.GILDED_NETHERRITE_INGOT.get(), ModItems.GILDED_NETHERITE_SCYTHE.get());
                makeScytheRecipe(recipeOutput, ModItems.MOLTEN_INGOT.get(), ModItems.MOLTEN_SCYTHE.get());
                makeScytheRecipe(recipeOutput, ModItems.PRISMARINE_IRON_INGOT.get(), ModItems.PRISMARINE_SCYTHE.get());

                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.EQUINOX_TABLE.get())
                                .pattern(" Z ")
                                .pattern("YXY")
                                .define('X', Items.ENCHANTING_TABLE)
                                .define('Y', Items.NETHERITE_INGOT)
                                .define('Z', ModItems.MANA_MANIFESTATION.get())
                                .unlockedBy("has_mana_manifestation", has(ModItems.MANA_MANIFESTATION.get()))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.THE_HARBINGER.get())
                                .pattern("AIB")
                                .pattern("CDE")
                                .pattern("FHG")
                                .define('A', ModItems.NETHERITE_SCYTHE.get())
                                .define('B', ModItems.ANCIENT_SCYTHE.get())
                                .define('C', ModItems.BLACK_ICE_SCYTHE.get())
                                .define('D', ModItems.CRYSTAL_SCYTHE.get())
                                .define('E', ModItems.GILDED_NETHERITE_SCYTHE.get())
                                .define('F', ModItems.MOLTEN_SCYTHE.get())
                                .define('G', ModItems.PRISMARINE_SCYTHE.get())
                                .define('H', ModItems.MANA_MANIFESTATION.get())
                                .define('I', ModItems.POWER_SOUL.get())
                                .unlockedBy("has_mana_manifestation", has(ModItems.MANA_MANIFESTATION.get()))
                                .save(recipeOutput);

                ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.MANA_IGNITER.get())
                                .requires(Items.IRON_INGOT)
                                .requires(ModItems.MANA_MANIFESTATION.get())
                                .unlockedBy("has_mana_manifestation", has(ModItems.MANA_MANIFESTATION.get()))
                                .save(recipeOutput);

                makeCustomArrow(recipeOutput, Items.TNT, ModItems.TNT_ARROW.get());
                makeCustomArrow(recipeOutput, ModItems.DRAGON_IRON_INGOT.get(), ModItems.DRAGON_IRON_ARROW.get());
                makeCustomArrow(recipeOutput, ModItems.BLUE_ICE_INGOT.get(), ModItems.FROST_ARROW.get());
                makeCustomArrow(recipeOutput, Items.COPPER_INGOT, ModItems.COPPER_ARROW.get());
                makeCustomArrow(recipeOutput, Items.ECHO_SHARD, ModItems.SONIC_ARROW.get());

                ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, ModItems.STAFF_OF_ALMIGHTY.get())
                                .requires(ModItems.STAFF_OF_FOREST.get())
                                .requires(ModItems.STAFF_OF_EARTH.get())
                                .requires(ModItems.STAFF_OF_WISE.get())
                                .requires(ModItems.STAFF_OF_VOID.get())
                                .requires(ModItems.STAFF_OF_COSMOS.get())
                                .requires(ModItems.ONE_THOUSAND_SCREAMS.get())
                                .requires(ModItems.STAFF_OF_HELL.get())
                                .requires(ModItems.BOOK_OF_WISE.get())
                                .requires(ModItems.IRAS_SOUL_FRAGMENT.get())
                                .unlockedBy("has_iras_soul", has(ModItems.IRAS_SOUL_FRAGMENT.get()))
                                .save(recipeOutput);

                makeStaff(recipeOutput, Ingredient.of(Items.STICK), Ingredient.of(ItemTags.SAPLINGS), ModItems.STAFF_OF_FOREST.get());
                makeStaff(recipeOutput, Ingredient.of(ModItems.IRON_STICK.get()), Ingredient.of(Items.GOLD_INGOT), ModItems.STAFF_OF_EARTH.get());
                makeStaff(recipeOutput, Ingredient.of(ModItems.IRON_STICK.get()), Ingredient.of(ModItems.JADE.get()), ModItems.STAFF_OF_WISE.get());
                makeStaff(recipeOutput, Ingredient.of(ModItems.IRON_STICK.get()), Ingredient.of(Items.ENDER_PEARL), ModItems.STAFF_OF_VOID.get());
                makeStaff(recipeOutput, Ingredient.of(ModItems.IRON_STICK.get()), Ingredient.of(ModItems.MOON_STONE.get()), ModItems.STAFF_OF_COSMOS.get());
                makeStaff(recipeOutput, Ingredient.of(Items.STICK), Ingredient.of(ModItems.FIRE_CRYSTAL.get()), ModItems.STAFF_OF_HELL.get());
        }

        private void makeArmorRecipes(RecipeOutput recipeOutput, Item ingot, Item helmet, Item chestplate,
                        Item leggings, Item boots) {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, helmet, 1)
                                .pattern("III")
                                .pattern("I I")
                                .define('I', ingot)
                                .unlockedBy("has_ingot", has(ingot)).save(recipeOutput);
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, chestplate, 1)
                                .pattern("I I")
                                .pattern("III")
                                .pattern("III")
                                .define('I', ingot)
                                .unlockedBy("has_ingot", has(ingot)).save(recipeOutput);
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, leggings, 1)
                                .pattern("III")
                                .pattern("I I")
                                .pattern("I I")
                                .define('I', ingot)
                                .unlockedBy("has_ingot", has(ingot)).save(recipeOutput);
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, boots, 1)
                                .pattern("I I")
                                .pattern("I I")
                                .define('I', ingot)
                                .unlockedBy("has_ingot", has(ingot)).save(recipeOutput);
        }

        private void makeSwordRecipe(RecipeOutput recipeOutput, Item ingot, Item sword) {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, sword, 1)
                                .pattern("I")
                                .pattern("I")
                                .pattern("S")
                                .define('I', ingot)
                                .define('S', Items.STICK)
                                .unlockedBy("has_ingot", has(ingot)).save(recipeOutput);
        }

        private void makeScytheRecipe(RecipeOutput recipeOutput, Item ingot, Item scythe) {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, scythe, 1)
                                .pattern("III")
                                .pattern("I S")
                                .pattern("  S")
                                .define('I', ingot)
                                .define('S', Items.STICK)
                                .unlockedBy("has_ingot", has(ingot)).save(recipeOutput);
        }

        private void makeWoodRecipes(RecipeOutput recipeOutput, Block planks, Block strippedLog, Block button, Block pressurePlate, Block door, Block trapdoor, Item sign, Item hangingSign, Item boat, Item chestBoat, String woodName) {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, button)
                                .requires(planks)
                                .unlockedBy("has_" + woodName + "_planks", has(planks))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, pressurePlate, 1)
                                .pattern("PP")
                                .define('P', planks)
                                .unlockedBy("has_" + woodName + "_planks", has(planks))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, door, 3)
                                .pattern("PP")
                                .pattern("PP")
                                .pattern("PP")
                                .define('P', planks)
                                .unlockedBy("has_" + woodName + "_planks", has(planks))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, trapdoor, 2)
                                .pattern("PPP")
                                .pattern("PPP")
                                .define('P', planks)
                                .unlockedBy("has_" + woodName + "_planks", has(planks))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, sign, 3)
                                .pattern("PPP")
                                .pattern("PPP")
                                .pattern(" S ")
                                .define('P', planks)
                                .define('S', Items.STICK)
                                .unlockedBy("has_" + woodName + "_planks", has(planks))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, hangingSign, 6)
                                .pattern("C C")
                                .pattern("LLL")
                                .pattern("LLL")
                                .define('C', Items.CHAIN)
                                .define('L', strippedLog)
                                .unlockedBy("has_stripped_" + woodName + "_log", has(strippedLog))
                                .save(recipeOutput);

                ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, boat, 1)
                                .pattern("P P")
                                .pattern("PPP")
                                .define('P', planks)
                                .unlockedBy("has_" + woodName + "_planks", has(planks))
                                .save(recipeOutput);

                ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, chestBoat, 1)
                                .requires(boat)
                                .requires(Items.CHEST)
                                .unlockedBy("has_" + woodName + "_boat", has(boat))
                                .save(recipeOutput);
        }

        private void makeStoneRecipes(RecipeOutput recipeOutput, String name, Block baseBlock, Block stairs, Block slab, Block wall) {
                stairBuilder(stairs, Ingredient.of(baseBlock))
                                .unlockedBy("has_" + name, has(baseBlock)).save(recipeOutput);
                slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, slab, baseBlock);
                if (wall != null) {
                        wallBuilder(RecipeCategory.BUILDING_BLOCKS, wall, Ingredient.of(baseBlock))
                                        .unlockedBy("has_" + name, has(baseBlock)).save(recipeOutput);
                }
        }

        private void makeCustomArrow(RecipeOutput recipeOutput, net.minecraft.world.level.ItemLike tip, Item result) {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result, 4)
                                .pattern("F")
                                .pattern("S")
                                .pattern("E")
                                .define('F', tip)
                                .define('S', Items.STICK)
                                .define('E', Items.FEATHER)
                                .unlockedBy("has_tip", has(tip))
                                .save(recipeOutput);
        }

        private void makeStaff(RecipeOutput recipeOutput, Ingredient stick, Ingredient head, Item result) {
                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result, 1)
                                .pattern("  Z")
                                .pattern(" Y ")
                                .pattern("Y  ")
                                .define('Y', stick)
                                .define('Z', head)
                                .unlockedBy("has_stick", has(Items.STICK))
                                .save(recipeOutput);
        }
}
