package net.ganyusbathwater.oririmod.compat;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.ganyusbathwater.oririmod.recipe.ModRecipeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;

@EmiEntrypoint
public class EmiOririPlugin implements EmiPlugin {

    public static final ResourceLocation WORKSTATION_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "equinox_table");
    public static final EmiStack WORKSTATION = EmiStack.of(ModBlocks.EQUINOX_TABLE.get());
    public static final EmiRecipeCategory RECIPE_CATEGORY = new EmiRecipeCategory(WORKSTATION_ID, WORKSTATION);

    @Override
    public void register(EmiRegistry registry) {
        // Register the category and its workstation
        registry.addCategory(RECIPE_CATEGORY);
        registry.addWorkstation(RECIPE_CATEGORY, WORKSTATION);

        // Fetch recipes and register them
        RecipeManager rm = Minecraft.getInstance().level.getRecipeManager();
        for (RecipeHolder<EquinoxTableRecipe> holder : rm.getAllRecipesFor(ModRecipeTypes.EQUINOX_TABLE.get())) {
            registry.addRecipe(new EquinoxTableEmiRecipe(holder.id(), holder.value()));
        }
    }
}
