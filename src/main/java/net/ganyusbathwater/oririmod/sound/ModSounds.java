package net.ganyusbathwater.oririmod.sound;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT,
            OririMod.MOD_ID);

    public static final Supplier<SoundEvent> DOOM_CLOCK_TICK = registerSound("doom_clock_tick");
    public static final Supplier<SoundEvent> DOOM_CLOCK_GONG = registerSound("doom_clock_gong");

    private static Supplier<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent
                .createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
