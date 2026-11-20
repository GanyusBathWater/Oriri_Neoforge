package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

public class ModDataMapProvider extends DataMapProvider {
    protected ModDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.builder(NeoForgeDataMaps.FURNACE_FUELS)
                .add(ModBlocks.VILE_VINE_BLOCK.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.STRIPPED_VILE_VINE.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.VILE_PLANKS.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.VILE_STAIRS.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.VILE_SLAB.getId(), new FurnaceFuel(150), false)
                .add(ModBlocks.VILE_FENCE.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.VILE_GATE.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.VILE_TRAPDOOR.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.VILE_PRESSURE_PLATE.getId(), new FurnaceFuel(300), false)
                .add(ModBlocks.VILE_DOOR.getId(), new FurnaceFuel(200), false)
                .add(ModBlocks.VILE_BUTTON.getId(), new FurnaceFuel(100), false)
                .add(net.ganyusbathwater.oririmod.item.ModItems.VILE_SIGN.getId(), new FurnaceFuel(200), false)
                .add(net.ganyusbathwater.oririmod.item.ModItems.VILE_HANGING_SIGN.getId(), new FurnaceFuel(800), false)
                .add(net.ganyusbathwater.oririmod.item.ModItems.VILE_BOAT.getId(), new FurnaceFuel(1200), false)
                .add(net.ganyusbathwater.oririmod.item.ModItems.VILE_CHEST_BOAT.getId(), new FurnaceFuel(1200), false)
                .add(ModBlocks.VILE_SAPLING.getId(), new FurnaceFuel(100), false)
                .add(ModBlocks.UPGRADED_VILE_SAPLING.getId(), new FurnaceFuel(100), false);
    }
}
