package net.ganyusbathwater.oririmod.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class EquinoxTableEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final EquinoxTableRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public EquinoxTableEmiRecipe(ResourceLocation id, EquinoxTableRecipe recipe) {
        this.id = id;
        this.recipe = recipe;
        this.inputs = List.of(
            EmiIngredient.of(recipe.getTop()),
            EmiIngredient.of(recipe.getLeft()),
            EmiIngredient.of(recipe.getCenter()),
            EmiIngredient.of(recipe.getRight()),
            EmiIngredient.of(recipe.getBottom()),
            EmiIngredient.of(recipe.getTemplate())
        );
        this.outputs = List.of(EmiStack.of(recipe.getResult()));
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
