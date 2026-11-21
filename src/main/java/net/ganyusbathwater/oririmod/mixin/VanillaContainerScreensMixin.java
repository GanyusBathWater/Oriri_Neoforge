package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.OririVanillaContainerRenderer;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the initial vanilla background texture blit across all standard and
 * workstation vanilla container screens with Oriri's dark-reliquary & chromatic-edge
 * UI when {@link OririClientConfig#isCustomInventoryEnabled()} is true, while letting
 * every vanilla overlay (flames, progress arrows, enchanting book, banner/map previews,
 * recipe grids, beacon buttons, etc.) render cleanly on top.
 */
@Mixin({
        ContainerScreen.class,
        ShulkerBoxScreen.class,
        DispenserScreen.class,
        HopperScreen.class,
        CraftingScreen.class,
        AbstractFurnaceScreen.class,
        BrewingStandScreen.class,
        ItemCombinerScreen.class,
        GrindstoneScreen.class,
        CrafterScreen.class,
        EnchantmentScreen.class,
        StonecutterScreen.class,
        LoomScreen.class,
        CartographyTableScreen.class,
        BeaconScreen.class
})
public abstract class VanillaContainerScreensMixin {

    @Redirect(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
            )
    )
    private void oriri$redirectVanillaBgBlit(GuiGraphics guiGraphics, ResourceLocation texture,
                                             int x, int y, int uOffset, int vOffset, int width, int height) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            guiGraphics.blit(texture, x, y, uOffset, vOffset, width, height);
            return;
        }
        // ContainerScreen performs two blits (top chest half at u=0,v=0 and bottom inventory half at v!=0).
        // Render the complete full-height Oriri chassis once on the first blit and skip the second half.
        if (uOffset == 0 && vOffset == 0) {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            OririVanillaContainerRenderer.renderVanillaContainerBg(
                    guiGraphics, self,
                    self.getGuiLeft(), self.getGuiTop(),
                    self.getXSize(), self.getYSize());
        }
    }
}
