package net.ganyusbathwater.oririmod.block;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.ElderBerryBush;
import net.ganyusbathwater.oririmod.block.custom.MagicBarrierBlock;
import net.ganyusbathwater.oririmod.block.custom.MagicBarrierCoreBlock;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(OririMod.MOD_ID);

    public static final DeferredBlock<Block> DARK_SOIL = registerBlock("dark_soil_block", () -> new Block(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> ELDERBUSH_BLOCK = registerBlock("elderbush_block",() -> new ElderBerryBush(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> MAGIC_BARRIER_BLOCK = registerBlock("magic_barrier_block",() -> new MagicBarrierBlock(BlockBehaviour.Properties.of()));         //.nonOpaque().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.AMETHYST_CLUSTER)));

    public static final DeferredBlock<Block> MAGIC_BARRIER_CORE_BLOCK = registerBlock("magic_barrier_core_block",() -> new MagicBarrierCoreBlock(BlockBehaviour.Properties.of()));    //.nonOpaque().strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.AMETHYST_CLUSTER)));

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