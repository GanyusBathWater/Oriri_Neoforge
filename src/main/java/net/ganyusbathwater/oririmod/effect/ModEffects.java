package net.ganyusbathwater.oririmod.effect;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.MobSenseEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, OririMod.MOD_ID);

    public static final Holder<MobEffect> STUNNED_EFFECT = MOB_EFFECTS.register("stunned",
            () -> new StunnedEffect(MobEffectCategory.HARMFUL, 0x000000));

    public static final Holder<MobEffect> BROKEN_EFFECT = MOB_EFFECTS.register("broken",
            () -> new BrokenEffect(MobEffectCategory.HARMFUL, 0x000000));

    public static final Holder<MobEffect> CHARMED_EFFECT = MOB_EFFECTS.register("charmed",
            () -> new BrokenEffect(MobEffectCategory.HARMFUL, 0xFF69B4));

    public static final Holder<MobEffect> MOB_SENSE_EFFECT = MOB_EFFECTS.register("mob_sense",
            () -> new MobSenseEffect(MobEffectCategory.BENEFICIAL, 0xffffff));

    public static void registerEffects(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
        OririMod.LOGGER.info("Registering Mod Effects for " + OririMod.MOD_ID);
    }
}