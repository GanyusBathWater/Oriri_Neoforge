package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class FuelEvents {

    @SubscribeEvent
    public static void onBurnTime(FurnaceFuelBurnTimeEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty())
            return;

        Item item = itemStack.getItem();

        // 300 Ticks (1.5 items) - Logs, Stems, Planks, Fences, Gates, Stairs
        if (item == ModBlocks.ELDER_LOG_BLOCK.get().asItem() ||
                item == ModBlocks.CRACKED_ELDER_LOG_BLOCK.get().asItem() ||
                item == ModBlocks.STRIPPED_ELDER_LOG_BLOCK.get().asItem() ||
                item == ModBlocks.ELDER_STEM_BLOCK.get().asItem() ||
                item == ModBlocks.STRIPPED_ELDER_STEM_BLOCK.get().asItem() ||
                item == ModBlocks.ELDER_PLANKS.get().asItem() ||
                item == ModBlocks.ELDER_STAIRS.get().asItem() ||
                item == ModBlocks.ELDER_FENCE.get().asItem() ||
                item == ModBlocks.ELDER_GATE.get().asItem() ||
                item == ModBlocks.SCARLET_LOG.get().asItem() ||
                item == ModBlocks.STRIPPED_SCARLET_LOG.get().asItem() ||
                item == ModBlocks.SCARLET_STEM.get().asItem() ||
                item == ModBlocks.STRIPPED_SCARLET_STEM.get().asItem() ||
                item == ModBlocks.SCARLET_PLANKS.get().asItem() ||
                item == ModBlocks.SCARLET_STAIRS.get().asItem() ||
                item == ModBlocks.SCARLET_FENCE.get().asItem() ||
                item == ModBlocks.SCARLET_GATE.get().asItem() ||
                item == ModBlocks.ABYSS_CROWN_LOG.get().asItem() ||
                item == ModBlocks.STRIPPED_ABYSS_CROWN_LOG.get().asItem() ||
                item == ModBlocks.ABYSS_CROWN_STEM.get().asItem() ||
                item == ModBlocks.STRIPPED_ABYSS_CROWN_STEM.get().asItem() ||
                item == ModBlocks.ABYSS_CROWN_PLANKS.get().asItem() ||
                item == ModBlocks.ABYSS_CROWN_STAIRS.get().asItem() ||
                item == ModBlocks.ABYSS_CROWN_FENCE.get().asItem() ||
                item == ModBlocks.ABYSS_CROWN_GATE.get().asItem()) {

            event.setBurnTime(300);
            return;
        }

        // 150 Ticks (0.75 items) - Slabs
        if (item == ModBlocks.ELDER_SLAB.get().asItem() ||
                item == ModBlocks.SCARLET_SLAB.get().asItem() ||
                item == ModBlocks.ABYSS_CROWN_SLAB.get().asItem()) {

            event.setBurnTime(150);
            return;
        }

        // 100 Ticks (0.5 items) - Saplings
        if (item == ModBlocks.ELDER_SAPLING.get().asItem() ||
                item == ModBlocks.SCARLET_SAPLING.get().asItem()) {

            event.setBurnTime(100);
        }
    }
}
