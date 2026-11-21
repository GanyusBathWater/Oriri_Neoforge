package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.OririInventoryGuiRenderer;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional {@code @Pseudo} client Mixin for Curios API's {@code CuriosScreen}.
 * Reskins both the cloned 176×166 player inventory panel and Curios's dynamic left-side
 * curio slot panel (plus optional cosmetic tab) when {@link OririClientConfig#isCustomInventoryEnabled()} is true.
 * Safely ignored if Curios is not installed.
 */
@Pseudo
@Mixin(targets = "top.theillusivec4.curios.client.gui.CuriosScreen", remap = false)
public abstract class CuriosScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {

    protected CuriosScreenMixin(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * Inject right before Curios's first {@code guiGraphics.blit(INVENTORY_LOCATION, ...)} call
     * so Curios still updates its internal {@code scrollCooldown} and {@code panelWidth} first,
     * then we draw our Oriri reliquary chassis + Curios wing with zero vanilla bleed-through.
     */
    @Inject(
            method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 0
            ),
            cancellable = true,
            require = 0
    )
    private void oriri$renderCustomCuriosBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        OririInventoryGuiRenderer.renderCuriosBg(
                guiGraphics,
                this.leftPos,
                this.topPos,
                this.imageHeight,
                mouseX,
                mouseY,
                this.minecraft != null ? this.minecraft.player : null,
                this.menu.slots,
                this.renderables
        );
        ci.cancel();
    }

    @Inject(
            method = "renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void oriri$renderCustomCuriosLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        if (this.minecraft != null && this.minecraft.player != null) {
            OririInventoryGuiRenderer.renderSurvivalTitle(guiGraphics, this.font, this.title, 97, 6);
        }
        ci.cancel();
    }
}
