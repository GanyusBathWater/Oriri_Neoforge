package net.ganyusbathwater.oririmod.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.ganyusbathwater.oririmod.recipe.ModRecipeTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class JeiOririPlugin implements IModPlugin {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new EquinoxTableJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<net.minecraft.world.item.ItemStack> interpreter = new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(net.minecraft.world.item.ItemStack ingredient, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return net.ganyusbathwater.oririmod.item.custom.VestigeItem.getUnlockedLevel(ingredient);
            }
            @Override
            public String getLegacyStringSubtypeInfo(net.minecraft.world.item.ItemStack ingredient, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return String.valueOf(net.ganyusbathwater.oririmod.item.custom.VestigeItem.getUnlockedLevel(ingredient));
            }
        };

        net.minecraft.world.item.Item[] items = {
                net.ganyusbathwater.oririmod.item.ModItems.SOLIS_BROOCH.get(),
                net.ganyusbathwater.oririmod.item.ModItems.CANDY_BAG.get(),
                net.ganyusbathwater.oririmod.item.ModItems.STRIDER_SCALE.get(),
                net.ganyusbathwater.oririmod.item.ModItems.MAGMA_COOKING_BOOK.get(),
                net.ganyusbathwater.oririmod.item.ModItems.SNOW_BOOTS.get(),
                net.ganyusbathwater.oririmod.item.ModItems.STIGMA_OF_DARKNESS.get(),
                net.ganyusbathwater.oririmod.item.ModItems.IRON_GOLEM_MANUAL.get(),
                net.ganyusbathwater.oririmod.item.ModItems.BOUND_OF_THE_CELESTIAL_SISTERS.get(),
                net.ganyusbathwater.oririmod.item.ModItems.BLAZING_PYROMANIAC_GUIDE.get(),
                net.ganyusbathwater.oririmod.item.ModItems.HEART_OF_THE_TANK.get(),
                net.ganyusbathwater.oririmod.item.ModItems.SKELETON_ENCYCLOPEDIA.get(),
                net.ganyusbathwater.oririmod.item.ModItems.SLIMY_COOKING_BOOK.get(),
                net.ganyusbathwater.oririmod.item.ModItems.IVY_BOTANIC_GUIDE.get(),
                net.ganyusbathwater.oririmod.item.ModItems.STRANGE_ENDER_EYE.get(),
                net.ganyusbathwater.oririmod.item.ModItems.WITHER_ROSE.get(),
                net.ganyusbathwater.oririmod.item.ModItems.ZOMBIE_ENCYCLOPEDIA.get(),
                net.ganyusbathwater.oririmod.item.ModItems.MINERS_LANTERN.get(),
                net.ganyusbathwater.oririmod.item.ModItems.SPRING.get(),
                net.ganyusbathwater.oririmod.item.ModItems.CRIT_GLOVE.get(),
                net.ganyusbathwater.oririmod.item.ModItems.MIRROR_OF_THE_VOID.get(),
                net.ganyusbathwater.oririmod.item.ModItems.PHOENIX_FEATHER.get(),
                net.ganyusbathwater.oririmod.item.ModItems.RELIC_OF_THE_PAST.get(),
                net.ganyusbathwater.oririmod.item.ModItems.DUELLANT_CORTEX.get()
        };

        for (net.minecraft.world.item.Item item : items) {
            registration.registerSubtypeInterpreter(item, interpreter);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager rm = Minecraft.getInstance().level.getRecipeManager();
        List<EquinoxTableRecipe> recipes = rm.getAllRecipesFor(ModRecipeTypes.EQUINOX_TABLE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        registration.addRecipes(EquinoxTableJeiCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EQUINOX_TABLE.get()), EquinoxTableJeiCategory.TYPE);
    }
}
