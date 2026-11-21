package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.OririVanillaContainerRenderer;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reskins {@link MerchantScreen} (Villager & Wandering Trader trading UI) using
 * Oriri's dark-reliquary & chromatic-edge style with crisp gilded labels while
 * preserving all trade rows, XP progress bar, discount icons, and scrolling.
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    protected MerchantScreenMixin(MerchantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Redirect(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"
            )
    )
    private void oriri$redirectMerchantBgBlit(GuiGraphics guiGraphics, ResourceLocation texture,
                                              int x, int y, int blitOffset, float uOffset, float vOffset,
                                              int uWidth, int vHeight, int textureWidth, int textureHeight) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            guiGraphics.blit(texture, x, y, blitOffset, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
            return;
        }
        OririVanillaContainerRenderer.renderVanillaContainerBg(
                guiGraphics, this, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomMerchantLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        OririVanillaContainerRenderer.renderMerchantLabels(
                guiGraphics, this.font, this.menu,
                this.title, this.playerInventoryTitle,
                this.imageWidth, this.inventoryLabelX, this.inventoryLabelY);
        ci.cancel();
    }
}
