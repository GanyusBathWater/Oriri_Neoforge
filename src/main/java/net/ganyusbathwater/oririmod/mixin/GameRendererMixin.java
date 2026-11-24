package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.render.world.haze.GroundHazeRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks right after {@code LevelRenderer.renderLevel(...)} returns inside {@link GameRenderer#renderLevel}.
 * <p>
 * At this exact point:
 * <ul>
 *   <li>Iris's {@code finalizeLevelRendering()} has already finished ({@code isRenderingWorld == false}),
 *       so {@code GameRenderer::getPositionTexColorShader} runs pure vanilla {@code position_tex_color}
 *       with smooth 0..255 alpha instead of Iris's {@code AlphaTests.ONE_TENTH_ALPHA} gbuffer override.</li>
 *   <li>{@code mainRenderTarget} holds both the final shader-lit color image and the exact 3D scene depth
 *       buffer (prior to GUI depth clear), and behaves identically whether {@code F1} is hidden or shown.</li>
 * </ul>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(
            method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void oriri_renderPostWorldHaze(DeltaTracker deltaTracker, CallbackInfo ci) {
        GroundHazeRenderer.renderPostWorld(deltaTracker);
    }
}
