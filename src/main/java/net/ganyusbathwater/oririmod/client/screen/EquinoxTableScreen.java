package net.ganyusbathwater.oririmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.menu.EquinoxTableMenu;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class EquinoxTableScreen extends AbstractContainerScreen<EquinoxTableMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            OririMod.MOD_ID, "textures/gui/equinox_table.png");
    private static final ResourceLocation MANA_ICON = ResourceLocation.fromNamespaceAndPath(
            OririMod.MOD_ID, "textures/gui/mana_100.png");

    // The texture is 1024×1024 (4× scale).
    // The GUI content occupies approximately the top-left 700×592 pixels.
    private static final int TEX_WIDTH = 1024;
    private static final int TEX_HEIGHT = 1024;
    private static final int GUI_TEX_W = 704; // 176 * 4
    private static final int GUI_TEX_H = 664; // 166 * 4

    public EquinoxTableScreen(EquinoxTableMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = EquinoxTableMenu.IMAGE_WIDTH;
        this.imageHeight = EquinoxTableMenu.IMAGE_HEIGHT;
        // Hide the default title/inventory labels — the custom texture has its own
        this.titleLabelY = -999;
        this.inventoryLabelY = -999;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw the GUI background from the 1024×1024 texture,
        // sampling the top-left GUI_TEX_W × GUI_TEX_H region and
        // rendering it at imageWidth × imageHeight screen pixels.
        guiGraphics.blit(TEXTURE,
                leftPos, topPos, // screen position
                imageWidth, imageHeight, // screen size (destination)
                0, 0, // UV start in texture
                GUI_TEX_W, GUI_TEX_H, // UV size to sample
                TEX_WIDTH, TEX_HEIGHT); // total texture dimensions

        // ── Ghost / mana-cost display ──
        if (menu.hasRecipe()) {
            int ghostX = leftPos + EquinoxTableMenu.GHOST_X;
            int ghostY = topPos + EquinoxTableMenu.GHOST_Y;

            // Draw the mana icon at reduced opacity
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.6f);
            guiGraphics.blit(MANA_ICON,
                    ghostX, ghostY,
                    16, 16, // render at 16×16 screen pixels
                    0, 0,
                    16, 16, // sample full 16×16 from the mana icon
                    16, 16);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // ── Mana-cost tooltip on ghost slot hover ──
        if (menu.hasRecipe()) {
            int manaCost = menu.getManaCost();
            int ghostX = leftPos + EquinoxTableMenu.GHOST_X;
            int ghostY = topPos + EquinoxTableMenu.GHOST_Y;
            if (mouseX >= ghostX && mouseX < ghostX + 16 && mouseY >= ghostY && mouseY < ghostY + 16) {
                int playerMana = 0;
                boolean isCreative = false;
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    playerMana = ModManaUtil.getMana(mc.player);
                    isCreative = mc.player.isCreative();
                }
                boolean canAfford = isCreative || playerMana >= manaCost;
                String costColor = canAfford ? "§a" : "§c";
                guiGraphics.renderTooltip(font,
                        List.of(
                                Component.literal("§9Mana Cost: " + costColor + manaCost),
                                Component.literal("§7Your Mana: §b" + playerMana)),
                        java.util.Optional.empty(),
                        mouseX, mouseY);
            }
        }
    }
}
