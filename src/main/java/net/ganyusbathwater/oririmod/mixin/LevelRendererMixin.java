package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.client.render.world.CustomDimensionSpecialEffects;
import net.ganyusbathwater.oririmod.client.render.world.TimestopSkyboxRenderer;
import net.ganyusbathwater.oririmod.client.render.world.TimestopState;
import net.ganyusbathwater.oririmod.client.render.world.TornadoSkyboxRenderer;
import net.ganyusbathwater.oririmod.client.skybox.LunarSkyboxRenderer;
import net.ganyusbathwater.oririmod.client.skybox.LunarSkyboxState;
import net.ganyusbathwater.oririmod.events.disaster.ClientDisasterState;
import net.ganyusbathwater.oririmod.events.disaster.NaturalDisasterType;
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
     * Intercepts sky rendering at HEAD when the Lunar Skybox or Timestop is active.
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
        if (TimestopState.isActive()) {
            PoseStack poseStack = new PoseStack();
            poseStack.last().pose().set(modelViewMatrix);
            TimestopSkyboxRenderer.render(poseStack, projectionMatrix, partialTick);
            ci.cancel();
            return;
        }

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
     * Renders the separate Tornado tempest supercell sky layer across the entire sky.
     */
    @Inject(
        method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
        at = @At("TAIL")
    )
    private void oriri_onRenderSkyTail(
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup,
            CallbackInfo ci
    ) {
        NaturalDisasterType typeToUse = ClientDisasterState.isDisasterActive()
                ? ClientDisasterState.getActiveType()
                : ClientDisasterState.getLastActiveType();
        if (typeToUse == NaturalDisasterType.TORNADO && ClientDisasterState.getTransitionProgress() > 0.0f) {
            PoseStack poseStack = new PoseStack();
            poseStack.last().pose().set(modelViewMatrix);
            TornadoSkyboxRenderer.render(poseStack, projectionMatrix, partialTick,
                    ClientDisasterState.getTransitionProgress());
        }
    }

    /**
     * Modifies the sky color during celestial events and natural disasters (Blood Moon, Meteor Shower, etc.).
     */
    @ModifyVariable(method = "renderSky", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"), name = "vec3")
    private Vec3 oriri_modifySkyColor(Vec3 skyColor) {
        if (ClientDisasterState.isDisasterActive() || ClientDisasterState.getTransitionProgress() > 0.0f) {
            Vector3f disasterSky = ClientDisasterState.getSkyColor();
            if (disasterSky != null) {
                float prog = ClientDisasterState.getTransitionProgress();
                return new Vec3(
                        net.minecraft.util.Mth.lerp(prog, skyColor.x, (double) disasterSky.x),
                        net.minecraft.util.Mth.lerp(prog, skyColor.y, (double) disasterSky.y),
                        net.minecraft.util.Mth.lerp(prog, skyColor.z, (double) disasterSky.z)
                );
            }
        }
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && WorldEventManager.isEventActive(level, WorldEventType.ECLIPSE)) {
            return new Vec3(0.1, 0.1, 0.15);
        }
        return skyColor;
    }

    @ModifyVariable(method = "renderSky", at = @At(value = "STORE"), name = "vec32", ordinal = 0)
    private Vec3 oriri_modifySkyColorVector(Vec3 skyColor) {
        if (ClientDisasterState.isDisasterActive() || ClientDisasterState.getTransitionProgress() > 0.0f) {
            Vector3f disasterSky = ClientDisasterState.getSkyColor();
            if (disasterSky != null) {
                float prog = ClientDisasterState.getTransitionProgress();
                return new Vec3(
                        net.minecraft.util.Mth.lerp(prog, skyColor.x, (double) disasterSky.x),
                        net.minecraft.util.Mth.lerp(prog, skyColor.y, (double) disasterSky.y),
                        net.minecraft.util.Mth.lerp(prog, skyColor.z, (double) disasterSky.z)
                );
            }
        }
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && WorldEventManager.isEventActive(level, WorldEventType.ECLIPSE)) {
            return new Vec3(0.1, 0.1, 0.15);
        }
        return skyColor;
    }

    @ModifyVariable(method = "renderClouds", at = @At(value = "STORE"), name = "vec32", ordinal = 0)
    private Vec3 oriri_modifyCloudColor(Vec3 originalCloudColor) {
        if (ClientDisasterState.isDisasterActive() || ClientDisasterState.getTransitionProgress() > 0.0f) {
            Vector3f disasterSky = ClientDisasterState.getSkyColor();
            if (disasterSky != null) {
                float prog = ClientDisasterState.getTransitionProgress();
                Vector3f originalColorVec = new Vector3f((float) originalCloudColor.x, (float) originalCloudColor.y,
                        (float) originalCloudColor.z);
                originalColorVec.lerp(disasterSky, prog);
                return new Vec3(originalColorVec.x, originalColorVec.y, originalColorVec.z);
            }
        }

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
