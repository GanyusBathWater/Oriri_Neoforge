package net.ganyusbathwater.oririmod.particle;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister
            .create(Registries.PARTICLE_TYPE, OririMod.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHINING_PARTICLES = PARTICLE_TYPES
            .register("shining", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELDERWOODS_CAVE_PARTICLE = PARTICLE_TYPES
            .register("elderwoods_cave_particle", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SCARLET_CAVE_PARTICLE = PARTICLE_TYPES
            .register("scarlet_cave_particle", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}