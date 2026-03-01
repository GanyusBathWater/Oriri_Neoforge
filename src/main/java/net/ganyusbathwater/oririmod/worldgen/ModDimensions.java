package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
        public static final ResourceKey<Level> ELDERWOODS_LEVEL_KEY = ResourceKey.create(
                        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("minecraft", "dimension")),
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"));
        public static final ResourceKey<DimensionType> ELDERWOODS_DIM_TYPE = ResourceKey.create(
                        Registries.DIMENSION_TYPE,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"));
}
