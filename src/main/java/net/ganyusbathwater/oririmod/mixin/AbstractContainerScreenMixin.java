package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.OririVanillaContainerRenderer;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders readable gilded/parchment container & inventory titles on all supported
 * vanilla container screens when {@link OririClientConfig#isCustomInventoryEnabled()} is true.
 * Never affects modded or Oriri's own custom container screens.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow
    protected int titleLabelX;

    @Shadow
    protected int titleLabelY;

    @Shadow
    protected int inventoryLabelX;

    @Shadow
    protected int inventoryLabelY;

    @Shadow
    @Final
    protected Component playerInventoryTitle;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomVanillaLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!OririVanillaContainerRenderer.isSupportedVanillaContainer(self)) {
            return;
        }
        OririVanillaContainerRenderer.renderVanillaLabels(
                guiGraphics, this.font,
                this.title, this.titleLabelX, this.titleLabelY,
                this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY);
        ci.cancel();
    }
}
