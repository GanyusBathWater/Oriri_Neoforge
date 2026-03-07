package net.ganyusbathwater.oririmod.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class EquinoxTableRecipe implements Recipe<RecipeInput> {
    private final Ingredient top;
    private final Ingredient left;
    private final Ingredient center;
    private final Ingredient right;
    private final Ingredient bottom;
    private final Ingredient template;
    private final ItemStack result;
    private final int manaCost;

    public EquinoxTableRecipe(Ingredient top, Ingredient left, Ingredient center,
            Ingredient right, Ingredient bottom, Ingredient template,
            ItemStack result, int manaCost) {
        this.top = top;
        this.left = left;
        this.center = center;
        this.right = right;
        this.bottom = bottom;
        this.template = template;
        this.result = result;
        this.manaCost = manaCost;
    }

    /**
     * Checks if the given input matches this recipe.
     * RecipeInput item order: 0=top, 1=left, 2=center, 3=right, 4=bottom,
     * 5=template
     */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (input.size() < 6)
            return false;
        return top.test(input.getItem(0))
                && left.test(input.getItem(1))
                && center.test(input.getItem(2))
                && right.test(input.getItem(3))
                && bottom.test(input.getItem(4))
                && template.test(input.getItem(5));
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(top);
        list.add(left);
        list.add(center);
        list.add(right);
        list.add(bottom);
        list.add(template);
        return list;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.EQUINOX_TABLE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.EQUINOX_TABLE.get();
    }

    // Getters
    public Ingredient getTop() {
        return top;
    }

    public Ingredient getLeft() {
        return left;
    }

    public Ingredient getCenter() {
        return center;
    }

    public Ingredient getRight() {
        return right;
    }

    public Ingredient getBottom() {
        return bottom;
    }

    public Ingredient getTemplate() {
        return template;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getManaCost() {
        return manaCost;
    }
}
