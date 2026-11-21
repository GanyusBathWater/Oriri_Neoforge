package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.OririInventoryGuiRenderer;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optionally reskins the Creative Mode Inventory screen (chassis, item grid,
 * search bar, scrollbar, survival tab, and top/bottom creative tab buttons)
 * when {@link OririClientConfig#isCustomInventoryEnabled()} is true.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin
        extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    @Shadow
    private EditBox searchBox;

    @Shadow
    private CreativeTabsScreenPage currentPage;

    @Shadow
    protected abstract void renderTabButton(GuiGraphics guiGraphics, CreativeModeTab creativeModeTab);

    @Shadow
    private int getTabX(CreativeModeTab tab) {
        throw new AssertionError();
    }

    @Shadow
    private boolean canScroll() {
        throw new AssertionError();
    }

    protected CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu,
                                               Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomCreativeBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }

        // 1. Unselected tabs behind the main panel
        for (CreativeModeTab tab : this.currentPage.getVisibleTabs()) {
            if (tab != selectedTab) {
                this.renderTabButton(guiGraphics, tab);
            }
        }

        // Check if the selected tab sits flush on any outer corner so we suppress colliding corner filigree
        boolean selVisible = this.currentPage.getVisibleTabs().contains(selectedTab);
        boolean selTop = selVisible && this.currentPage.isTop(selectedTab);
        boolean selBot = selVisible && !selTop;
        int selRelX = selVisible ? this.getTabX(selectedTab) : -1;
        boolean selLeft = selVisible && selRelX == 0;
        boolean selRight = selVisible && (selRelX + 26 == this.imageWidth);

        // 2. Main reliquary chassis, slots, search bar, and custom scrollbar
        OririInventoryGuiRenderer.renderCreativeMainPanel(
                guiGraphics,
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight,
                selectedTab,
                this.searchBox,
                this.scrollOffs,
                this.canScroll(),
                selTop && selLeft,
                selTop && selRight,
                selBot && selLeft,
                selBot && selRight,
                mouseX,
                mouseY,
                partialTick
        );

        // 3. Selected tab drawn on top so it seamlessly merges into the chassis
        if (selVisible) {
            this.renderTabButton(guiGraphics, selectedTab);
        }

        // 4. Live 3D player model on the Survival Inventory tab
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY && this.minecraft != null) {
            OririInventoryGuiRenderer.renderCreativePlayerPreview(
                    guiGraphics, this.leftPos, this.topPos, mouseX, mouseY, this.minecraft.player);
        }

        ci.cancel();
    }

    @Inject(method = "renderTabButton", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomTabButton(GuiGraphics guiGraphics, CreativeModeTab tab, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }

        boolean selected = tab == selectedTab;
        boolean isTop = this.currentPage.isTop(tab);
        int tabX = this.leftPos + this.getTabX(tab);
        int tabY = this.topPos - (isTop ? 28 : -(this.imageHeight - 4));

        OririInventoryGuiRenderer.renderCreativeTabButton(
                guiGraphics, this.font, tab, tabX, tabY, selected, isTop,
                this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        ci.cancel();
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomCreativeLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        OririInventoryGuiRenderer.renderCreativeTabTitle(guiGraphics, this.font, selectedTab);
        ci.cancel();
    }
}
