package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, OririMod.MOD_ID);

    public static final Supplier<BlockEntityType<EquinoxTableBlockEntity>> EQUINOX_TABLE = BLOCK_ENTITIES.register(
            "equinox_table",
            () -> BlockEntityType.Builder.of(EquinoxTableBlockEntity::new,
                    ModBlocks.EQUINOX_TABLE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
