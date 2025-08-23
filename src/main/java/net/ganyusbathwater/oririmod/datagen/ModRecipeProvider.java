package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

        @Override
        protected void buildRecipes(RecipeOutput recipeOutput) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRIT_GLOVE,1)
                .pattern("BAB")
                .pattern("AXA")
                .pattern("BAB")
                .define('B', Items.LEATHER)
                .define('X', ModItems.TORTURED_SOUL)
                .define('A', Items.RED_WOOL)
                .unlockedBy("has_tortured_soul", has(ModItems.TORTURED_SOUL)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMERALD_SWORD,1)
                    .pattern("B")
                    .pattern("B")
                    .pattern("A")
                    .define('B', Items.EMERALD)
                    .define('A', Items.STICK)
                    .unlockedBy("has_emerald", has(Items.EMERALD)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMERALD_AXE,1)
                    .pattern("BB")
                    .pattern("BA")
                    .pattern(" A")
                    .define('B', Items.EMERALD)
                    .define('A', Items.STICK)
                    .unlockedBy("has_emerald", has(Items.EMERALD)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMERALD_PICKAXE,1)
                    .pattern("BBB")
                    .pattern(" A ")
                    .pattern(" A ")
                    .define('B', Items.EMERALD)
                    .define('A', Items.STICK)
                    .unlockedBy("has_emerald", has(Items.EMERALD)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMERALD_SHOVEL,1)
                    .pattern("B")
                    .pattern("A")
                    .pattern("A")
                    .define('B', Items.EMERALD)
                    .define('A', Items.STICK)
                    .unlockedBy("has_emerald", has(Items.EMERALD)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMERALD_HOE,1)
                    .pattern("BB")
                    .pattern(" A")
                    .pattern(" A")
                    .define('B', Items.EMERALD)
                    .define('A', Items.STICK)
                    .unlockedBy("has_emerald", has(Items.EMERALD)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEART_OF_THE_TANK,1)
                    .pattern("BBB")
                    .pattern("BAB")
                    .pattern("BBB")
                    .define('B', Items.GOLDEN_APPLE)
                    .define('A', ModItems.HOLLOW_SOUL)
                    .unlockedBy("has_hollowed_soul", has(ModItems.HOLLOW_SOUL)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ICE_SKATES,1)
                    .pattern("B  ")
                    .pattern("ABS")
                    .pattern("SSS")
                    .define('B', Items.LEATHER)
                    .define('A', ModItems.DAMNED_SOUL)
                    .define('S', ModItems.IRON_STICK)
                    .unlockedBy("has_damned_soul", has(ModItems.DAMNED_SOUL)).save(recipeOutput);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IRON_STICK,4)
                    .pattern("A")
                    .pattern("A")
                    .define('A', Items.IRON_BARS)
                    .unlockedBy("has_iron_bars", has(Items.IRON_BARS)).save(recipeOutput);
        /*
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BISMUTH.get(), 9)
                .requires(ModBlocks.BISMUTH_BLOCK)
                .unlockedBy("has_bismuth_block", has(ModBlocks.BISMUTH_BLOCK)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BISMUTH.get(), 18)
                .requires(ModBlocks.MAGIC_BLOCK)
                .unlockedBy("has_magic_block", has(ModBlocks.MAGIC_BLOCK))
                .save(recipeOutput, "oririmod:bismuth_from_magic_block");

        */
    }
}
