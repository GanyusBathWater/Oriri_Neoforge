package net.ganyusbathwater.oririmod.potion;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(BuiltInRegistries.POTION, OririMod.MOD_ID);

    public static final Holder<Potion> STUNNED_POTION = POTIONS.register("stunned_potion", () -> new Potion(new MobEffectInstance(ModEffects.STUNNED_EFFECT, 1200, 0)));

    public static final Holder<Potion> BROKEN_POTION1 = POTIONS.register("broken_potion1", () -> new Potion(new MobEffectInstance(ModEffects.BROKEN_EFFECT, 1200, 0)));

    public static final Holder<Potion> BROKEN_POTION2 = POTIONS.register("broken_potion2", () -> new Potion(new MobEffectInstance(ModEffects.BROKEN_EFFECT, 1200, 1)));

    public static final Holder<Potion> BROKEN_POTION3 = POTIONS.register("broken_potion3", () -> new Potion(new MobEffectInstance(ModEffects.BROKEN_EFFECT, 1200, 2)));

    public static void registerPotions(IEventBus eventBus) {
        POTIONS.register(eventBus);
        OririMod.LOGGER.info("Registering Mod Potions for " + OririMod.MOD_ID);
    }
}
