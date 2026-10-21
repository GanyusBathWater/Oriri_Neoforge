package net.ganyusbathwater.oririmod.recipe;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class LamiaDyeRecipe extends CustomRecipe {

    public LamiaDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        // Strictly single-color dyeing: exactly 1 Lamia Scale and exactly 1 Dye Item
        if (input.ingredientCount() != 2) {
            return false;
        }

        boolean hasScale = false;
        boolean hasDye = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.LAMIA_SCALE.get())) {
                    if (hasScale) return false;
                    hasScale = true;
                } else if (stack.getItem() instanceof DyeItem) {
                    if (hasDye) return false;
                    hasDye = true;
                } else {
                    return false;
                }
            }
        }

        return hasScale && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack scaleStack = ItemStack.EMPTY;
        DyeItem dyeItem = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.LAMIA_SCALE.get())) {
                    scaleStack = stack;
                } else if (stack.getItem() instanceof DyeItem dye) {
                    dyeItem = dye;
                }
            }
        }

        if (scaleStack.isEmpty() || dyeItem == null) {
            return ItemStack.EMPTY;
        }

        int dyeRgb = dyeItem.getDyeColor().getTextureDiffuseColor();
        ItemStack result = scaleStack.copyWithCount(1);
        result.set(DataComponents.DYED_COLOR, new DyedItemColor(dyeRgb, true));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.LAMIA_DYE.get();
    }
}
