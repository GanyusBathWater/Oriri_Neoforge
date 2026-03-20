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
                                .unlockedBy("has_abyss_crown_planks", has(ModBlocks.ABYSS_CROWN_PLANKS)).save(recipeOutput);
                slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_SLAB.get(),
                                ModBlocks.ABYSS_CROWN_PLANKS);
                fenceBuilder(ModBlocks.ABYSS_CROWN_FENCE.get(), Ingredient.of(ModBlocks.ABYSS_CROWN_PLANKS))
                                .unlockedBy("has_abyss_crown_planks", has(ModBlocks.ABYSS_CROWN_PLANKS)).save(recipeOutput);
                fenceGateBuilder(ModBlocks.ABYSS_CROWN_GATE.get(), Ingredient.of(ModBlocks.ABYSS_CROWN_PLANKS))
                                .unlockedBy("has_abyss_crown_planks", has(ModBlocks.ABYSS_CROWN_PLANKS)).save(recipeOutput);

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
                                .unlockedBy("has_stripped_abyss_crown_log", has(ModBlocks.STRIPPED_ABYSS_CROWN_LOG.get()))
                                .save(recipeOutput, "oririmod:abyss_crown_planks_from_stripped_log");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_PLANKS.get(), 4)
                                .requires(ModBlocks.ABYSS_CROWN_STEM.get())
                                .unlockedBy("has_abyss_crown_stem", has(ModBlocks.ABYSS_CROWN_STEM.get()))
                                .save(recipeOutput, "oririmod:abyss_crown_planks_from_stem");
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ABYSS_CROWN_PLANKS.get(), 4)
                                .requires(ModBlocks.STRIPPED_ABYSS_CROWN_STEM.get())
                                .unlockedBy("has_stripped_abyss_crown_stem", has(ModBlocks.STRIPPED_ABYSS_CROWN_STEM.get()))
                                .save(recipeOutput, "oririmod:abyss_crown_planks_from_stripped_stem");

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

                // Dragon Iron Ingot
                SimpleCookingRecipeBuilder
                                .smelting(Ingredient.of(ModItems.RAW_DRAGON_IRON), RecipeCategory.MISC,
                                                ModItems.DRAGON_IRON_INGOT.get(), 0.1f, 200)
                                .unlockedBy("has_raw_dragon_iron", has(ModItems.RAW_DRAGON_IRON))
                                .save(recipeOutput, "oririmod:dragon_iron_ingot_from_smelting");

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
}
