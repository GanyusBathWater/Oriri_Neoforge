package net.ganyusbathwater.oririmod.block;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.ElderBerryBush;
import net.ganyusbathwater.oririmod.block.custom.MagicBarrierBlock;
import net.ganyusbathwater.oririmod.block.custom.MagicBarrierCoreBlock;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(OririMod.MOD_ID);

    public static final DeferredBlock<Block> DARK_SOIL_BLOCK = registerBlock("dark_soil_block", () -> new Block(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> ELDERBUSH_BLOCK = registerBlock("elderbush_block",() -> new ElderBerryBush(BlockBehaviour.Properties.of().noOcclusion().isViewBlocking((s, g, p) -> false)));

    public static final DeferredBlock<Block> MAGIC_BARRIER_BLOCK = registerBlock("magic_barrier_block",() -> new MagicBarrierBlock(BlockBehaviour.Properties.of().noLootTable().noOcclusion().sound(SoundType.AMETHYST).isViewBlocking((s, g, p) -> false)));

    public static final DeferredBlock<Block> MAGIC_BARRIER_CORE_BLOCK = registerBlock("magic_barrier_core_block",() -> new MagicBarrierCoreBlock(BlockBehaviour.Properties.of().noLootTable().noOcclusion().sound(SoundType.AMETHYST).isViewBlocking((s, g, p) -> false)));

    public static final DeferredBlock<Block> MANA_CRYSTAL_CLUSTER = registerBlock("mana_crystal_cluster", () -> new AmethystClusterBlock(1, 1, BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> MANA_CRYSTAL_BLOCK = registerBlock("mana_crystal_block", () -> new AmethystBlock(BlockBehaviour.Properties.of()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}