package net.ganyusbathwater.oririmod.client.render.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class SolQuicksandHudOverlay {
    private static final ResourceLocation QUICKSAND_OUTLINE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/misc/sol_quicksand_outline.png");

    private SolQuicksandHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        if (isEyeInQuicksand(mc.player)) {
            renderTexture(event.getGuiGraphics(), QUICKSAND_OUTLINE);
        }
    }

    private static boolean isEyeInQuicksand(LocalPlayer player) {
        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (player.level() != null) {
            BlockState state = player.level().getBlockState(pos);
            return state.is(ModBlocks.SOL_QUICKSAND.get());
        }
        return false;
    }

    private static void renderTexture(GuiGraphics guiGraphics, ResourceLocation texture) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Render texture over the entire screen
        guiGraphics.blit(texture, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
