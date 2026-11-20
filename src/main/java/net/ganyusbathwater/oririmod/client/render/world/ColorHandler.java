package net.ganyusbathwater.oririmod.client.render.world;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class ColorHandler {
    // ---------- Block Colors ----------
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0) {
                return (level != null && pos != null)
                        ? BiomeColors.getAverageFoliageColor(level, pos)
                        : FoliageColor.getDefaultColor();
            }
            return 0xFFFFFFFF;
        }, ModBlocks.ELDERBUSH_BLOCK.get());
    }

    // ---------- Item Colors ----------
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                return FoliageColor.getDefaultColor() | 0xFF000000;
            }
            return 0xFFFFFFFF;
        }, ModBlocks.ELDERBUSH_BLOCK.get());

        ItemColor mermaidScaleItem = (stack, tintIndex) -> {
            if (tintIndex == 0) {
                net.minecraft.world.item.DyeColor color = stack.get(net.minecraft.core.component.DataComponents.BASE_COLOR);
                if (color != null) return color.getTextureDiffuseColor() | 0xFF000000;
            }
            return 0xFFFFFFFF;
        };
        event.register(mermaidScaleItem, net.ganyusbathwater.oririmod.item.ModItems.MERMAID_SCALE.get());

        ItemColor kitsuneBellItem = (stack, tintIndex) -> {
            net.ganyusbathwater.oririmod.item.component.KitsuneColors colors = stack.getOrDefault(
                    net.ganyusbathwater.oririmod.item.component.ModDataComponents.KITSUNE_COLORS.get(),
                    net.ganyusbathwater.oririmod.item.component.KitsuneColors.DEFAULT);
            if (tintIndex == 1) return colors.primary() | 0xFF000000;
            if (tintIndex == 2) return colors.secondary() | 0xFF000000;
            return 0xFFFFFFFF;
        };
        event.register(kitsuneBellItem, net.ganyusbathwater.oririmod.item.ModItems.KITSUNE_BELL.get());

        ItemColor lamiaScaleItem = (stack, tintIndex) -> {
            if (tintIndex == 0) {
                return net.ganyusbathwater.oririmod.item.custom.cosmetic.LamiaScaleItem.getColor(stack) | 0xFF000000;
            }
            return 0xFFFFFFFF;
        };
        event.register(lamiaScaleItem, net.ganyusbathwater.oririmod.item.ModItems.LAMIA_SCALE.get());

        ItemColor silentNereidScaleItem = (stack, tintIndex) -> {
            if (tintIndex == 0) {
                net.ganyusbathwater.oririmod.item.component.SilentNereidColors colors = stack.getOrDefault(
                        net.ganyusbathwater.oririmod.item.component.ModDataComponents.SILENT_NEREID_COLORS.get(),
                        net.ganyusbathwater.oririmod.item.component.SilentNereidColors.DEFAULT);
                return colors.primary() | 0xFF000000;
            }
            return 0xFFFFFFFF;
        };
        event.register(silentNereidScaleItem, net.ganyusbathwater.oririmod.item.ModItems.SILENT_NEREID_SCALE.get());


        ItemColor teleporterItem = (stack, tintIndex) -> {
            if (tintIndex == 0) {
                net.minecraft.world.item.DyeColor color = stack.get(net.minecraft.core.component.DataComponents.BASE_COLOR);
                if (color != null) return color.getTextureDiffuseColor() | 0xFF000000;
            }
            return 0xFFFFFFFF;
        };
        event.register(teleporterItem, net.ganyusbathwater.oririmod.block.ModBlocks.TELEPORTER_BLOCK.get());

        ItemColor soulShardColor = (stack, tintIndex) -> {
            net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (tag.contains("captured_entity")) {
                String entityId = tag.getString("captured_entity");
                java.util.Optional<net.minecraft.world.entity.EntityType<?>> optType = net.minecraft.world.entity.EntityType.byString(entityId);
                if (optType.isPresent()) {
                    net.minecraft.world.item.SpawnEggItem egg = net.minecraft.world.item.SpawnEggItem.byId(optType.get());
                    if (egg != null) {
                        return egg.getColor(tintIndex) | 0xFF000000; // Force opaque alpha for Sodium/Iris
                    }
                }
            }
            return 0xFFFFFFFF; // Opaque white
        };
        event.register(soulShardColor, net.ganyusbathwater.oririmod.item.ModItems.SOUL_SHARD.get());
    }

    private ColorHandler() {}
}
