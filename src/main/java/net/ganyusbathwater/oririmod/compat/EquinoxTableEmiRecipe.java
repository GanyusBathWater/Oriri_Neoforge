package net.ganyusbathwater.oririmod.compat;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EquinoxTableEmiRecipe implements EmiRecipe {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/equinox_table.png");

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
                EmiIngredient.of(recipe.getTemplate()));

        this.outputs = List.of(EmiStack.of(recipe.getResultItem(null)));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return EmiOririPlugin.RECIPE_CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return this.inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return this.outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 158;
    }

    @Override
    public int getDisplayHeight() {
        return 72;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // Draw the main UI background, offsetting by 5 to match the exact cutout size
        // we used in JEI
        widgets.addTexture(new EmiTexture(TEXTURE, 5, 5, 158, 72), 0, 0);

        // Draw slots (using same coordinates as JEI minus 6)
        widgets.addSlot(EmiIngredient.of(recipe.getTop()), 35 - 6, 14 - 6).drawBack(false);
        widgets.addSlot(EmiIngredient.of(recipe.getLeft()), 17 - 6, 32 - 6).drawBack(false);
        widgets.addSlot(EmiIngredient.of(recipe.getCenter()), 35 - 6, 32 - 6).drawBack(false);
        widgets.addSlot(EmiIngredient.of(recipe.getRight()), 53 - 6, 32 - 6).drawBack(false);
        widgets.addSlot(EmiIngredient.of(recipe.getBottom()), 35 - 6, 50 - 6).drawBack(false);

        widgets.addSlot(EmiIngredient.of(recipe.getTemplate()), 89 - 6, 17 - 6).drawBack(false);

        widgets.addSlot(outputs.get(0), 141 - 6, 32 - 6).recipeContext(this).drawBack(false);

        // Draw Mana text if > 0
        if (recipe.getManaCost() > 0) {
            String text = String.valueOf(recipe.getManaCost());
            int color = 0x00FFFF;
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.font.width(text);

            widgets.addText(Component.literal(text), (89 - 6) + 8 - width / 2, (48 - 6) + 4, color, false);
        }
    }
}
