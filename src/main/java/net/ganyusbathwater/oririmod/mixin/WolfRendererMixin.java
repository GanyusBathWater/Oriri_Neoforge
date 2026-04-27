package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WolfRenderer.class)
public abstract class WolfRendererMixin {
    private static final ResourceLocation GLITZY_TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/entity/glitzy.png");

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Wolf;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void oririmod$getGlitzyTexture(Wolf wolf, CallbackInfoReturnable<ResourceLocation> cir) {
        if (wolf.isTame() && wolf.hasCustomName()) {
            String name = wolf.getCustomName().getString();
            if ("Glitzy".equalsIgnoreCase(name)) {
                cir.setReturnValue(GLITZY_TEXTURE);
            }
        }
    }
}
