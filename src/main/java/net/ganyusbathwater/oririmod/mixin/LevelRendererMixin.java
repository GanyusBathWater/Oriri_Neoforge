package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.world.CustomDimensionSpecialEffects;
import net.ganyusbathwater.oririmod.client.skybox.LunarSkyboxRenderer;
import net.ganyusbathwater.oririmod.client.skybox.LunarSkyboxState;
import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    @Mutable
    private static ResourceLocation SUN_LOCATION;

    @Shadow
    @Final
    @Mutable
    private static ResourceLocation MOON_LOCATION;

    @Unique
    private static final ResourceLocation oririmod$VANILLA_SUN_TEXTURE = ResourceLocation
            .fromNamespaceAndPath("minecraft", "textures/environment/sun.png");
    @Unique
    private static final ResourceLocation oririmod$VANILLA_MOON_TEXTURE = ResourceLocation
            .fromNamespaceAndPath("minecraft", "textures/environment/moon_phases.png");

    @Unique
    private static final ResourceLocation oririmod$BLOOD_MOON_TEXTURE = ResourceLocation
            .fromNamespaceAndPath(OririMod.MOD_ID, "textures/environment/bloodmoon_phases.png");
    @Unique
    private static final ResourceLocation oririmod$GREEN_MOON_TEXTURE = ResourceLocation
            .fromNamespaceAndPath(OririMod.MOD_ID, "textures/environment/greenmoon_phases.png");
    @Unique
    private static final ResourceLocation oririmod$ECLIPSE_SUN_TEXTURE = ResourceLocation
            .fromNamespaceAndPath(OririMod.MOD_ID, "textures/environment/eclipse_sun.png");

    @Unique
    private final CustomDimensionSpecialEffects oririmod$effects = new CustomDimensionSpecialEffects();

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void oriri_onRenderLevel(CallbackInfo ci) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null)
            return;

        if (WorldEventManager.isEventActive(level, WorldEventType.ECLIPSE)) {
            SUN_LOCATION = oririmod$ECLIPSE_SUN_TEXTURE;
        } else {
            SUN_LOCATION = oririmod$VANILLA_SUN_TEXTURE;
        }

        WorldEventType activeEvent = WorldEventManager.getActiveEvent(level);
        if (activeEvent == WorldEventType.BLOOD_MOON) {
            MOON_LOCATION = oririmod$BLOOD_MOON_TEXTURE;
        } else if (activeEvent == WorldEventType.GREEN_MOON) {
            MOON_LOCATION = oririmod$GREEN_MOON_TEXTURE;
        } else {
            MOON_LOCATION = oririmod$VANILLA_MOON_TEXTURE;
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"))
    private void oriri_updateCloudTransition(CallbackInfo ci) {
        oririmod$effects.updateTransitionProgress();
    }

    /**
     * Intercepts sky rendering at HEAD when the Lunar Skybox is active.
     *
     * When enabled, we cancel vanilla sky rendering entirely and hand off
     * to LunarSkyboxRenderer which draws the void + starfield.
     *
     * The PoseStack is constructed locally because renderSky in 1.21.1
     * does not pass one in the signature — we create a fresh one.
     * The projection matrix comes directly from the Mixin parameter.
     */
    @Inject(
        method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void oriri_onRenderSkyHead(
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup,
            CallbackInfo ci
    ) {
        if (!LunarSkyboxState.isEnabled()) return;

        // Build a PoseStack from the modelView matrix so LunarSkyboxRenderer
        // can use the standard PoseStack API.
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(modelViewMatrix);

        LunarSkyboxRenderer.render(poseStack, projectionMatrix, partialTick);

        // Cancel the entire vanilla renderSky — sun, moon, stars, sky dome, all gone.
        ci.cancel();
    }

    /**
     * When the lunar skybox is active, force the sky color seen by other
     * ModifyVariable targets to pure black so no color bleeds into fog.
     */
    @ModifyVariable(method = "renderSky", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"), name = "vec3")
    private Vec3 oriri_modifySkyColor(Vec3 skyColor) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && WorldEventManager.isEventActive(level, WorldEventType.ECLIPSE)) {
            return new Vec3(0.1, 0.1, 0.15);
        }
        return skyColor;
    }

    @ModifyVariable(method = "renderSky", at = @At(value = "STORE"), name = "vec32", ordinal = 0)
    private Vec3 oriri_modifySkyColorVector(Vec3 skyColor) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && WorldEventManager.isEventActive(level, WorldEventType.ECLIPSE)) {
            return new Vec3(0.1, 0.1, 0.15);
        }
        return skyColor;
    }

    @ModifyVariable(method = "renderClouds", at = @At(value = "STORE"), name = "vec32", ordinal = 0)
    private Vec3 oriri_modifyCloudColor(Vec3 originalCloudColor) {
        float transitionProgress = oririmod$effects.getTransitionProgress();
        if (transitionProgress > 0) {
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            WorldEventType activeEvent = level != null ? WorldEventManager.getActiveEvent(level) : WorldEventType.NONE;
            Vector3f eventColor = oririmod$effects.getEventSkyColor(activeEvent);
            Vector3f originalColorVec = new Vector3f((float) originalCloudColor.x, (float) originalCloudColor.y,
                    (float) originalCloudColor.z);

            originalColorVec.lerp(eventColor, transitionProgress);
            return new Vec3(originalColorVec.x, originalColorVec.y, originalColorVec.z);
        }
        return originalCloudColor;
    }
}