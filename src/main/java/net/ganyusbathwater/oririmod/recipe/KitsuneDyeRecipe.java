package net.ganyusbathwater.oririmod.recipe;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.component.KitsuneColors;
import net.ganyusbathwater.oririmod.item.component.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class KitsuneDyeRecipe extends CustomRecipe {

    public KitsuneDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int ingredientCount = input.ingredientCount();
        if (ingredientCount < 2 || ingredientCount > 3) {
            return false;
        }

        int bellX = -1;
        int bellY = -1;
        int bellCount = 0;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (!stack.isEmpty()) {
                    if (stack.is(ModItems.KITSUNE_BELL.get())) {
                        bellX = x;
                        bellY = y;
                        bellCount++;
                    }
                }
            }
        }

        if (bellCount != 1 || bellX == -1 || bellY == -1) {
            return false;
        }

        boolean hasTopDye = false;
        if (bellY > 0) {
            ItemStack top = input.getItem(bellX, bellY - 1);
            if (!top.isEmpty() && top.getItem() instanceof DyeItem) {
                hasTopDye = true;
            }
        }

        boolean hasBottomDye = false;
        if (bellY < input.height() - 1) {
            ItemStack bottom = input.getItem(bellX, bellY + 1);
            if (!bottom.isEmpty() && bottom.getItem() instanceof DyeItem) {
                hasBottomDye = true;
            }
        }

        int expectedIngredients = 1 + (hasTopDye ? 1 : 0) + (hasBottomDye ? 1 : 0);
        return expectedIngredients == ingredientCount && (hasTopDye || hasBottomDye);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int bellX = -1;
        int bellY = -1;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.is(ModItems.KITSUNE_BELL.get())) {
                    bellX = x;
                    bellY = y;
                    break;
                }
            }
            if (bellX != -1) break;
        }

        if (bellX == -1 || bellY == -1) {
            return ItemStack.EMPTY;
        }

        ItemStack bellStack = input.getItem(bellX, bellY);
        KitsuneColors currentColors = bellStack.getOrDefault(ModDataComponents.KITSUNE_COLORS.get(), KitsuneColors.DEFAULT);
        int newPrimary = currentColors.primary();
        int newSecondary = currentColors.secondary();

        // Dye above -> Primary color (Fur)
        if (bellY > 0) {
            ItemStack top = input.getItem(bellX, bellY - 1);
            if (!top.isEmpty() && top.getItem() instanceof DyeItem dye) {
                newPrimary = dye.getDyeColor().getTextureDiffuseColor();
            }
        }

        // Dye below -> Secondary color (Tips)
        if (bellY < input.height() - 1) {
            ItemStack bottom = input.getItem(bellX, bellY + 1);
            if (!bottom.isEmpty() && bottom.getItem() instanceof DyeItem dye) {
                newSecondary = dye.getDyeColor().getTextureDiffuseColor();
            }
        }

        ItemStack result = bellStack.copyWithCount(1);
        result.set(ModDataComponents.KITSUNE_COLORS.get(), new KitsuneColors(newPrimary, newSecondary));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.KITSUNE_DYE.get();
    }
}
