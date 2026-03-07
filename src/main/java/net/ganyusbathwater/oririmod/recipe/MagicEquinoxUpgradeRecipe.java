package net.ganyusbathwater.oririmod.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;

import net.minecraft.world.level.Level;

public class MagicEquinoxUpgradeRecipe extends EquinoxTableRecipe {

    private final int requiredLevel;

    public MagicEquinoxUpgradeRecipe(Ingredient top, Ingredient left, Ingredient center,
            Ingredient right, Ingredient bottom, Ingredient template,
            ItemStack result, int manaCost, int requiredLevel) {
        super(top, left, center, right, bottom, template, result, manaCost);
        this.requiredLevel = requiredLevel;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (!super.matches(input, level))
            return false;

        ItemStack centerItem = input.getItem(2);
        CustomData data = centerItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        int currentLevel = tag.contains("oriri_level") ? tag.getInt("oriri_level") : 1;

        return currentLevel == this.requiredLevel;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        // RecipeInput item order: 0=top, 1=left, 2=center, 3=right, 4=bottom,
        // 5=template
        ItemStack centerItem = input.getItem(2);

        ItemStack resultStack = centerItem.copy();
        resultStack.setCount(1);

        CustomData data = resultStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();

        tag.putInt("oriri_level", this.requiredLevel + 1);

        resultStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        return resultStack;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack staticResult = super.getResultItem(registries);
        CustomData data = staticResult.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        tag.putInt("oriri_level", this.requiredLevel + 1);
        staticResult.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return staticResult;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.MAGIC_EQUINOX_UPGRADE.get();
    }
}
