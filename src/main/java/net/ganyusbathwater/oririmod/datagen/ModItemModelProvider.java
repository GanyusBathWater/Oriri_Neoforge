package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        //-------------Items-------------

        basicItem(ModItems.FIRE_CRYSTAL.get());
        basicItem(ModItems.MANA_MANIFESTATION.get());
        basicItem(ModItems.MOON_STONE.get());
        basicItem(ModItems.DAMNED_SOUL.get());
        basicItem(ModItems.HOLLOW_SOUL.get());
        basicItem(ModItems.IRAS_SOUL.get());
        basicItem(ModItems.IRON_STICK.get());
        basicItem(ModItems.POWER_SOUL.get());
        basicItem(ModItems.TORTURED_SOUL.get());
        basicItem(ModItems.VOID_SOUL.get());

        //-------------Blocks------------

        simpleBlockItem(ModBlocks.DARK_SOIL_BLOCK.get());
        simpleBlockItem(ModBlocks.ELDERBUSH_BLOCK.get());
        simpleBlockItem(ModBlocks.MAGIC_BARRIER_BLOCK.get());
        simpleBlockItem(ModBlocks.MAGIC_BARRIER_CORE_BLOCK.get());
        simpleBlockItem(ModBlocks.ELDER_LOG_BLOCK.get());
        simpleBlockItem(ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get());
        simpleBlockItem(ModBlocks.CRACKED_ELDER_LOG_BLOCK.get());
        simpleBlockItem(ModBlocks.ELDER_PLANKS.get());
        simpleBlockItem(ModBlocks.ELDER_LEAVES.get());
        saplingItem(ModBlocks.ELDER_SAPLING);
        saplingItem(ModBlocks.STAR_HERB);
        simpleBlockItem(ModBlocks.ELDER_STEM_BLOCK.get());
        simpleBlockItem(ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get());
        simpleBlockItem(ModBlocks.MANA_CRYSTAL_BLOCK.get());
        simpleBlockItem(ModBlocks.MANA_CRYSTAL_CLUSTER.get());
        simpleBlockItem(ModBlocks.ELDER_LEAVES_FLOWERING.get());
        simpleBlockItem(ModBlocks.ELDER_SPORE_BLOSSOM.get());
        simpleBlockItem(ModBlocks.ELDER_STAIRS.get());
        simpleBlockItem(ModBlocks.ELDER_SLAB.get());
        simpleBlockItem(ModBlocks.ELDER_GATE.get());
        fenceItem(ModBlocks.ELDER_FENCE, ModBlocks.ELDER_PLANKS);

        //-------------Foods-------------

        basicItem(ModItems.ELDERBERRY.get());

        //-----------Artifacts-----------

        basicItem(ModItems.STRANGE_ENDER_EYE.get());
        basicItem(ModItems.BOUND_OF_THE_CELESTIAL_SISTERS.get());
        basicItem(ModItems.HEART_OF_THE_TANK.get());
        basicItem(ModItems.DUELLANT_CORTEX.get());
        basicItem(ModItems.CRIT_GLOVE.get());
        basicItem(ModItems.PHOENIX_FEATHER.get());

        //------------Weapons------------

        handheldItem(ModItems.PANDORAS_BLADE);
        bowItem(ModItems.ORAPHIM_BOW);
        handheldItem(ModItems.PIRATE_SABER);
        handheldItem(ModItems.NEBULA_PICKAXE);
        handheldItem(ModItems.ICE_SWORD);
        handheldItem(ModItems.MOLTEN_PICKAXE);
        handheldItem(ModItems.LAW_BREAKER);
        handheldItem(ModItems.STELLA_PERDITOR);
        handheldItem(ModItems.QILINS_WRATH);
        handheldItem(ModItems.SOLS_EMBRACE);
        crossbowItem(ModItems.ARBITER_CROSSBOW);
        handheldItem(ModItems.EMERALD_SWORD);
        handheldItem(ModItems.EMERALD_AXE);
        handheldItem(ModItems.EMERALD_PICKAXE);
        handheldItem(ModItems.EMERALD_SHOVEL);
        handheldItem(ModItems.EMERALD_HOE);
        handheldItem(ModItems.STAFF_OF_WISE);
        handheldItem(ModItems.STAFF_OF_EARTH);
        handheldItem(ModItems.STAFF_OF_FOREST);
        handheldItem(ModItems.STAFF_OF_COSMOS);
        handheldItem(ModItems.STAFF_OF_HELL);
        handheldItem(ModItems.STAFF_OF_VOID);
        handheldItem(ModItems.DODOCO);
        handheldItem(ModItems.ONE_THOUSAND_SCREAMS);
        handheldItem(ModItems.BOOK_OF_AMATEUR);
        handheldItem(ModItems.BOOK_OF_APPRENTICE);
        handheldItem(ModItems.BOOK_OF_JOURNEYMAN);
        handheldItem(ModItems.BOOK_OF_WISE);
        handheldItem(ModItems.STAFF_OF_ALMIGHTY);

        //------------Armor---------------

        basicItem(ModItems.CRYSTAL_HELMET.get());
        basicItem(ModItems.CRYSTAL_CHESTPLATE.get());
        basicItem(ModItems.CRYSTAL_LEGGINGS.get());
        basicItem(ModItems.CRYSTAL_BOOTS.get());

        basicItem(ModItems.ANCIENT_HELMET.get());
        basicItem(ModItems.ANCIENT_CHESTPLATE.get());
        basicItem(ModItems.ANCIENT_LEGGINGS.get());
        basicItem(ModItems.ANCIENT_BOOTS.get());

        basicItem(ModItems.GILDED_NETHERRITE_HELMET.get());
        basicItem(ModItems.GILDED_NETHERRITE_CHESTPLATE.get());
        basicItem(ModItems.GILDED_NETHERRITE_LEGGINGS.get());
        basicItem(ModItems.GILDED_NETHERRITE_BOOTS.get());

        basicItem(ModItems.BLUE_ICE_HELMET.get());
        basicItem(ModItems.BLUE_ICE_CHESTPLATE.get());
        basicItem(ModItems.BLUE_ICE_LEGGINGS.get());
        basicItem(ModItems.BLUE_ICE_BOOTS.get());

        basicItem(ModItems.MOLTEN_HELMET.get());
        basicItem(ModItems.MOLTEN_CHESTPLATE.get());
        basicItem(ModItems.MOLTEN_LEGGINGS.get());
        basicItem(ModItems.MOLTEN_BOOTS.get());

        basicItem(ModItems.PRISMARINE_HELMET.get());
        basicItem(ModItems.PRISMARINE_CHESTPLATE.get());
        basicItem(ModItems.PRISMARINE_LEGGINGS.get());
        basicItem(ModItems.PRISMARINE_BOOTS.get());

        //-------------Arkana-------------

        basicItem(ModItems.THE_FOOL.get());
        basicItem(ModItems.THE_MAGICIAN.get());
        basicItem(ModItems.THE_HIGH_PRIESTESS.get());
        basicItem(ModItems.THE_EMPRESS.get());
        basicItem(ModItems.THE_EMPEROR.get());
        basicItem(ModItems.THE_HIEROPHANT.get());
        basicItem(ModItems.THE_LOVERS.get());
        basicItem(ModItems.THE_CHARIOT.get());
        basicItem(ModItems.STRENGTH.get());
        basicItem(ModItems.THE_HERMIT.get());
        basicItem(ModItems.WHEEL_OF_FORTUNE.get());
        basicItem(ModItems.JUSTICE.get());
        basicItem(ModItems.THE_HANGED_MAN.get());
        basicItem(ModItems.DEATH.get());
        basicItem(ModItems.TEMPERANCE.get());
        basicItem(ModItems.THE_DEVIL.get());
        basicItem(ModItems.THE_TOWER.get());
        basicItem(ModItems.THE_STAR.get());
        basicItem(ModItems.THE_MOON.get());
        basicItem(ModItems.THE_SUN.get());
        basicItem(ModItems.JUDGEMENT.get());
        basicItem(ModItems.THE_WORLD.get());
    }

    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,"item/" + item.getId().getPath()));
    }

    private void bowItem(DeferredItem<?> deferredItem) {
        String name = (deferredItem.getId().getPath());

        withExistingParent(name, mcLoc("item/bow"))
                .texture("layer0", modLoc("item/" + name).toString())
                .override()
                .predicate(mcLoc("pulling"), 1.0F)
                .model(withExistingParent(name + "_pulling_0", mcLoc("item/bow"))
                        .texture("layer0", modLoc("item/" + name + "_pulling_0").toString()))
                .end()
                .override()
                .predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.65F)
                .model(withExistingParent(name + "_pulling_1", mcLoc("item/bow"))
                        .texture("layer0", modLoc("item/" + name + "_pulling_1").toString()))
                .end()
                .override()
                .predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.9F)
                .model(withExistingParent(name + "_pulling_2", mcLoc("item/bow"))
                        .texture("layer0", modLoc("item/" + name + "_pulling_2").toString()))
                .end();
    }

    private void crossbowItem(DeferredItem<?> deferredItem) {
        String name = (deferredItem.getId().getPath());

        ResourceLocation base = modLoc("item/" + name + "_standby");
        ResourceLocation pulling0 = modLoc("item/" + name + "_pulling_0");
        ResourceLocation pulling1 = modLoc("item/" + name + "_pulling_1");
        ResourceLocation pulling2 = modLoc("item/" + name + "_pulling_2");
        ResourceLocation arrow = modLoc("item/" + name + "_arrow");
        ResourceLocation firework = modLoc("item/" + name + "_firework");

        withExistingParent(name, mcLoc("item/crossbow"))
                .texture("layer0", base.toString())
                .override().predicate(mcLoc("pulling"), 1.0F).model(
                        withExistingParent(name + "_pulling_0", mcLoc("item/crossbow"))
                                .texture("layer0", pulling0.toString())
                ).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 0.58F).model(
                        withExistingParent(name + "_pulling_1", mcLoc("item/crossbow"))
                                .texture("layer0", pulling1.toString())
                ).end()
                .override().predicate(mcLoc("pulling"), 1.0F).predicate(mcLoc("pull"), 1.0F).model(
                        withExistingParent(name + "_pulling_2", mcLoc("item/crossbow"))
                                .texture("layer0", pulling2.toString())
                ).end()
                .override().predicate(mcLoc("charged"), 1.0F).model(
                        withExistingParent(name + "_arrow", mcLoc("item/crossbow"))
                                .texture("layer0", arrow.toString())
                ).end()
                .override().predicate(mcLoc("charged"), 1.0F).predicate(mcLoc("firework"), 1.0F).model(
                        withExistingParent(name + "_firework", mcLoc("item/crossbow"))
                                .texture("layer0", firework.toString())
                ).end();
    }

    public void buttonItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/button"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }


    public void wallItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/wall"))
                .texture("wall",  ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,"block/" + item.getId().getPath()));
    }
}
