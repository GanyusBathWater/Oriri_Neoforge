package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class AetherHudOverlay {
    // Nutzt dieselbe Overlay-Textur wie beim Fluid-Overlay, aber als HUD.
    private static final ResourceLocation AETHER_HUD =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/block/aether_overlay.png");

    private AetherHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Beispielbedingung: nur wenn der Spieler "in Aether" ist (anpassen!)
        // if (!isInAether(mc)) return;

        GuiGraphics gg = event.getGuiGraphics();
        int w = gg.guiWidth();
        int h = gg.guiHeight();

        // Größe/Position wie beim "Feuer unten" (anpassbar)
        int quadW = w / 2;
        int quadH = h / 2;
        int y = h - quadH;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Linke/rechte Hälfte mit zwei Sprites überlagern (wie Vanilla Fire-Overlay)
        drawSprite(gg, AETHER_HUD, 0, y, quadW, quadH);
        drawSprite(gg, AETHER_HUD, w - quadW, y, quadW, quadH);

        RenderSystem.disableBlend();
    }

    private static void drawSprite(GuiGraphics gg, ResourceLocation spriteId, int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        TextureAtlas atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(spriteId);

        // Atlas binden, dann das Sprite blitten (Sprite liefert u/v und ist animiert)
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        gg.blit(x, y, 0, w, h, sprite);
    }
}