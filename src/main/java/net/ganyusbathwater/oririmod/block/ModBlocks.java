package net.ganyusbathwater.oririmod.block;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.*;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.worldgen.tree.ModTreeGrowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.ganyusbathwater.oririmod.block.custom.AbyssCrownSaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.ganyusbathwater.oririmod.util.ModWoodTypes;
import net.minecraft.world.level.block.IronBarsBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import java.util.function.Supplier;

public class ModBlocks {

        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OririMod.MOD_ID);

        public static final DeferredBlock<Block> DARK_SOIL_BLOCK = registerBlock("dark_soil_block",
                        () -> new GrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

        public static final DeferredBlock<Block> BLOOD_SLUDGE = registerBlock("blood_sludge",
                        () -> new SlimeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK)));

        public static final DeferredBlock<Block> ELDERBUSH_BLOCK = registerBlock("elderbush_block",
                        () -> new ElderBerryBush(BlockBehaviour.Properties.of().noOcclusion()
                                        .sound(SoundType.GRASS).isViewBlocking((s, g, p) -> false)));

        public static final DeferredBlock<Block> MAGIC_BARRIER_BLOCK = registerBlock("magic_barrier_block",
                        () -> new MagicBarrierBlock(BlockBehaviour.Properties.of().noLootTable().noOcclusion()
                                        .sound(SoundType.AMETHYST).isViewBlocking((s, g, p) -> false)));

        public static final DeferredBlock<Block> MAGIC_BARRIER_CORE_BLOCK = registerBlock("magic_barrier_core_block",
                        () -> new MagicBarrierCoreBlock(BlockBehaviour.Properties.of().noLootTable().noOcclusion()
                                        .sound(SoundType.AMETHYST).isViewBlocking((s, g, p) -> false)));

        public static final DeferredBlock<Block> MANA_CRYSTAL_CLUSTER = registerBlock("mana_crystal_cluster",
                        () -> new AmethystClusterBlock(7, 3,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER)));

        public static final DeferredBlock<Block> MANA_CRYSTAL_BLOCK = registerBlock("mana_crystal_block",
                        () -> new AmethystBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)
                                        .strength(1.5f, 1.5f)));

        public static final DeferredBlock<Block> ELDER_LOG_BLOCK = registerBlock("elder_log_block",
                        () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

        public static final DeferredBlock<Block> CRACKED_ELDER_LOG_BLOCK = registerBlock("cracked_elder_log_block",
                        () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

        public static final DeferredBlock<Block> ELDER_PLANKS = registerBlock("elder_planks",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

        public static final DeferredBlock<StairBlock> ELDER_STAIRS = registerBlock("elder_stairs",
                        () -> new StairBlock(ModBlocks.ELDER_PLANKS.get().defaultBlockState(),
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)));

        public static final DeferredBlock<SlabBlock> ELDER_SLAB = registerBlock("elder_slab",
                        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));

        public static final DeferredBlock<FenceBlock> ELDER_FENCE = registerBlock("elder_fence",
                        () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)));

        public static final DeferredBlock<FenceGateBlock> ELDER_GATE = registerBlock("elder_gate",
                        () -> new FenceGateBlock(ModWoodTypes.ELDER_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)));

        public static final DeferredBlock<DoorBlock> ELDER_DOOR = registerBlock("elder_door",
                        () -> new DoorBlock(ModWoodTypes.ELDER_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)));

        public static final DeferredBlock<TrapDoorBlock> ELDER_TRAPDOOR = registerBlock("elder_trapdoor",
                        () -> new TrapDoorBlock(ModWoodTypes.ELDER_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));

        public static final DeferredBlock<Block> STRIPPED_ELDER_LOG_BLOCK = registerBlock("stripped_elder_log_block",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));

        public static final DeferredBlock<Block> ELDER_STEM_BLOCK = registerBlock("elder_stem_block",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));

        public static final DeferredBlock<Block> STRIPPED_ELDER_STEM_BLOCK = registerBlock("stripped_elder_stem_block",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));

        public static final DeferredBlock<Block> ELDER_LEAVES_FLOWERING = registerBlock("elder_leaves_flowering",
                        () -> new ElderLeavesFloweringBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWERING_AZALEA_LEAVES)));

        public static final DeferredBlock<Block> ELDER_SPORE_BLOSSOM = registerBlock("elder_spore_blossom",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ElderSporeBlossomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPORE_BLOSSOM)));

        public static final DeferredBlock<Block> ELDER_LEAVES = registerBlock("elder_leaves",
                        () -> new ElderLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES)));

        public static final DeferredBlock<Block> ELDER_SAPLING = registerBlock("elder_sapling",
                        () -> new SaplingBlock(ModTreeGrowers.ELDER_TREE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

        public static final DeferredBlock<Block> POTTED_ELDER_SAPLING = registerPottedBlock("potted_elder_sapling",
                        ELDER_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_ELDER_SAPLING = registerBlock("upgraded_elder_sapling",
                        () -> new UpgradedSaplingBlock(ModTreeGrowers.ELDER_TREE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

        public static final DeferredBlock<Block> POTTED_UPGRADED_ELDER_SAPLING = registerPottedBlock(
                        "potted_upgraded_elder_sapling", UPGRADED_ELDER_SAPLING);

        // ===== SCARLET STONE VARIANTS =====
        public static final DeferredBlock<Block> SCARLET_STONE = registerBlock("scarlet_stone",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));

        public static final DeferredBlock<Block> COBBLED_SCARLET_DEEPSLATE = registerBlock("cobbled_scarlet_deepslate",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE)));

        public static final DeferredBlock<Block> SMOOTH_SCARLET_STONE = registerBlock("smooth_scarlet_stone",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE)));

        public static final DeferredBlock<SlabBlock> SMOOTH_SCARLET_STONE_SLAB = registerBlock(
                        "smooth_scarlet_stone_slab",
                        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE_SLAB)));

        public static final DeferredBlock<Block> SCARLET_STONE_BRICKS = registerBlock("scarlet_stone_bricks",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));

        public static final DeferredBlock<Block> CHISELED_SCARLET_STONE_BRICKS = registerBlock(
                        "chiseled_scarlet_stone_bricks",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS)));

        public static final DeferredBlock<Block> CRACKED_SCARLET_STONE_BRICKS = registerBlock(
                        "cracked_scarlet_stone_bricks",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS)));

        public static final DeferredBlock<Block> MOSSY_SCARLET_STONE_BRICKS = registerBlock(
                        "mossy_scarlet_stone_bricks",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS)));

        // ===== SCARLET DEEPSLATE VARIANTS =====
        public static final DeferredBlock<Block> SCARLET_DEEPSLATE = registerBlock("scarlet_deepslate",
                        () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)));

        public static final DeferredBlock<Block> POLISHED_SCARLET_DEEPSLATE = registerBlock(
                        "polished_scarlet_deepslate",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)));

        public static final DeferredBlock<Block> CHISELED_SCARLET_DEEPSLATE = registerBlock(
                        "chiseled_scarlet_deepslate",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_DEEPSLATE)));

        public static final DeferredBlock<Block> SCARLET_DEEPSLATE_BRICKS = registerBlock("scarlet_deepslate_bricks",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS)));

        public static final DeferredBlock<Block> CRACKED_SCARLET_DEEPSLATE_BRICKS = registerBlock(
                        "cracked_scarlet_deepslate_bricks",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_DEEPSLATE_BRICKS)));

        public static final DeferredBlock<Block> SCARLET_DEEPSLATE_TILES = registerBlock("scarlet_deepslate_tiles",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)));

        public static final DeferredBlock<Block> CRACKED_SCARLET_DEEPSLATE_TILES = registerBlock(
                        "cracked_scarlet_deepslate_tiles",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_DEEPSLATE_TILES)));

        // ===== SCARLET WOOD TYPES =====
        public static final DeferredBlock<Block> SCARLET_LOG = registerBlock("scarlet_log",
                        () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

        public static final DeferredBlock<Block> STRIPPED_SCARLET_LOG = registerBlock("stripped_scarlet_log",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));

        public static final DeferredBlock<Block> SCARLET_PLANKS = registerBlock("scarlet_planks",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

        public static final DeferredBlock<StairBlock> SCARLET_STAIRS = registerBlock("scarlet_stairs",
                        () -> new StairBlock(ModBlocks.SCARLET_PLANKS.get().defaultBlockState(),
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)));

        public static final DeferredBlock<SlabBlock> SCARLET_SLAB = registerBlock("scarlet_slab",
                        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));

        public static final DeferredBlock<FenceBlock> SCARLET_FENCE = registerBlock("scarlet_fence",
                        () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)));

        public static final DeferredBlock<FenceGateBlock> SCARLET_GATE = registerBlock("scarlet_gate",
                        () -> new FenceGateBlock(ModWoodTypes.SCARLET_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)));

        public static final DeferredBlock<DoorBlock> SCARLET_DOOR = registerBlock("scarlet_door",
                        () -> new DoorBlock(ModWoodTypes.SCARLET_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)));

        public static final DeferredBlock<TrapDoorBlock> SCARLET_TRAPDOOR = registerBlock("scarlet_trapdoor",
                        () -> new TrapDoorBlock(ModWoodTypes.SCARLET_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));

        public static final DeferredBlock<Block> SCARLET_SAPLING = registerBlock("scarlet_sapling",
                        () -> new SaplingBlock(ModTreeGrowers.SCARLET_TREE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

        public static final DeferredBlock<Block> POTTED_SCARLET_SAPLING = registerPottedBlock("potted_scarlet_sapling",
                        SCARLET_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_SCARLET_SAPLING = registerBlock("upgraded_scarlet_sapling",
                        () -> new UpgradedSaplingBlock(ModTreeGrowers.SCARLET_TREE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

        public static final DeferredBlock<Block> POTTED_UPGRADED_SCARLET_SAPLING = registerPottedBlock(
                        "potted_upgraded_scarlet_sapling", UPGRADED_SCARLET_SAPLING);

        // ===== SCARLET VEGETATION =====
        public static final DeferredBlock<Block> SCARLET_LEAVES = registerBlock("scarlet_leaves",
                        () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));

        public static final DeferredBlock<Block> SCARLET_MOSS = registerBlock("scarlet_moss",
                        () -> new MossBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK)));

        public static final DeferredBlock<Block> SCARLET_VINE = registerBlock("scarlet_vine",
                        () -> new VineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.VINE)));

        public static final DeferredBlock<Block> STAR_HERB = registerBlock("star_herb",
                        () -> new FlowerBlock(SuspiciousStewEffects.EMPTY,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).lightLevel(state -> 7)));

        public static final DeferredBlock<Block> BLOOD_CAP_BLOCK = registerBlock("blood_cap",
                        () -> new BloodCapBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).noCollission().instabreak()));

        public static final DeferredBlock<Block> POTTED_STAR_HERB = registerPottedBlock("potted_star_herb", STAR_HERB);

        // ===== NEW SCARLET BLOCKS =====
        public static final DeferredBlock<Block> SCARLET_GRASS_BLOCK = registerBlock("scarlet_grass_block",
                        () -> new ScarletGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

        public static final DeferredBlock<Block> SCARLET_STEM = registerBlock("scarlet_stem",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));

        public static final DeferredBlock<Block> STRIPPED_SCARLET_STEM = registerBlock("stripped_scarlet_stem",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));

        public static final DeferredBlock<Block> SCARLET_GRASS = registerBlock("scarlet_grass",
                        () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)));

        public static final DeferredBlock<Block> SCARLET_TOOTH_LEAVES = registerBlock("scarlet_tooth_leaves",
                        () -> new ScarletToothLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                                        .noCollission().instabreak()));

        public static final DeferredBlock<Block> POTTED_SCARLET_TOOTH_LEAVES = registerPottedBlock(
                        "potted_scarlet_tooth_leaves", SCARLET_TOOTH_LEAVES);

        public static final DeferredBlock<Block> SCARLET_LILY = registerBlock("scarlet_lily",
                        () -> new ScarletLilyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD)));

        // ===== FLUORITE BLOCKS =====
        public static final DeferredBlock<Block> FLUORITE_BLOCK = registerBlock("fluorite_block",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)
                                        .strength(1.5f, 1.5f).lightLevel((state) -> 5).noOcclusion()));

        public static final DeferredBlock<Block> FLUORITE_CLUSTER = registerBlock("fluorite_cluster",
                        () -> new AmethystClusterBlock(7, 3,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER)
                                                        .lightLevel((state) -> 5).noOcclusion()));

        public static final DeferredBlock<Block> GLOWLINGS_BLOCK = registerBlock("glowling",
                        () -> new GlowlingsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)
                                        .noCollission().instabreak().lightLevel((state) -> 5)));

        // ===== SCARLET DRIPSTONE =====
        public static final DeferredBlock<Block> SCARLET_DRIPSTONE_BLOCK = registerBlock("scarlet_dripstone_block",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DRIPSTONE_BLOCK)));

        public static final DeferredBlock<Block> POINTED_SCARLET_DRIPSTONE = registerBlock("pointed_scarlet_dripstone",
                        () -> new ScarletPointedDripstoneBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.POINTED_DRIPSTONE)));

        // ===== SOL SAND & SANDSTONES =====
        public static final DeferredBlock<Block> SOL_SAND = registerBlock("sol_sand",
                        () -> new ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xFFDED6A3),
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

        public static final DeferredBlock<Block> SOL_SANDSTONE = registerBlock("sol_sandstone",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE)));

        public static final DeferredBlock<Block> CUT_SOL_SANDSTONE = registerBlock("cut_sol_sandstone",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CUT_SANDSTONE)));

        public static final DeferredBlock<Block> CHISELED_SOL_SANDSTONE = registerBlock("chiseled_sol_sandstone",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_SANDSTONE)));

        // ===== BROKEN SWORDS =====
        public static final DeferredBlock<Block> BROKEN_SWORD_BLOCK = registerBlock("broken_sword_block",
                        () -> new BrokenSwordBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
                                        .strength(3.0f, 3.0f)));

        public static final DeferredBlock<Block> TILTED_BROKEN_SWORD_BLOCK = registerBlock("tilted_broken_sword_block",
                        () -> new TiltedBrokenSwordBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                                        .noOcclusion().strength(3.0f, 3.0f)));

        // ===== GLASS & PANES =====
        public static final DeferredBlock<Block> SOL_GLASS = registerBlock("sol_glass",
                        () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                                        .lightLevel((state) -> 5)));

        public static final DeferredBlock<Block> SOL_GLASS_PANE = registerBlock("sol_glass_pane",
                        () -> new IronBarsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE)
                                        .lightLevel((state) -> 5)));

        // ===== JADE BLOCKS =====
        public static final DeferredBlock<Block> JADE_BLOCK = registerBlock("jade_block",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK)));

        public static final DeferredBlock<Block> JADE_ORE = registerBlock("jade_ore",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)));

        public static final DeferredBlock<Block> DEEPSLATE_JADE_ORE = registerBlock("deepslate_jade_ore",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_EMERALD_ORE)));

        public static final DeferredBlock<Block> DRAGON_IRON_ORE = registerBlock("dragon_iron_ore",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)));

        public static final DeferredBlock<Block> DEEPSLATE_DRAGON_IRON_ORE = registerBlock("deepslate_dragon_iron_ore",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE)));

        public static final DeferredBlock<StairBlock> JADE_STAIRS = registerBlock("jade_stairs",
                        () -> new StairBlock(ModBlocks.JADE_BLOCK.get().defaultBlockState(),
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK)));

        public static final DeferredBlock<SlabBlock> JADE_SLAB = registerBlock("jade_slab",
                        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK)));

        public static final DeferredBlock<WallBlock> JADE_WALL = registerBlock("jade_wall",
                        () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK)));

        public static final DeferredBlock<Block> ELDERWOODS_PORTAL_BLOCK = registerBlock("elderwoods_portal_block",
                        () -> new ElderwoodsPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL)
                                        .noCollission().noLootTable()
                                        .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

        public static final DeferredBlock<Block> MOONSHROOM_BLOCK = registerBlock("moonshroom",
                        () -> new MoonshroomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)
                                        .noCollission().instabreak().lightLevel((state) -> 3)));

        // ===== UPGRADED VANILLA SAPLINGS =====
        public static final DeferredBlock<Block> UPGRADED_OAK_SAPLING = registerBlock("upgraded_oak_sapling",
                        () -> new UpgradedSaplingBlock(TreeGrower.OAK,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
        public static final DeferredBlock<Block> POTTED_UPGRADED_OAK_SAPLING = registerPottedBlock(
                        "potted_upgraded_oak_sapling", UPGRADED_OAK_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_SPRUCE_SAPLING = registerBlock("upgraded_spruce_sapling",
                        () -> new UpgradedSaplingBlock(TreeGrower.SPRUCE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING)));
        public static final DeferredBlock<Block> POTTED_UPGRADED_SPRUCE_SAPLING = registerPottedBlock(
                        "potted_upgraded_spruce_sapling", UPGRADED_SPRUCE_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_BIRCH_SAPLING = registerBlock("upgraded_birch_sapling",
                        () -> new UpgradedSaplingBlock(TreeGrower.BIRCH,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SAPLING)));
        public static final DeferredBlock<Block> POTTED_UPGRADED_BIRCH_SAPLING = registerPottedBlock(
                        "potted_upgraded_birch_sapling", UPGRADED_BIRCH_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_JUNGLE_SAPLING = registerBlock("upgraded_jungle_sapling",
                        () -> new UpgradedSaplingBlock(TreeGrower.JUNGLE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_SAPLING)));
        public static final DeferredBlock<Block> POTTED_UPGRADED_JUNGLE_SAPLING = registerPottedBlock(
                        "potted_upgraded_jungle_sapling", UPGRADED_JUNGLE_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_ACACIA_SAPLING = registerBlock("upgraded_acacia_sapling",
                        () -> new UpgradedSaplingBlock(TreeGrower.ACACIA,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_SAPLING)));
        public static final DeferredBlock<Block> POTTED_UPGRADED_ACACIA_SAPLING = registerPottedBlock(
                        "potted_upgraded_acacia_sapling", UPGRADED_ACACIA_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_DARK_OAK_SAPLING = registerBlock("upgraded_dark_oak_sapling",
                        () -> new UpgradedSaplingBlock(TreeGrower.DARK_OAK,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SAPLING)));
        public static final DeferredBlock<Block> POTTED_UPGRADED_DARK_OAK_SAPLING = registerPottedBlock(
                        "potted_upgraded_dark_oak_sapling", UPGRADED_DARK_OAK_SAPLING);

        public static final DeferredBlock<Block> UPGRADED_CHERRY_SAPLING = registerBlock("upgraded_cherry_sapling",
                        () -> new UpgradedSaplingBlock(TreeGrower.CHERRY,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING)));
        public static final DeferredBlock<Block> POTTED_UPGRADED_CHERRY_SAPLING = registerPottedBlock(
                        "potted_upgraded_cherry_sapling", UPGRADED_CHERRY_SAPLING);

        // ===== EQUINOX TABLE =====
        public static final DeferredBlock<Block> EQUINOX_TABLE = registerBlock("equinox_table",
                        () -> new EquinoxTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
                                        .strength(5.0f, 6.0f).requiresCorrectToolForDrops()));

        // ===== NEW BLOCKS: Hardened Manashroom & Abyss Crown =====
        public static final DeferredBlock<Block> HARDENED_MANASHROOM = registerBlock("hardened_manashroom",
                        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)));

        public static final DeferredBlock<Block> ABYSS_CROWN_LOG = registerBlock("abyss_crown_log",
                        () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

        public static final DeferredBlock<Block> STRIPPED_ABYSS_CROWN_LOG = registerBlock("stripped_abyss_crown_log",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));

        public static final DeferredBlock<Block> ABYSS_CROWN_STEM = registerBlock("abyss_crown_stem",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));

        public static final DeferredBlock<Block> STRIPPED_ABYSS_CROWN_STEM = registerBlock("stripped_abyss_crown_stem",
                        () -> new ModFlammableRotatedPillarBlock(
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));

        public static final DeferredBlock<Block> ABYSS_CROWN_PLANKS = registerBlock("abyss_crown_planks",
                        () -> new ModFlammableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

        public static final DeferredBlock<StairBlock> ABYSS_CROWN_STAIRS = registerBlock("abyss_crown_stairs",
                        () -> new StairBlock(ModBlocks.ABYSS_CROWN_PLANKS.get().defaultBlockState(),
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)) {
                                @Override
                                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return true;
                                }

                                @Override
                                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 20;
                                }

                                @Override
                                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 5;
                                }
                        });

        public static final DeferredBlock<SlabBlock> ABYSS_CROWN_SLAB = registerBlock("abyss_crown_slab",
                        () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)) {
                                @Override
                                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return true;
                                }

                                @Override
                                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 20;
                                }

                                @Override
                                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 5;
                                }
                        });

        public static final DeferredBlock<FenceBlock> ABYSS_CROWN_FENCE = registerBlock("abyss_crown_fence",
                        () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)) {
                                @Override
                                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return true;
                                }

                                @Override
                                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 20;
                                }

                                @Override
                                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 5;
                                }
                        });

        public static final DeferredBlock<FenceGateBlock> ABYSS_CROWN_GATE = registerBlock("abyss_crown_gate",
                        () -> new FenceGateBlock(ModWoodTypes.ABYSS_CROWN_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)) {
                                @Override
                                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return true;
                                }

                                @Override
                                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 20;
                                }

                                @Override
                                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 5;
                                }
                        });

        public static final DeferredBlock<DoorBlock> ABYSS_CROWN_DOOR = registerBlock("abyss_crown_door",
                        () -> new DoorBlock(ModWoodTypes.ABYSS_CROWN_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)) {
                                @Override
                                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return true;
                                }

                                @Override
                                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 20;
                                }

                                @Override
                                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 5;
                                }
                        });

        public static final DeferredBlock<TrapDoorBlock> ABYSS_CROWN_TRAPDOOR = registerBlock("abyss_crown_trapdoor",
                        () -> new TrapDoorBlock(ModWoodTypes.ABYSS_CROWN_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)) {
                                @Override
                                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return true;
                                }

                                @Override
                                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 20;
                                }

                                @Override
                                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos,
                                                Direction direction) {
                                        return 5;
                                }
                        });

        public static final DeferredBlock<Block> ABYSS_CROWN_LEAVES = registerBlock("abyss_crown_leaves",
                        () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));

        public static final DeferredBlock<Block> ABYSS_CROWN_SAPLING = registerBlock("abyss_crown_sapling",
                        () -> new AbyssCrownSaplingBlock(ModTreeGrowers.ABYSS_CROWN_TREE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING), false));

        public static final DeferredBlock<Block> UPGRADED_ABYSS_CROWN_SAPLING = registerBlock(
                        "upgraded_abyss_crown_sapling",
                        () -> new AbyssCrownSaplingBlock(ModTreeGrowers.ABYSS_CROWN_TREE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING), true));

        public static final DeferredBlock<Block> REVIVAL_SHRINE = registerBlock("revival_shrine",
                        () -> new RevivalShrineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).noOcclusion()));

        public static final DeferredBlock<Block> AETHER_MAGMA_BLOCK = registerBlock("aether_magma_block",
                        () -> new AetherMagmaBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MAGMA_BLOCK)
                                        .lightLevel(state -> 3)));

        public static final DeferredBlock<Block> AETHER_FIRE_BLOCK = BLOCKS.register("aether_fire_block",
                        () -> new AetherFireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FIRE)
                                        .noCollission().instabreak().lightLevel(state -> 15).noLootTable()));

        // ===== BUTTONS & PRESSURE PLATES =====
        public static final DeferredBlock<ButtonBlock> ELDER_BUTTON = registerBlock("elder_button",
                        () -> new ButtonBlock(ModWoodTypes.ELDER_BLOCK_SET_TYPE, 30,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));
        public static final DeferredBlock<ButtonBlock> SCARLET_BUTTON = registerBlock("scarlet_button",
                        () -> new ButtonBlock(ModWoodTypes.SCARLET_BLOCK_SET_TYPE, 30,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));
        public static final DeferredBlock<ButtonBlock> ABYSS_CROWN_BUTTON = registerBlock("abyss_crown_button",
                        () -> new ButtonBlock(ModWoodTypes.ABYSS_CROWN_BLOCK_SET_TYPE, 30,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));

        public static final DeferredBlock<PressurePlateBlock> ELDER_PRESSURE_PLATE = registerBlock(
                        "elder_pressure_plate", () -> new PressurePlateBlock(ModWoodTypes.ELDER_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));
        public static final DeferredBlock<PressurePlateBlock> SCARLET_PRESSURE_PLATE = registerBlock(
                        "scarlet_pressure_plate", () -> new PressurePlateBlock(ModWoodTypes.SCARLET_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));
        public static final DeferredBlock<PressurePlateBlock> ABYSS_CROWN_PRESSURE_PLATE = registerBlock(
                        "abyss_crown_pressure_plate",
                        () -> new PressurePlateBlock(ModWoodTypes.ABYSS_CROWN_BLOCK_SET_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));

        // ===== SIGNS & HANGING SIGNS =====
        public static final DeferredBlock<StandingSignBlock> ELDER_SIGN = BLOCKS.register("elder_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModStandingSignBlock(
                                        ModWoodTypes.ELDER_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)));
        public static final DeferredBlock<WallSignBlock> ELDER_WALL_SIGN = BLOCKS.register("elder_wall_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModWallSignBlock(
                                        ModWoodTypes.ELDER_WOOD_TYPE, BlockBehaviour.Properties
                                                        .ofFullCopy(Blocks.OAK_WALL_SIGN).lootFrom(ELDER_SIGN)));
        public static final DeferredBlock<CeilingHangingSignBlock> ELDER_HANGING_SIGN = BLOCKS.register(
                        "elder_hanging_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModCeilingHangingSignBlock(
                                        ModWoodTypes.ELDER_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN)));
        public static final DeferredBlock<WallHangingSignBlock> ELDER_WALL_HANGING_SIGN = BLOCKS
                        .register("elder_wall_hanging_sign",
                                        () -> new net.ganyusbathwater.oririmod.block.custom.ModWallHangingSignBlock(
                                                        ModWoodTypes.ELDER_WOOD_TYPE,
                                                        BlockBehaviour.Properties
                                                                        .ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN)
                                                                        .lootFrom(ELDER_HANGING_SIGN)));

        public static final DeferredBlock<StandingSignBlock> SCARLET_SIGN = BLOCKS.register("scarlet_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModStandingSignBlock(
                                        ModWoodTypes.SCARLET_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)));
        public static final DeferredBlock<WallSignBlock> SCARLET_WALL_SIGN = BLOCKS.register("scarlet_wall_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModWallSignBlock(
                                        ModWoodTypes.SCARLET_WOOD_TYPE, BlockBehaviour.Properties
                                                        .ofFullCopy(Blocks.OAK_WALL_SIGN).lootFrom(SCARLET_SIGN)));
        public static final DeferredBlock<CeilingHangingSignBlock> SCARLET_HANGING_SIGN = BLOCKS.register(
                        "scarlet_hanging_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModCeilingHangingSignBlock(
                                        ModWoodTypes.SCARLET_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN)));
        public static final DeferredBlock<WallHangingSignBlock> SCARLET_WALL_HANGING_SIGN = BLOCKS
                        .register("scarlet_wall_hanging_sign",
                                        () -> new net.ganyusbathwater.oririmod.block.custom.ModWallHangingSignBlock(
                                                        ModWoodTypes.SCARLET_WOOD_TYPE,
                                                        BlockBehaviour.Properties
                                                                        .ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN)
                                                                        .lootFrom(SCARLET_HANGING_SIGN)));

        public static final DeferredBlock<StandingSignBlock> ABYSS_CROWN_SIGN = BLOCKS.register("abyss_crown_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModStandingSignBlock(
                                        ModWoodTypes.ABYSS_CROWN_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN)));
        public static final DeferredBlock<WallSignBlock> ABYSS_CROWN_WALL_SIGN = BLOCKS.register(
                        "abyss_crown_wall_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModWallSignBlock(
                                        ModWoodTypes.ABYSS_CROWN_WOOD_TYPE, BlockBehaviour.Properties
                                                        .ofFullCopy(Blocks.OAK_WALL_SIGN).lootFrom(ABYSS_CROWN_SIGN)));
        public static final DeferredBlock<CeilingHangingSignBlock> ABYSS_CROWN_HANGING_SIGN = BLOCKS.register(
                        "abyss_crown_hanging_sign",
                        () -> new net.ganyusbathwater.oririmod.block.custom.ModCeilingHangingSignBlock(
                                        ModWoodTypes.ABYSS_CROWN_WOOD_TYPE,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN)));
        public static final DeferredBlock<WallHangingSignBlock> ABYSS_CROWN_WALL_HANGING_SIGN = BLOCKS
                        .register("abyss_crown_wall_hanging_sign",
                                        () -> new net.ganyusbathwater.oririmod.block.custom.ModWallHangingSignBlock(
                                                        ModWoodTypes.ABYSS_CROWN_WOOD_TYPE,
                                                        BlockBehaviour.Properties
                                                                        .ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN)
                                                                        .lootFrom(ABYSS_CROWN_HANGING_SIGN)));

        // ===== FORCEFIELD EMITTERS =====
        public static final DeferredBlock<Block> REPELLENT_FORCEFIELD_EMITTER = registerBlock(
                        "repellent_forcefield_emitter",
                        () -> new ForcefieldEmitterBlock(
                                        net.ganyusbathwater.oririmod.block.custom.ForcefieldVariant.REPELLENT,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
        public static final DeferredBlock<Block> ATTRACTING_FORCEFIELD_EMITTER = registerBlock(
                        "attracting_forcefield_emitter",
                        () -> new ForcefieldEmitterBlock(
                                        net.ganyusbathwater.oririmod.block.custom.ForcefieldVariant.ATTRACTING,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
        public static final DeferredBlock<Block> PROTECTION_FORCEFIELD_EMITTER = registerBlock(
                        "protection_forcefield_emitter",
                        () -> new ForcefieldEmitterBlock(
                                        net.ganyusbathwater.oririmod.block.custom.ForcefieldVariant.PROTECTION,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
        public static final DeferredBlock<Block> MODIFIER_FORCEFIELD_EMITTER = registerBlock(
                        "modifier_forcefield_emitter",
                        () -> new ForcefieldEmitterBlock(
                                        net.ganyusbathwater.oririmod.block.custom.ForcefieldVariant.MODIFIER,
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));

        private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
                DeferredBlock<T> toReturn = BLOCKS.register(name, block);
                registerBlockItem(name, toReturn);
                return toReturn;
        }

        private static DeferredBlock<Block> registerPottedBlock(String name, DeferredBlock<Block> plant) {
                return BLOCKS.register(name, () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, plant,
                                BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING)));
        }

        private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
                if (name.equals("scarlet_lily")) {
                        ModItems.ITEMS.register(name,
                                        () -> new net.minecraft.world.item.PlaceOnWaterBlockItem(block.get(),
                                                        new Item.Properties()));
                } else if (name.startsWith("upgraded_")) {
                        ModItems.ITEMS.register(name,
                                        () -> new net.ganyusbathwater.oririmod.item.custom.UpgradedSaplingItem(
                                                        block.get(), new Item.Properties()));
                } else if (name.equals("revival_shrine")) {
                        ModItems.ITEMS.register(name,
                                        () -> new net.ganyusbathwater.oririmod.item.custom.RevivalShrineBlockItem(
                                                        block.get(), new Item.Properties()));
                } else if (name.contains("forcefield_emitter")) {
                        ModItems.ITEMS.register(name,
                                        () -> new net.ganyusbathwater.oririmod.item.custom.ForcefieldEmitterBlockItem(
                                                        block.get(), new Item.Properties()));
                } else {
                        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
                }
        }

        public static void register(IEventBus eventBus) {
                BLOCKS.register(eventBus);
        }
}