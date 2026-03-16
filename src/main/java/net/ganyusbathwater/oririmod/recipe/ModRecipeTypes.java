package net.ganyusbathwater.oririmod.recipe;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE,
            OririMod.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EquinoxTableRecipe>> EQUINOX_TABLE = RECIPE_TYPES
            .register("equinox_table",
                    () -> new RecipeType<EquinoxTableRecipe>() {
                        @Override
                        public String toString() {
                            return "oririmod:equinox_table";
                        }
                    });

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
