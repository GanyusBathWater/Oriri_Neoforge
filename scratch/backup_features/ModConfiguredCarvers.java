package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CarverDebugSettings;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;

public class ModConfiguredCarvers {
        public static final ResourceKey<ConfiguredWorldCarver<?>> SCARLET_CAVE_ENTRANCE = registerKey(
                        "scarlet_cave_entrance");

        public static final ResourceKey<ConfiguredWorldCarver<?>> ELYSIAN_ABYSS_CARVER_KEY = registerKey(
                        "elysian_abyss_carver");

        public static void bootstrap(BootstrapContext<ConfiguredWorldCarver<?>> context) {
                context.register(SCARLET_CAVE_ENTRANCE, new ConfiguredWorldCarver<>(
                                ModCarvers.SCARLET_CAVE_ENTRANCE.get(),
                                new CaveCarverConfiguration(
                                                0.15f,
                                                ConstantHeight.of(VerticalAnchor.aboveBottom(10)),
                                                ConstantFloat.of(1.0f),
                                                VerticalAnchor.aboveBottom(8),
                                                CarverDebugSettings.of(false, Blocks.AIR.defaultBlockState()),
                                                context.lookup(Registries.BLOCK)
                                                                .getOrThrow(BlockTags.OVERWORLD_CARVER_REPLACEABLES),
                                                ConstantFloat.of(1.0f),
                                                ConstantFloat.of(1.0f),
                                                ConstantFloat.of(-1.0f)
                                )));

                // Elysian Abyss carver – high probability so it fires in every chunk
                // of the biome. The carver itself handles interior logic.
                context.register(ELYSIAN_ABYSS_CARVER_KEY, new ConfiguredWorldCarver<>(
                                ModCarvers.ELYSIAN_ABYSS_CARVER.get(),
                                new CaveCarverConfiguration(
                                                0.9f, // very high probability
                                                ConstantHeight.of(VerticalAnchor.absolute(-90)),
                                                ConstantFloat.of(1.0f),
                                                VerticalAnchor.absolute(-100),
                                                CarverDebugSettings.of(false, Blocks.AIR.defaultBlockState()),
                                                context.lookup(Registries.BLOCK)
                                                                .getOrThrow(BlockTags.OVERWORLD_CARVER_REPLACEABLES),
                                                ConstantFloat.of(8.0f),  // wide horizontal
                                                ConstantFloat.of(1.0f),
                                                ConstantFloat.of(-1.0f)
                                )));
        }

        public static ResourceKey<ConfiguredWorldCarver<?>> registerKey(String name) {
                return ResourceKey.create(Registries.CONFIGURED_CARVER,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }
}
