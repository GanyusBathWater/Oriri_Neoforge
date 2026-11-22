package net.ganyusbathwater.oririmod.client.render.world.haze;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps biomes to their atmospheric ground haze profiles.
 * Active in Scarlet Swamp, Elderwoods, Elder Beach, Vile Plains, and Elder Valley,
 * with intensity peaking at night and during/after rainfall.
 */
public final class GroundHazeRegistry {

    private static final Map<ResourceKey<Biome>, GroundHazeProfile> PROFILES = new HashMap<>();

    static {
        // Scarlet Swamp: thick moody crimson-rose swamp floor mist over blood water and bog ground
        register("scarlet_swamp", new GroundHazeProfile(
                0.76F, 0.30F, 0.34F,
                0.76F, 0.22F, 0.92F, 1.00F,
                0.14F, 1.12F, 0.032F, 0.011F));

        // Elderwoods: lush silvery-blue nocturnal woodland haze drifting between giant trees
        register("elderwoods", new GroundHazeProfile(
                0.70F, 0.84F, 0.95F,
                0.58F, 0.14F, 0.88F, 1.00F,
                0.14F, 1.06F, 0.028F, 0.010F));

        // Elder Beach: thick coastal sea-mist hugging sandbars and shoreline
        register("elder_beach", new GroundHazeProfile(
                0.84F, 0.92F, 0.96F,
                0.62F, 0.16F, 0.85F, 1.00F,
                0.14F, 1.02F, 0.026F, 0.013F));

        // Vile Plains: heavy twilight violet miasma creeping across low plains
        register("vile_plains", new GroundHazeProfile(
                0.58F, 0.36F, 0.74F,
                0.72F, 0.20F, 0.95F, 1.00F,
                0.16F, 1.16F, 0.034F, 0.012F));

        // Elder Valley: dense pale morning/night river mist hugging valley slopes and banks
        register("elder_valley", new GroundHazeProfile(
                0.85F, 0.93F, 0.90F,
                0.66F, 0.18F, 0.90F, 1.00F,
                0.14F, 1.10F, 0.030F, 0.011F));
    }

    private GroundHazeRegistry() {
    }

    private static void register(String biomePath, GroundHazeProfile profile) {
        PROFILES.put(
                ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, biomePath)),
                profile);
    }

    public static GroundHazeProfile getProfile(Holder<Biome> biome) {
        return biome.unwrapKey().map(PROFILES::get).orElse(GroundHazeProfile.NONE);
    }
}
