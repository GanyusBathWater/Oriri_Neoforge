package net.ganyusbathwater.oririmod.worldgen.structure;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, OririMod.MOD_ID);

    public static final Supplier<StructureType<CenteredJigsawStructure>> CENTERED_JIGSAW = STRUCTURE_TYPES.register("centered_jigsaw", () -> type(CenteredJigsawStructure.CODEC));

    private static <S extends Structure> StructureType<S> type(MapCodec<S> codec) {
        return () -> codec;
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
    }
}
