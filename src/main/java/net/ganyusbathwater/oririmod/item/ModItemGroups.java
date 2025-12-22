package net.ganyusbathwater.oririmod.item;


import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class ModItemGroups {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OririMod.MOD_ID);

    public static final Supplier<CreativeModeTab> Oriri_TAB = CREATIVE_MODE_TAB.register("oriri_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.QILINS_WRATH.get()))
                    .title(Component.translatable("creative.oriri_tab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        //-------------Items-------------
                        output.accept(ModItems.FIRE_CRYSTAL);
                        output.accept(ModItems.MANA_MANIFESTATION);
                        output.accept(ModItems.MOON_STONE);
                        output.accept(ModItems.DAMNED_SOUL);
                        output.accept(ModItems.HOLLOW_SOUL);
                        output.accept(ModItems.IRAS_SOUL);
                        output.accept(ModItems.IRON_STICK);
                        output.accept(ModItems.POWER_SOUL);
                        output.accept(ModItems.TORTURED_SOUL);
                        output.accept(ModItems.VOID_SOUL);
                        output.accept(ModFluids.AETHER_BUCKET.get());

                        //-------------Blocks------------
                        output.accept(ModBlocks.MANA_CRYSTAL_BLOCK);
                        output.accept(ModBlocks.MANA_CRYSTAL_CLUSTER);
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
                        output.accept(ModBlocks.ELDER_SPORE_BLOSSOM);
                        output.accept(ModBlocks.STAR_HERB);

                        //-------------Foods-------------
                        output.accept(ModItems.ELDERBERRY);

                        //-----------Vestigess-----------
                        output.accept(ModItems.BOUND_OF_THE_CELESTIAL_SISTERS);
                        output.accept(ModItems.CRIT_GLOVE);
                        output.accept(ModItems.DUELLANT_CORTEX);
                        output.accept(ModItems.HEART_OF_THE_TANK);
                        output.accept(ModItems.SNOW_BOOTS);
                        output.accept(ModItems.MIRROR_OF_THE_BLACK_SUN);
                        output.accept(ModItems.PHOENIX_FEATHER);
                        output.accept(ModItems.MINERS_LANTERN);
                        output.accept(ModItems.RELIC_OF_THE_PAST);
                        output.accept(ModItems.SOLIS_BROOCH);
                        output.accept(ModItems.STIGMA_OF_THE_ARCHITECT);
                        output.accept(ModItems.SPRING);
                        output.accept(ModItems.STRIDERS_SCALE);
                        output.accept(ModItems.STRANGE_ENDER_EYE);
                        output.accept(ModItems.CANDY_BAG);
                        output.accept(ModItems.WITHER_ROSE);

                        //------------Weapons------------
                        output.accept(ModItems.PANDORAS_BLADE);
                        output.accept(ModItems.ORAPHIM_BOW);
                        output.accept(ModItems.PIRATE_SABER);
                        output.accept(ModItems.NEBULA_PICKAXE);
                        output.accept(ModItems.ICE_SWORD);
                        output.accept(ModItems.MOLTEN_PICKAXE);
                        output.accept(ModItems.MJOELNIR);
                        output.accept(ModItems.LAW_BREAKER);
                        output.accept(ModItems.STELLA_PERDITOR);
                        output.accept(ModItems.QILINS_WRATH);
                        output.accept(ModItems.SOLS_EMBRACE);
                        output.accept(ModItems.ARBITER_CROSSBOW);
                        output.accept(ModItems.EMERALD_SWORD);
                        output.accept(ModItems.EMERALD_AXE);
                        output.accept(ModItems.EMERALD_PICKAXE);
                        output.accept(ModItems.EMERALD_SHOVEL);
                        output.accept(ModItems.EMERALD_HOE);
                        output.accept(ModItems.STAFF_OF_WISE);
                        output.accept(ModItems.STAFF_OF_EARTH);
                        output.accept(ModItems.STAFF_OF_FOREST);
                        output.accept(ModItems.ONE_THOUSAND_SCREAMS);
                        output.accept(ModItems.STAFF_OF_HELL);
                        output.accept(ModItems.STAFF_OF_COSMOS);
                        output.accept(ModItems.STAFF_OF_VOID);
                        output.accept(ModItems.DODOCO);
                        output.accept(ModItems.BOOK_OF_AMATEUR);
                        output.accept(ModItems.BOOK_OF_APPRENTICE);
                        output.accept(ModItems.BOOK_OF_JOURNEYMAN);
                        output.accept(ModItems.BOOK_OF_WISE);
                        output.accept(ModItems.STAFF_OF_ALMIGHTY);

                        //------------Armor---------------

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
                        public static final Supplier<CreativeModeTab> ORIRI_COLLECTIBLES_TAB = CREATIVE_MODE_TAB.register("creative.oriri_tab_misc",
                                () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.THE_FOOL.get()))
                                        .withTabsBefore(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "oriri_tab"))
                                        .title(Component.translatable("creative.oriri_tab_misc"))
                                        .displayItems((itemDisplayParameters, output) -> {

                        //-------------Arkana-------------
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



    public static void registerItemGroups(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
        OririMod.LOGGER.info("Registering Item Groups for " + OririMod.MOD_ID);
    }
}
