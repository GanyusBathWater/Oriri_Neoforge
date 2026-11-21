package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.OririInventoryGuiRenderer;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optionally reskins the Survival Player Inventory screen using Oriri's programmatic
 * dark-reliquary & chromatic-edge style when {@link OririClientConfig#isCustomInventoryEnabled()} is true.
 * Preserves 100% compatibility with Recipe Book, status effects, EMI, and Curios.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {

    @Shadow
    private float xMouse;

    @Shadow
    private float yMouse;

    protected InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        OririInventoryGuiRenderer.renderSurvivalBg(
                guiGraphics,
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight,
                this.xMouse,
                this.yMouse,
                this.minecraft != null ? this.minecraft.player : null
        );
        ci.cancel();
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        OririInventoryGuiRenderer.renderSurvivalTitle(
                guiGraphics, this.font, this.title, this.titleLabelX, this.titleLabelY);
        ci.cancel();
    }
}
