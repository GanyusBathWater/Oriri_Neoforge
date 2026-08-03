package net.ganyusbathwater.oririmod.worldgen.dimension;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalLong;

public class ModDimensionTypes {
    public static final ResourceKey<DimensionType> DUNGEON = ResourceKey.create(Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dungeon"));

    public static void bootstrap(BootstrapContext<DimensionType> context) {
        context.register(DUNGEON, new DimensionType(
                OptionalLong.empty(), // fixedTime
                true, // hasSkylight
                false, // hasCeiling
                false, // ultraWarm
                true, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                0, // minY
                256, // height
                256, // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                ResourceLocation.parse("minecraft:the_end"), // effectsLocation
                0.0f, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 7), 0)
        ));
    }
}
