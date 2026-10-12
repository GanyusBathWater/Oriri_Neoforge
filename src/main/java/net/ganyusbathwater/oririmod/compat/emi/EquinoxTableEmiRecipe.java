package net.ganyusbathwater.oririmod.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.ganyusbathwater.oririmod.recipe.MagicEquinoxUpgradeRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class EquinoxTableEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final EquinoxTableRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public EquinoxTableEmiRecipe(ResourceLocation id, EquinoxTableRecipe recipe) {
        this.id = id;
        this.recipe = recipe;

        EmiIngredient centerIngredient = EmiIngredient.of(recipe.getCenter());
        ItemStack resultStack = recipe.getResult().copy();

        if (recipe instanceof MagicEquinoxUpgradeRecipe magicUpgrade) {
            ItemStack[] centerItems = recipe.getCenter().getItems();
            if (centerItems != null && centerItems.length > 0) {
                List<EmiStack> centerStacks = new ArrayList<>();
                for (ItemStack base : centerItems) {
                    ItemStack copy = base.copy();
                    CompoundTag tag = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                    tag.putInt("oriri_level", magicUpgrade.getRequiredLevel());
                    copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    centerStacks.add(EmiStack.of(copy));
                }
                centerIngredient = EmiIngredient.of(centerStacks);
            }

            CompoundTag resTag = resultStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            resTag.putInt("oriri_level", magicUpgrade.getRequiredLevel() + 1);
            resultStack.set(DataComponents.CUSTOM_DATA, CustomData.of(resTag));
        }

        this.inputs = List.of(
            EmiIngredient.of(recipe.getTop()),
            EmiIngredient.of(recipe.getLeft()),
            centerIngredient,
            EmiIngredient.of(recipe.getRight()),
            EmiIngredient.of(recipe.getBottom()),
            EmiIngredient.of(recipe.getTemplate())
        );
        this.outputs = List.of(EmiStack.of(resultStack));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return OririEmiPlugin.EQUINOX_TABLE_CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 70; 
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 35, 14); // Top
        widgets.addSlot(inputs.get(1), 17, 32); // Left
        widgets.addSlot(inputs.get(2), 35, 32); // Center
        widgets.addSlot(inputs.get(3), 53, 32); // Right
        widgets.addSlot(inputs.get(4), 35, 50); // Bottom
        
        widgets.addSlot(inputs.get(5), 89, 17); // Template

        widgets.addSlot(outputs.get(0), 141, 32).recipeContext(this); // Output
        
        if (recipe.getManaCost() > 0) {
            widgets.addText(Component.literal("Mana: " + recipe.getManaCost()), 85, 52, 0x00AFFF, true);
        }
    }
}
