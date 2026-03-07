package net.ganyusbathwater.oririmod.recipe;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, OririMod.MOD_ID);

    public static final Supplier<RecipeSerializer<EquinoxTableRecipe>> EQUINOX_TABLE = RECIPE_SERIALIZERS.register(
            "equinox_table",
            EquinoxTableRecipeSerializer::new);

    public static final Supplier<RecipeSerializer<MagicEquinoxUpgradeRecipe>> MAGIC_EQUINOX_UPGRADE = RECIPE_SERIALIZERS
            .register(
                    "magic_equinox_upgrade",
                    MagicEquinoxUpgradeRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
