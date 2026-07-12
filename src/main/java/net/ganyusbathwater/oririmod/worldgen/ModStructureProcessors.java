package net.ganyusbathwater.oririmod.worldgen;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.processor.LootTableChestProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModStructureProcessors {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = 
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, OririMod.MOD_ID);

    public static final Supplier<StructureProcessorType<LootTableChestProcessor>> LOOT_CHEST_PROCESSOR = 
            PROCESSORS.register("loot_chest_processor", () -> () -> LootTableChestProcessor.CODEC);

    public static void register(IEventBus eventBus) {
        PROCESSORS.register(eventBus);
    }
}
