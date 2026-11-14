package net.ganyusbathwater.oririmod.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.ganyusbathwater.oririmod.client.render.world.AbyssalDarknessManager;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Deepens unlit lightmap texels inside The Silent Depths / Sunken Trenches so expanding
 * fog around placed or held lights preserves true pitch-black shadows on unlit stone
 * instead of revealing vanilla's ~4% ambient brightness floor.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Shadow
    @Final
    private NativeImage lightPixels;

    @Inject(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"
            )
    )
    private void oririmod$suppressAbyssalLightmap(float partialTicks, CallbackInfo ci) {
        if (this.lightPixels == null) {
            return;
        }
        float suppression = AbyssalDarknessManager.getLightmapSuppression();
        if (suppression <= 0.001F) {
            return;
        }

        int floorPixel = this.lightPixels.getPixelRGBA(0, 0);
        int floorR = floorPixel & 0xFF;
        int floorG = (floorPixel >> 8) & 0xFF;
        int floorB = (floorPixel >> 16) & 0xFF;

        for (int block = 0; block < 16; block++) {
            float t = Mth.clamp((block - 0.5F) / 7.5F, 0.0F, 1.0F);
            float blockCurve = t * t * (3.0F - 2.0F * t);
            float keepScale = block == 0 ? 0.04F : (0.12F + 0.78F * blockCurve);

            int row0Pixel = this.lightPixels.getPixelRGBA(block, 0);
            float floorRemove = 1.0F - blockCurve;
            int targetR = Math.max(4, Math.round(Math.max(0.0F, (row0Pixel & 0xFF) - floorR * floorRemove) * keepScale));
            int targetG = Math.max(7, Math.round(Math.max(0.0F, ((row0Pixel >> 8) & 0xFF) - floorG * floorRemove) * keepScale));
            int targetB = Math.max(11, Math.round(Math.max(0.0F, ((row0Pixel >> 16) & 0xFF) - floorB * floorRemove) * keepScale));

            for (int sky = 0; sky < 16; sky++) {
                int abgr = this.lightPixels.getPixelRGBA(block, sky);
                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                int a = (abgr >>> 24) & 0xFF;

                int newR = Mth.clamp(Math.round(Mth.lerp(suppression, r, targetR)), 0, 255);
                int newG = Mth.clamp(Math.round(Mth.lerp(suppression, g, targetG)), 0, 255);
                int newB = Mth.clamp(Math.round(Mth.lerp(suppression, b, targetB)), 0, 255);

                this.lightPixels.setPixelRGBA(block, sky, (a << 24) | (newB << 16) | (newG << 8) | newR);
            }
        }
    }
}

