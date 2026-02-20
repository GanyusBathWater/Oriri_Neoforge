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

        public static void bootstrap(BootstrapContext<ConfiguredWorldCarver<?>> context) {
                // Register the configured carver
                // We use available constructors. If 3 args (probability, height, replaceable)
                // or more.
                // CaveCarverConfiguration typical constructor in 1.20.x:
                // (probability, height, yScale, lavaLevel, debugSettings, replaceable)

                context.register(SCARLET_CAVE_ENTRANCE, new ConfiguredWorldCarver<>(
                                ModCarvers.SCARLET_CAVE_ENTRANCE.get(),
                                new CaveCarverConfiguration(
                                                0.15f, // probability
                                                ConstantHeight.of(VerticalAnchor.aboveBottom(10)), // height
                                                ConstantFloat.of(1.0f), // yScale
                                                VerticalAnchor.aboveBottom(8), // lavaLevel (VerticalAnchor)
                                                CarverDebugSettings.of(false, Blocks.AIR.defaultBlockState()), // debugSettings
                                                context.lookup(Registries.BLOCK)
                                                                .getOrThrow(BlockTags.OVERWORLD_CARVER_REPLACEABLES), // replaceable
                                                ConstantFloat.of(1.0f), // horizontalRadiusMultiplier
                                                ConstantFloat.of(1.0f), // verticalRadiusMultiplier
                                                ConstantFloat.of(-1.0f) // floorLevel
                                )));
        }

        public static ResourceKey<ConfiguredWorldCarver<?>> registerKey(String name) {
                return ResourceKey.create(Registries.CONFIGURED_CARVER,
                                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }
}
