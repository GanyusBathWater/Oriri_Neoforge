package net.ganyusbathwater.oririmod.block;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.*;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.worldgen.tree.ModTreeGrowers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(OririMod.MOD_ID);

    public static final DeferredBlock<Block> DARK_SOIL_BLOCK = registerBlock("dark_soil_block", () -> new GrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

    public static final DeferredBlock<Block> ELDERBUSH_BLOCK = registerBlock("elderbush_block",() -> new ElderBerryBush(BlockBehaviour.Properties.of().noOcclusion().isViewBlocking((s, g, p) -> false)));

    public static final DeferredBlock<Block> MAGIC_BARRIER_BLOCK = registerBlock("magic_barrier_block",() -> new MagicBarrierBlock(BlockBehaviour.Properties.of().noLootTable().noOcclusion().sound(SoundType.AMETHYST).isViewBlocking((s, g, p) -> false)));

    public static final DeferredBlock<Block> MAGIC_BARRIER_CORE_BLOCK = registerBlock("magic_barrier_core_block",() -> new MagicBarrierCoreBlock(BlockBehaviour.Properties.of().noLootTable().noOcclusion().sound(SoundType.AMETHYST).isViewBlocking((s, g, p) -> false)));

    public static final DeferredBlock<Block> MANA_CRYSTAL_CLUSTER = registerBlock("mana_crystal_cluster", () -> new AmethystClusterBlock(7, 3, BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER)));

    public static final DeferredBlock<Block> MANA_CRYSTAL_BLOCK = registerBlock("mana_crystal_block", () -> new AmethystBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));

    public static final DeferredBlock<Block> ELDER_LOG_BLOCK = registerBlock("elder_log_block", () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final DeferredBlock<Block> CRACKED_ELDER_LOG_BLOCK = registerBlock("cracked_elder_log_block", () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final DeferredBlock<Block> ELDER_PLANKS = registerBlock("elder_planks", () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

    public static final DeferredBlock<Block> STRIPPED_ELDER_LOG_BLOCK = registerBlock("stripped_elder_log_block", () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));

    public static final DeferredBlock<Block> ELDER_STEM_BLOCK = registerBlock("elder_stem_block", () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));

    public static final DeferredBlock<Block> STRIPPED_ELDER_STEM_BLOCK = registerBlock("stripped_elder_stem_block", () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));

    public static final DeferredBlock<Block> ELDER_LEAVES_FLOWERING = registerBlock("elder_leaves_flowering", () -> new ElderLeavesFloweringBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWERING_AZALEA_LEAVES)));

    public static final DeferredBlock<Block> ELDER_SPORE_BLOSSOM = registerBlock("elder_spore_blossom", () -> new SporeBlossomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPORE_BLOSSOM)));

    public static final DeferredBlock<Block> ELDER_LEAVES = registerBlock("elder_leaves", () -> new ElderLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES)));

    public static final DeferredBlock<Block> ELDER_SAPLING = registerBlock("elder_sapling", () -> new SaplingBlock(ModTreeGrowers.ELDER_TREE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

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