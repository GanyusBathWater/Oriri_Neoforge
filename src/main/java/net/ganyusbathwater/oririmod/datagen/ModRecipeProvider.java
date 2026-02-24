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
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;

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

                ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.JADE_SHIELD, 1)
                                .pattern("WJW")
                                .pattern("WWW")
                                .pattern(" W ")
                                .define('W', net.minecraft.tags.ItemTags.PLANKS)
                                .define('J', ModBlocks.JADE_BLOCK)
                                .unlockedBy("has_jade_block", has(ModBlocks.JADE_BLOCK)).save(recipeOutput);

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

                // --- Wood from Logs ---
                woodFromLogs(recipeOutput, ModBlocks.ELDER_LOG_BLOCK.get(), ModBlocks.ELDER_PLANKS.get());
                woodFromLogs(recipeOutput, ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get(), ModBlocks.ELDER_PLANKS.get());

                woodFromLogs(recipeOutput, ModBlocks.SCARLET_LOG.get(), ModBlocks.SCARLET_PLANKS.get());
                woodFromLogs(recipeOutput, ModBlocks.STRIPPED_SCARLET_LOG.get(), ModBlocks.SCARLET_PLANKS.get());
                woodFromLogs(recipeOutput, ModBlocks.SCARLET_STEM.get(), ModBlocks.SCARLET_PLANKS.get());
                woodFromLogs(recipeOutput, ModBlocks.STRIPPED_SCARLET_STEM.get(), ModBlocks.SCARLET_PLANKS.get());

                // --- Scarlet Stone Recipes ---
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

                // --- Sol Sand Recipes ---
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

                // --- Sword Conversions ---
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.TILTED_BROKEN_SWORD_BLOCK, 1)
                                .requires(ModBlocks.BROKEN_SWORD_BLOCK)
                                .unlockedBy("has_broken_sword_block", has(ModBlocks.BROKEN_SWORD_BLOCK))
                                .save(recipeOutput);

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.BROKEN_SWORD_BLOCK, 1)
                                .requires(ModBlocks.TILTED_BROKEN_SWORD_BLOCK)
                                .unlockedBy("has_tilted_broken_sword_block", has(ModBlocks.TILTED_BROKEN_SWORD_BLOCK))
                                .save(recipeOutput, "oririmod:broken_sword_block_from_tilted");

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
        }
}
