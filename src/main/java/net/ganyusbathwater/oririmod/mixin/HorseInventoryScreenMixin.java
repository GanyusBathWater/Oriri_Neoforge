package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.OririVanillaContainerRenderer;
import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reskins {@link HorseInventoryScreen} (Horse, Donkey, Mule, Llama, Camel) with
 * Oriri's dark reliquary & chromatic-edge look so vanilla's grey saddle/armor/chest
 * slot sprites don't clash with the dark chassis.
 */
@Mixin(HorseInventoryScreen.class)
public abstract class HorseInventoryScreenMixin extends AbstractContainerScreen<HorseInventoryMenu> {

    @Shadow
    @Final
    private AbstractHorse horse;

    @Shadow
    @Final
    private int inventoryColumns;

    @Shadow
    private float xMouse;

    @Shadow
    private float yMouse;

    protected HorseInventoryScreenMixin(HorseInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomHorseBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!OririClientConfig.isCustomInventoryEnabled()) {
            return;
        }
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        OririVanillaContainerRenderer.renderHorseBg(
                guiGraphics, (HorseInventoryScreen) (Object) this,
                x, y, this.imageWidth, this.imageHeight,
                this.horse, this.inventoryColumns, this.xMouse, this.yMouse);
        ci.cancel();
    }
}
