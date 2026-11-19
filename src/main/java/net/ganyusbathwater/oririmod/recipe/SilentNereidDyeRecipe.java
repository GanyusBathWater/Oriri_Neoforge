package net.ganyusbathwater.oririmod.recipe;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.component.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.ganyusbathwater.oririmod.item.component.SilentNereidColors;

public class SilentNereidDyeRecipe extends CustomRecipe {

    public SilentNereidDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int ingredientCount = input.ingredientCount();
        if (ingredientCount < 2 || ingredientCount > 3) {
            return false;
        }

        int scaleX = -1;
        int scaleY = -1;
        int scaleCount = 0;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (!stack.isEmpty()) {
                    if (stack.is(ModItems.SILENT_NEREID_SCALE.get())) {
                        scaleX = x;
                        scaleY = y;
                        scaleCount++;
                    }
                }
            }
        }

        if (scaleCount != 1 || scaleX == -1 || scaleY == -1) {
            return false;
        }

        boolean hasTopDye = false;
        if (scaleY > 0) {
            ItemStack top = input.getItem(scaleX, scaleY - 1);
            if (!top.isEmpty() && top.getItem() instanceof DyeItem) {
                hasTopDye = true;
            }
        }

        boolean hasBottomDye = false;
        if (scaleY < input.height() - 1) {
            ItemStack bottom = input.getItem(scaleX, scaleY + 1);
            if (!bottom.isEmpty() && bottom.getItem() instanceof DyeItem) {
                hasBottomDye = true;
            }
        }

        int expectedIngredients = 1 + (hasTopDye ? 1 : 0) + (hasBottomDye ? 1 : 0);
        return expectedIngredients == ingredientCount && (hasTopDye || hasBottomDye);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int scaleX = -1;
        int scaleY = -1;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (stack.is(ModItems.SILENT_NEREID_SCALE.get())) {
                    scaleX = x;
                    scaleY = y;
                    break;
                }
            }
            if (scaleX != -1) break;
        }

        if (scaleX == -1 || scaleY == -1) {
            return ItemStack.EMPTY;
        }

        ItemStack scaleStack = input.getItem(scaleX, scaleY);
        SilentNereidColors currentColors = scaleStack.getOrDefault(ModDataComponents.SILENT_NEREID_COLORS.get(), SilentNereidColors.DEFAULT);
        int newPrimary = currentColors.primary();
        int newSecondary = currentColors.secondary();

        // Dye above -> Primary color (Scales / Lure Rod)
        if (scaleY > 0) {
            ItemStack top = input.getItem(scaleX, scaleY - 1);
            if (!top.isEmpty() && top.getItem() instanceof DyeItem dye) {
                newPrimary = dye.getDyeColor().getTextureDiffuseColor();
            }
        }

        // Dye below -> Secondary color (Fins / Emissive Lure Bulb)
        if (scaleY < input.height() - 1) {
            ItemStack bottom = input.getItem(scaleX, scaleY + 1);
            if (!bottom.isEmpty() && bottom.getItem() instanceof DyeItem dye) {
                newSecondary = dye.getDyeColor().getTextureDiffuseColor();
            }
        }

        ItemStack result = scaleStack.copyWithCount(1);
        result.set(ModDataComponents.SILENT_NEREID_COLORS.get(), new SilentNereidColors(newPrimary, newSecondary));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SILENT_NEREID_DYE.get();
    }
}
