package net.ganyusbathwater.oririmod.item;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItemGroups {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, OririMod.MOD_ID);

    public static final Supplier<CreativeModeTab> Oriri_TAB = CREATIVE_MODE_TAB.register("oriri_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.QILINS_WRATH.get()))
                    .title(Component.translatable("creative.oriri_tab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // -------------Items-------------
                        output.accept(ModItems.FIRE_CRYSTAL);
                        output.accept(ModItems.MANA_MANIFESTATION);
                        output.accept(ModItems.FLUORITE_CRYSTAL);
                        output.accept(ModItems.JADE);
                        output.accept(ModItems.MOON_STONE);
                        output.accept(ModItems.DAMNED_SOUL);
                        output.accept(ModItems.HOLLOW_SOUL);
                        output.accept(ModItems.IRAS_SOUL);
                        output.accept(ModItems.IRON_STICK);
                        output.accept(ModItems.POWER_SOUL);
                        output.accept(ModItems.TORTURED_SOUL);
                        output.accept(ModItems.VOID_SOUL);
                        output.accept(ModFluids.AETHER_BUCKET.get());
                        output.accept(ModFluids.BLOOD_WATER_BUCKET.get());

                        // -------------Blocks------------
                        output.accept(ModBlocks.MANA_CRYSTAL_BLOCK);
                        output.accept(ModBlocks.MANA_CRYSTAL_CLUSTER);
                        output.accept(ModBlocks.FLUORITE_BLOCK);
                        output.accept(ModBlocks.FLUORITE_CLUSTER);
                        output.accept(ModBlocks.JADE_BLOCK);
                        output.accept(ModBlocks.JADE_ORE);
                        output.accept(ModBlocks.DEEPSLATE_JADE_ORE);
                        output.accept(ModBlocks.JADE_STAIRS);
                        output.accept(ModBlocks.JADE_SLAB);
                        output.accept(ModBlocks.JADE_WALL);
                        output.accept(ModBlocks.DARK_SOIL_BLOCK);
                        output.accept(ModBlocks.ELDERBUSH_BLOCK);
                        output.accept(ModBlocks.MAGIC_BARRIER_BLOCK);
                        output.accept(ModBlocks.MAGIC_BARRIER_CORE_BLOCK);
                        output.accept(ModBlocks.ELDER_LOG_BLOCK);
                        output.accept(ModBlocks.CRACKED_ELDER_LOG_BLOCK);
                        output.accept(ModBlocks.STRIPPED_ELDER_LOG_BLOCK);
                        output.accept(ModBlocks.ELDER_STEM_BLOCK);
                        output.accept(ModBlocks.STRIPPED_ELDER_STEM_BLOCK);
                        output.accept(ModBlocks.ELDER_PLANKS);
                        output.accept(ModBlocks.ELDER_STAIRS);
                        output.accept(ModBlocks.ELDER_SLAB);
                        output.accept(ModBlocks.ELDER_FENCE);
                        output.accept(ModBlocks.ELDER_GATE);
                        output.accept(ModBlocks.ELDER_LEAVES);
                        output.accept(ModBlocks.ELDER_LEAVES_FLOWERING);
                        output.accept(ModBlocks.ELDER_SAPLING);
                        output.accept(ModBlocks.UPGRADED_ELDER_SAPLING);
                        output.accept(ModBlocks.ELDER_SPORE_BLOSSOM);
                        output.accept(ModBlocks.ELDER_LEAVES); // Added missing leaves if they were missed
                        output.accept(ModBlocks.ELDER_LEAVES_FLOWERING); // Ensuring order

                        // Scarlet Block Group
                        output.accept(ModBlocks.SCARLET_GRASS_BLOCK);
                        output.accept(ModBlocks.SCARLET_STONE);
                        output.accept(ModBlocks.COBBLED_SCARLET_DEEPSLATE);
                        output.accept(ModBlocks.SMOOTH_SCARLET_STONE);
                        output.accept(ModBlocks.SMOOTH_SCARLET_STONE_SLAB);
                        output.accept(ModBlocks.SCARLET_STONE_BRICKS);
                        output.accept(ModBlocks.CHISELED_SCARLET_STONE_BRICKS);
                        output.accept(ModBlocks.CRACKED_SCARLET_STONE_BRICKS);
                        output.accept(ModBlocks.MOSSY_SCARLET_STONE_BRICKS);

                        output.accept(ModBlocks.SCARLET_DEEPSLATE);
                        output.accept(ModBlocks.POLISHED_SCARLET_DEEPSLATE);
                        output.accept(ModBlocks.CHISELED_SCARLET_DEEPSLATE);
                        output.accept(ModBlocks.SCARLET_DEEPSLATE_BRICKS);
                        output.accept(ModBlocks.CRACKED_SCARLET_DEEPSLATE_BRICKS);
                        output.accept(ModBlocks.SCARLET_DEEPSLATE_TILES);
                        output.accept(ModBlocks.CRACKED_SCARLET_DEEPSLATE_TILES);

                        output.accept(ModBlocks.SCARLET_LOG);
                        output.accept(ModBlocks.STRIPPED_SCARLET_LOG);
                        output.accept(ModBlocks.SCARLET_STEM);
                        output.accept(ModBlocks.STRIPPED_SCARLET_STEM);
                        output.accept(ModBlocks.SCARLET_PLANKS);
                        output.accept(ModBlocks.SCARLET_STAIRS);
                        output.accept(ModBlocks.SCARLET_SLAB);
                        output.accept(ModBlocks.SCARLET_FENCE);
                        output.accept(ModBlocks.SCARLET_GATE);
                        output.accept(ModBlocks.SCARLET_SAPLING);
                        output.accept(ModBlocks.UPGRADED_SCARLET_SAPLING);
                        output.accept(ModBlocks.SCARLET_LEAVES);
                        output.accept(ModBlocks.SCARLET_TOOTH_LEAVES);
                        output.accept(ModBlocks.SCARLET_GRASS);
                        output.accept(ModBlocks.SCARLET_MOSS);
                        output.accept(ModBlocks.SCARLET_VINE);
                        output.accept(ModBlocks.SCARLET_LILY);
                        output.accept(ModBlocks.SCARLET_DRIPSTONE_BLOCK);
                        output.accept(ModBlocks.POINTED_SCARLET_DRIPSTONE);

                        // Sol Sand Blocks
                        output.accept(ModBlocks.SOL_SAND);
                        output.accept(ModBlocks.SOL_SANDSTONE);
                        output.accept(ModBlocks.CUT_SOL_SANDSTONE);
                        output.accept(ModBlocks.CHISELED_SOL_SANDSTONE);

                        // Sword Blocks
                        output.accept(ModBlocks.BROKEN_SWORD_BLOCK);
                        output.accept(ModBlocks.TILTED_BROKEN_SWORD_BLOCK);

                        output.accept(ModBlocks.UPGRADED_OAK_SAPLING);
                        output.accept(ModBlocks.UPGRADED_SPRUCE_SAPLING);
                        output.accept(ModBlocks.UPGRADED_BIRCH_SAPLING);
                        output.accept(ModBlocks.UPGRADED_JUNGLE_SAPLING);
                        output.accept(ModBlocks.UPGRADED_ACACIA_SAPLING);
                        output.accept(ModBlocks.UPGRADED_DARK_OAK_SAPLING);
                        output.accept(ModBlocks.UPGRADED_CHERRY_SAPLING);

                        output.accept(ModBlocks.STAR_HERB);

                        // -------------Foods-------------
                        output.accept(ModItems.ELDERBERRY);
                        output.accept(ModItems.DRAGON_FRUIT);
                        output.accept(ModItems.IRON_ROOTS);
                        output.accept(ModItems.DEVIL_FRUIT);
                        output.accept(ModItems.THE_FIRST_APPLE);
                        output.accept(ModItems.BLOOD_LOTUS);
                        output.accept(ModItems.MAGIC_MUSHROOM);
                        output.accept(ModItems.FOUR_LEAF_CLOVER);
                        output.accept(ModItems.MIRACLE_SEAWEED);
                        output.accept(ModItems.CALCIUM_CURRANT);

                        // -----------Vestiges-----------
                        acceptAllLevels(output, ModItems.BOUND_OF_THE_CELESTIAL_SISTERS);
                        acceptAllLevels(output, ModItems.CANDY_BAG);
                        acceptAllLevels(output, ModItems.CRIT_GLOVE);
                        acceptAllLevels(output, ModItems.DUELLANT_CORTEX);
                        acceptAllLevels(output, ModItems.HEART_OF_THE_TANK);
                        acceptAllLevels(output, ModItems.MINERS_LANTERN);
                        acceptAllLevels(output, ModItems.MIRROR_OF_THE_VOID);
                        acceptAllLevels(output, ModItems.PHOENIX_FEATHER);
                        acceptAllLevels(output, ModItems.RELIC_OF_THE_PAST);
                        acceptAllLevels(output, ModItems.SNOW_BOOTS);
                        acceptAllLevels(output, ModItems.SOLIS_BROOCH);
                        acceptAllLevels(output, ModItems.SPRING);
                        acceptAllLevels(output, ModItems.STIGMA_OF_DARKNESS);
                        acceptAllLevels(output, ModItems.STRANGE_ENDER_EYE);
                        acceptAllLevels(output, ModItems.STRIDER_SCALE);
                        acceptAllLevels(output, ModItems.WITHER_ROSE);

                        // ------------Weapons------------
                        output.accept(ModItems.JADE_SHIELD);
                        output.accept(ModItems.PANDORAS_BLADE);
                        output.accept(ModItems.ORAPHIM_BOW);
                        output.accept(ModItems.PIRATE_SABER);

                        output.accept(ModItems.ICE_SWORD);

                        output.accept(ModItems.MJOELNIR);
                        output.accept(ModItems.LAW_BREAKER);
                        output.accept(ModItems.STELLA_PERDITOR);
                        output.accept(ModItems.QILINS_WRATH);
                        output.accept(ModItems.SOLS_EMBRACE);
                        output.accept(ModItems.ARBITER_CROSSBOW);

                        output.accept(ModItems.STAFF_OF_WISE);
                        output.accept(ModItems.STAFF_OF_EARTH);
                        output.accept(ModItems.STAFF_OF_FOREST);
                        output.accept(ModItems.ONE_THOUSAND_SCREAMS);
                        output.accept(ModItems.STAFF_OF_HELL);
                        output.accept(ModItems.STAFF_OF_COSMOS);
                        output.accept(ModItems.STAFF_OF_ETERNAL_ICE);
                        output.accept(ModItems.AOE_TEST_ITEM);
                        output.accept(ModItems.STAFF_OF_VOID);
                        output.accept(ModItems.DODOCO);
                        output.accept(ModItems.BOOK_OF_AMATEUR);
                        output.accept(ModItems.BOOK_OF_APPRENTICE);
                        output.accept(ModItems.BOOK_OF_JOURNEYMAN);
                        output.accept(ModItems.BOOK_OF_WISE);
                        output.accept(ModItems.STAFF_OF_ALMIGHTY);
                        acceptAllLevels(output, ModItems.ZOMBIE_ENCYCLOPEDIA);
                        acceptAllLevels(output, ModItems.SKELETON_ENCYCLOPEDIA);
                        acceptAllLevels(output, ModItems.IRON_GOLEM_MANUAL);
                        acceptAllLevels(output, ModItems.BLAZING_PYROMANIAC_GUIDE);
                        acceptAllLevels(output, ModItems.MAGMA_COOKING_BOOK);
                        acceptAllLevels(output, ModItems.SLIMY_COOKING_BOOK);

                        // ------------Armor---------------
                        output.accept(ModItems.CRYSTAL_HELMET);
                        output.accept(ModItems.CRYSTAL_CHESTPLATE);
                        output.accept(ModItems.CRYSTAL_LEGGINGS);
                        output.accept(ModItems.CRYSTAL_BOOTS);

                        output.accept(ModItems.ANCIENT_HELMET);
                        output.accept(ModItems.ANCIENT_CHESTPLATE);
                        output.accept(ModItems.ANCIENT_LEGGINGS);
                        output.accept(ModItems.ANCIENT_BOOTS);

                        output.accept(ModItems.GILDED_NETHERRITE_HELMET);
                        output.accept(ModItems.GILDED_NETHERRITE_CHESTPLATE);
                        output.accept(ModItems.GILDED_NETHERRITE_LEGGINGS);
                        output.accept(ModItems.GILDED_NETHERRITE_BOOTS);

                        output.accept(ModItems.BLUE_ICE_HELMET);
                        output.accept(ModItems.BLUE_ICE_CHESTPLATE);
                        output.accept(ModItems.BLUE_ICE_LEGGINGS);
                        output.accept(ModItems.BLUE_ICE_BOOTS);

                        output.accept(ModItems.MOLTEN_HELMET);
                        output.accept(ModItems.MOLTEN_CHESTPLATE);
                        output.accept(ModItems.MOLTEN_LEGGINGS);
                        output.accept(ModItems.MOLTEN_BOOTS);

                        output.accept(ModItems.PRISMARINE_HELMET);
                        output.accept(ModItems.PRISMARINE_CHESTPLATE);
                        output.accept(ModItems.PRISMARINE_LEGGINGS);
                        output.accept(ModItems.PRISMARINE_BOOTS);

                    }).build());

    public static final Supplier<CreativeModeTab> ORIRI_COLLECTIBLES_TAB = CREATIVE_MODE_TAB.register(
            "creative.oriri_tab_misc",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.THE_FOOL.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "oriri_tab"))
                    .title(Component.translatable("creative.oriri_tab_misc"))
                    .displayItems((itemDisplayParameters, output) -> {

                        // -------------Arkana-------------
                        output.accept(ModItems.THE_FOOL);
                        output.accept(ModItems.THE_MAGICIAN);
                        output.accept(ModItems.THE_HIGH_PRIESTESS);
                        output.accept(ModItems.THE_EMPRESS);
                        output.accept(ModItems.THE_EMPEROR);
                        output.accept(ModItems.THE_HIEROPHANT);
                        output.accept(ModItems.THE_LOVERS);
                        output.accept(ModItems.THE_CHARIOT);
                        output.accept(ModItems.STRENGTH);
                        output.accept(ModItems.THE_HERMIT);
                        output.accept(ModItems.WHEEL_OF_FORTUNE);
                        output.accept(ModItems.JUSTICE);
                        output.accept(ModItems.THE_HANGED_MAN);
                        output.accept(ModItems.DEATH);
                        output.accept(ModItems.TEMPERANCE);
                        output.accept(ModItems.THE_DEVIL);
                        output.accept(ModItems.THE_TOWER);
                        output.accept(ModItems.THE_STAR);
                        output.accept(ModItems.THE_MOON);
                        output.accept(ModItems.THE_SUN);
                        output.accept(ModItems.JUDGEMENT);
                        output.accept(ModItems.THE_WORLD);

                    }).build());

    private static void acceptAllLevels(CreativeModeTab.Output output,
            Supplier<? extends net.minecraft.world.item.Item> itemSupplier) {
        for (int i = 1; i <= 3; i++) {
            ItemStack stack = new ItemStack(itemSupplier.get());
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putInt("oriri_level", i);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
            output.accept(stack);
        }
    }

    public static void registerItemGroups(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
        OririMod.LOGGER.info("Registering Item Groups for " + OririMod.MOD_ID);
    }
}
