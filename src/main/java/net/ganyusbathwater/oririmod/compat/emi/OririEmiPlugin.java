package net.ganyusbathwater.oririmod.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.ganyusbathwater.oririmod.recipe.ModRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

@EmiEntrypoint
public class OririEmiPlugin implements EmiPlugin {
    
    public static final EmiRecipeCategory EQUINOX_TABLE_CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("oririmod", "equinox_table"),
            EmiStack.of(ModBlocks.EQUINOX_TABLE.get())
    );

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(EQUINOX_TABLE_CATEGORY);
        registry.addWorkstation(EQUINOX_TABLE_CATEGORY, EmiStack.of(ModBlocks.EQUINOX_TABLE.get()));

        for (RecipeHolder<EquinoxTableRecipe> recipe : registry.getRecipeManager().getAllRecipesFor(ModRecipeTypes.EQUINOX_TABLE.get())) {
            registry.addRecipe(new EquinoxTableEmiRecipe(recipe.id(), recipe.value()));
        }

        // No workaround needed for speed upgrades anymore since it uses Sugar
    }
}
