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

    public static final Supplier<BlockEntityType<RevivalShrineBlockEntity>> REVIVAL_SHRINE = BLOCK_ENTITIES.register(
            "revival_shrine",
            () -> BlockEntityType.Builder.of(RevivalShrineBlockEntity::new,
                    ModBlocks.REVIVAL_SHRINE.get()).build(null));

    public static final Supplier<BlockEntityType<ModSignBlockEntity>> MOD_SIGN = BLOCK_ENTITIES.register(
            "mod_sign",
            () -> BlockEntityType.Builder.of(ModSignBlockEntity::new,
                    ModBlocks.ELDER_SIGN.get(), ModBlocks.ELDER_WALL_SIGN.get(),
                    ModBlocks.SCARLET_SIGN.get(), ModBlocks.SCARLET_WALL_SIGN.get(),
                    ModBlocks.ABYSS_CROWN_SIGN.get(), ModBlocks.ABYSS_CROWN_WALL_SIGN.get()).build(null));

    public static final Supplier<BlockEntityType<ModHangingSignBlockEntity>> MOD_HANGING_SIGN = BLOCK_ENTITIES.register(
            "mod_hanging_sign",
            () -> BlockEntityType.Builder.of(ModHangingSignBlockEntity::new,
                    ModBlocks.ELDER_HANGING_SIGN.get(), ModBlocks.ELDER_WALL_HANGING_SIGN.get(),
                    ModBlocks.SCARLET_HANGING_SIGN.get(), ModBlocks.SCARLET_WALL_HANGING_SIGN.get(),
                    ModBlocks.ABYSS_CROWN_HANGING_SIGN.get(), ModBlocks.ABYSS_CROWN_WALL_HANGING_SIGN.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
