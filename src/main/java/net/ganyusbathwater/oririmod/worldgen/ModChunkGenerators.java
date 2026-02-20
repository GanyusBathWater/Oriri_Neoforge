package net.ganyusbathwater.oririmod.worldgen;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for custom ChunkGenerator and BiomeSource codecs.
 */
public class ModChunkGenerators {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister
            .create(Registries.CHUNK_GENERATOR, OririMod.MOD_ID);

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister
            .create(Registries.BIOME_SOURCE, OririMod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<ElderwoodsChunkGenerator>> ELDERWOODS = CHUNK_GENERATORS
            .register("elderwoods", () -> ElderwoodsChunkGenerator.CODEC);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<ElderwoodsBiomeSource>> ELDERWOODS_BIOME_SOURCE = BIOME_SOURCES
            .register("elderwoods", () -> ElderwoodsBiomeSource.CODEC);

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
    }
}
