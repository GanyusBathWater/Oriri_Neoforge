package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.processor.OutpostTerrainBlendProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;

public class ModStructureProcessorLists {
    public static final ResourceKey<StructureProcessorList> OUTPOST_TERRAIN_BLEND_KEY = ResourceKey.create(
            Registries.PROCESSOR_LIST,
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "outpost_terrain_blend"));

    public static void bootstrap(BootstrapContext<StructureProcessorList> context) {
        context.register(OUTPOST_TERRAIN_BLEND_KEY,
                new StructureProcessorList(List.of(OutpostTerrainBlendProcessor.INSTANCE)));
    }
}
