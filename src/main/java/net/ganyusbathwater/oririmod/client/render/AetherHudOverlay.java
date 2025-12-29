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
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/aether_gui");

    private AetherHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;


        if (!isInAether(mc)) return;

        GuiGraphics gg = event.getGuiGraphics();
        int w = gg.guiWidth();
        int h = gg.guiHeight();

        int quadW = w / 2;
        int quadH = (int) (h * 0.7f); // etwas niedriger als 50%
        int y = h - quadH;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Linke/rechte Hälfte mit zwei Sprites überlagern (wie Vanilla Fire-Overlay)
        drawSprite(gg, AETHER_HUD, 0, y + 20, quadW +20, quadH);
        drawSprite(gg, AETHER_HUD, w - quadW, y + 20, quadW, quadH);

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

    private static boolean isInAether(Minecraft mc) {
        Player player = mc.player;
        if (player == null || mc.level == null) return false;

        FluidState state = mc.level.getFluidState(player.blockPosition());
        // Annahme: ModFluids.AETHER ist ein RegistryObject/Supplier; .get() liefert das registrierte Fluid.
        try {
            return state.is(ModFluids.AETHER_SOURCE.get()) || state.is(ModFluids.AETHER_FLOWING.get());
        } catch (Exception e) {
            // Fallback: falls ModFluids anders definiert ist oder .get() nicht vorhanden ist, sicherer Rückfall
            return false;
        }
    }
}