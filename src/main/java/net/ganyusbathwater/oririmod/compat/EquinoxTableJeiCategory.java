package net.ganyusbathwater.oririmod.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EquinoxTableJeiCategory implements IRecipeCategory<EquinoxTableRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "equinox_table");
    public static final RecipeType<EquinoxTableRecipe> TYPE = new RecipeType<>(UID, EquinoxTableRecipe.class);
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/equinox_table.png");

    private final IDrawable background;
    private final IDrawable icon;

    public EquinoxTableJeiCategory(IGuiHelper helper) {
        // Draw the main part of the GUI: x=5, y=5, width=158, height=72
        this.background = helper.createDrawable(TEXTURE, 5, 5, 158, 72);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.EQUINOX_TABLE.get()));
    }

    @Override
    public @NotNull RecipeType<EquinoxTableRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("block.oririmod.equinox_table");
    }

    @Override
    @SuppressWarnings("removal")
    public @NotNull IDrawable getBackground() {
        return this.background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EquinoxTableRecipe recipe, IFocusGroup focuses) {
        // Subtract 6 from original screen coordinates because the background starts at
        // x=5, y=5 on the texture, plus 1px offset

        // Input: Top
        builder.addSlot(RecipeIngredientRole.INPUT, 35 - 6, 14 - 6).addIngredients(recipe.getTop());
        // Input: Left
        builder.addSlot(RecipeIngredientRole.INPUT, 17 - 6, 32 - 6).addIngredients(recipe.getLeft());
        // Input: Center
        builder.addSlot(RecipeIngredientRole.INPUT, 35 - 6, 32 - 6).addIngredients(recipe.getCenter());
        // Input: Right
        builder.addSlot(RecipeIngredientRole.INPUT, 53 - 6, 32 - 6).addIngredients(recipe.getRight());
        // Input: Bottom
        builder.addSlot(RecipeIngredientRole.INPUT, 35 - 6, 50 - 6).addIngredients(recipe.getBottom());

        // Template
        builder.addSlot(RecipeIngredientRole.INPUT, 89 - 6, 17 - 6).addIngredients(recipe.getTemplate());

        // Output
        builder.addSlot(RecipeIngredientRole.OUTPUT, 141 - 6, 32 - 6).addItemStack(recipe.getResultItem(null));
    }

    @Override
    public void draw(EquinoxTableRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Draw mana cost at GHOST_X (89), GHOST_Y (48) but shifted by -6 for local GUI
        // relative space
        if (recipe.getManaCost() > 0) {
            String text = String.valueOf(recipe.getManaCost());
            // Magic color (example: a teal/blue color)
            int color = 0x00FFFF;
            Minecraft minecraft = Minecraft.getInstance();

            // Draw centered
            int width = minecraft.font.width(text);
            guiGraphics.drawString(minecraft.font, text, (89 - 6) + 8 - width / 2, (48 - 6) + 4, color, false);
        }
    }
}
