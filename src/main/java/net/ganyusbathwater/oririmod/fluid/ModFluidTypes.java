package net.ganyusbathwater.oririmod.fluid;

import net.ganyusbathwater.oririmod.OririMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluidTypes {
        private ModFluidTypes() {
        }

        public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister
                        .create(NeoForgeRegistries.FLUID_TYPES, OririMod.MOD_ID);

        public static final DeferredHolder<FluidType, FluidType> AETHER_TYPE = FLUID_TYPES.register("aether",
                        () -> new FluidType(FluidType.Properties.create()
                                        .lightLevel(15)
                                        .viscosity(6000)
                                        .density(3000)
                                        .canExtinguish(false)
                                        .supportsBoating(false)
                                        .canDrown(false)
                                        .canSwim(false)
                                        .canPushEntity(false)
                                        .canHydrate(false)
                                        .pathType(null)));

        public static final DeferredHolder<FluidType, FluidType> BLOOD_WATER_TYPE = FLUID_TYPES.register("blood_water",
                        () -> new FluidType(FluidType.Properties.create()
                                        .lightLevel(0)
                                        .viscosity(1000) // Lowered to water-like to allow jumping out of 1-block depth
                                        .density(1000)
                                        .canExtinguish(true)
                                        .supportsBoating(true)
                                        .canDrown(true) // Can drown in it
                                        .canSwim(true) // Can swim in it
                                        .motionScale(0.0) // No longer pushes the player aggressively, but doesn't break swimming
                                        .canHydrate(false)
                                        .pathType(null)));

        public static void register(IEventBus bus) {
                FLUID_TYPES.register(bus);
        }
}
