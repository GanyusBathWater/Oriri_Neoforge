package net.ganyusbathwater.oririmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.ganyusbathwater.oririmod.menu.ModMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ExtraInventoryScreen extends AbstractContainerScreen<ExtraInventoryMenu> {
    private static final int ROWS = 6; // Doppel‑Kiste
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 114 + ROWS * 18; // 222 bei 6 Reihen

    // Eigene Textur
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/extra_container.png");

    public ExtraInventoryScreen(ExtraInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.translatable("screen.oririmod.extra_inventory"));
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Textur setzen und komplett als Hintergrund zeichnen
        RenderSystem.setShaderTexture(0, BACKGROUND);
        g.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}