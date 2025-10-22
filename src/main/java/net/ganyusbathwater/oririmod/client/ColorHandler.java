package net.ganyusbathwater.oririmod.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class ColorHandler {
    private static final int ELDERWOODS_GRASS_COLOR = 0x2072fe;
    private static final int ELDERWOODS_FOLIAGE_COLOR = ELDERWOODS_GRASS_COLOR;

    // Erwartete Dimension als ResourceKey<Level>
    private static final ResourceKey<Level> ELDERWOODS_DIM =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elderwoods"));

    private static boolean DIM_LOGGED = false; // einmaliges Debug-Logging

    // ---------- Block Colors ----------
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor grassLike = (state, level, pos, tintIndex) -> grassColor(level, pos);
        BlockColor foliageLike = (state, level, pos, tintIndex) -> foliageColor(level, pos);

        event.register(grassLike,
                Blocks.GRASS_BLOCK,
                Blocks.SHORT_GRASS,
                Blocks.TALL_GRASS,
                Blocks.FERN,
                Blocks.LARGE_FERN
        );
        event.register(foliageLike, Blocks.OAK_LEAVES);
    }

    // ---------- Item Colors ----------
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // GRASS_BLOCK: getönter Layer ist beim Item index 0
        ItemColor grassBlockItem = (stack, tintIndex) -> (tintIndex == 0) ? grassColorClient() : -1;

        // Graspflanzen: Layer 0 getönt
        ItemColor grassPlantsItem = (stack, tintIndex) -> (tintIndex == 0) ? grassColorClient() : -1;

        // Leaves: Layer 0 getönt
        ItemColor leavesItem = (stack, tintIndex) -> (tintIndex == 0) ? foliageColorClient() : -1;

        event.register(grassBlockItem, Items.GRASS_BLOCK);
        event.register(grassPlantsItem, Items.SHORT_GRASS, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN);
        event.register(leavesItem, Items.OAK_LEAVES);
    }

    // ---------- Helpers ----------
    private static int grassColor(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (isElderwoods(level)) return ELDERWOODS_GRASS_COLOR;
        if (level != null && pos != null) return BiomeColors.getAverageGrassColor(level, pos);
        return GrassColor.get(0.5D, 1.0D);
    }

    private static int foliageColor(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (isElderwoods(level)) return ELDERWOODS_FOLIAGE_COLOR;
        if (level != null && pos != null) return BiomeColors.getAverageFoliageColor(level, pos);
        return FoliageColor.getDefaultColor();
    }

    private static int grassColorClient() {
        Level lvl = Minecraft.getInstance().level;
        if (lvl != null && isElderwoods(lvl)) return ELDERWOODS_GRASS_COLOR;
        return GrassColor.get(0.5D, 1.0D);
    }

    private static int foliageColorClient() {
        Level lvl = Minecraft.getInstance().level;
        if (lvl != null && isElderwoods(lvl)) return ELDERWOODS_FOLIAGE_COLOR;
        return FoliageColor.getDefaultColor();
    }

    // Robust: vergleicht die echte Level-ID; fällt bei Render-Kontext auf Client-Level zurück
    private static boolean isElderwoods(@Nullable BlockAndTintGetter ctx) {
        Level lvl = (ctx instanceof Level l) ? l : Minecraft.getInstance().level;
        return lvl != null && isElderwoods(lvl);
    }

    private static boolean isElderwoods(Level level) {
        ResourceLocation id = level.dimension().location();
        if (!DIM_LOGGED) {
            System.out.println("[OririMod] Aktive Dimension (Client): " + id);
            DIM_LOGGED = true;
        }
        // Direkter Vergleich der Location statt ResourceKey\<Level\>.equals(...)
        return OririMod.MOD_ID.equals(id.getNamespace()) && "elderwoods".equals(id.getPath());
    }

    private ColorHandler() {}
}
