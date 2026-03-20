package net.ganyusbathwater.oririmod.worldgen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.carver.ScarletCaveEntranceCarver;
import net.ganyusbathwater.oririmod.worldgen.carver.ElysianAbyssCarver;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCarvers {
        public static final DeferredRegister<WorldCarver<?>> CARVERS = DeferredRegister.create(Registries.CARVER,
                        OririMod.MOD_ID);

        public static final DeferredHolder<WorldCarver<?>, WorldCarver<CaveCarverConfiguration>> SCARLET_CAVE_ENTRANCE = CARVERS
                        .register("scarlet_cave_entrance",
                                        () -> new ScarletCaveEntranceCarver(CaveCarverConfiguration.CODEC));

        public static final DeferredHolder<WorldCarver<?>, WorldCarver<CaveCarverConfiguration>> ELYSIAN_ABYSS_CARVER = CARVERS
                        .register("elysian_abyss_carver",
                                        () -> new ElysianAbyssCarver(CaveCarverConfiguration.CODEC));
}

