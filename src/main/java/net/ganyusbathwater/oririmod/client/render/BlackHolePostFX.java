package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class BlackHolePostFX {

    private static final Logger LOGGER = LogManager.getLogger("OririMod/BlackHolePostFX");

    private static final ResourceLocation EFFECT_LOCATION =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "shaders/post/blackhole_distort.json");

    private static final double ACTIVATION_RADIUS = 50.0;
    private static final double ACTIVATION_RADIUS_SQ = ACTIVATION_RADIUS * ACTIVATION_RADIUS;

    private static PostChain postChain = null;
    private static boolean activeThisFrame = false;
    private static Matrix4f capturedProjMat = new Matrix4f();
    private static Vec3 capturedBhViewPos = Vec3.ZERO;
    private static float capturedScreenX = 0.5f;
    private static float capturedScreenY = 0.5f;
    private static int cachedW = -1;
    private static int cachedH = -1;
    private static boolean loadFailed = false;

    /**
     * Checks if Oculus or Iris is loaded AND actively using a shaderpack. 
     * If so, we must disable the Vanilla PostChain to prevent pipeline conflicts and screen corruption!
     */
    public static boolean areShadersLoaded() {
        if (net.neoforged.fml.ModList.get().isLoaded("iris")) {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = apiClass.getMethod("getInstance").invoke(null);
                return (boolean) apiClass.getMethod("isShaderPackInUse").invoke(instance);
            } catch (Exception e) {
                return true; // Fallback if reflection fails
            }
        }
        if (net.neoforged.fml.ModList.get().isLoaded("oculus")) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (areShadersLoaded()) return; // Abort completely if shaders are loaded!
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 1. Capture exact screen coordinates during AFTER_ENTITIES when the PoseStack is perfectly pristine.
        // It has all camera translations/bobbing, but no entity/particle offsets have contaminated it yet.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            captureScreenPosition(event, mc);
        }

        // 2. Execute the post-chain during AFTER_LEVEL using the captured coordinates.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            executePostChain(event, mc);
        }
    }

    private static void captureScreenPosition(RenderLevelStageEvent event, Minecraft mc) {
        activeThisFrame = false;

        Entity targetEntity = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof BlackHoleEntity bh && bh.isAlive()) {
                double distSq = entity.distanceToSqr(mc.player);
                if (distSq < closestDistSq && distSq <= ACTIVATION_RADIUS_SQ) {
                    closestDistSq = distSq;
                    targetEntity = entity;
                }
            }
        }

        if (targetEntity == null) return;
        activeThisFrame = true;

        Vec3 camPos = event.getCamera().getPosition();
        
        BlackHoleEntity bh = (BlackHoleEntity) targetEntity;
        float currentRadius = bh.getCurrentRadius();
        float maxRadius = bh.getMaxRadius();
        float visualScale = (maxRadius > 0f) ? (currentRadius / maxRadius) * 1.5f : 0f;
        float modelCenterY = (13.5f / 16.0f) * visualScale;
        
        // Use EXACT interpolated position to prevent jitter
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(mc.isPaused());
        Vec3 interpolatedPos = new Vec3(
            Mth.lerp(pt, targetEntity.xo, targetEntity.getX()),
            Mth.lerp(pt, targetEntity.yo, targetEntity.getY()),
            Mth.lerp(pt, targetEntity.zo, targetEntity.getZ())
        );

        Vec3 targetPos = interpolatedPos.add(0, modelCenterY, 0);
        Vec3 relativePos = targetPos.subtract(camPos);

        // 1. Calculate View Space Position using EXACTLY Minecraft's internal view matrix logic
        org.joml.Quaternionf cameraRot = event.getCamera().rotation().conjugate(new org.joml.Quaternionf());
        Matrix4f viewMat = new Matrix4f().rotation(cameraRot);
        
        Matrix4f projMat = event.getProjectionMatrix();

        // 1. Calculate View Space Position
        Vector4f viewPos = new Vector4f((float)relativePos.x, (float)relativePos.y, (float)relativePos.z, 1.0f);
        viewMat.transform(viewPos);

        // 2. Calculate Clip Space and Screen Position
        Vector4f clipPos = new Vector4f(viewPos);
        projMat.transform(clipPos);

        float screenX = 10.0f;
        float screenY = 10.0f;
        if (clipPos.w() > 0.0f) {
            clipPos.div(clipPos.w());
            screenX = (clipPos.x() + 1.0f) / 2.0f;
            screenY = (clipPos.y() + 1.0f) / 2.0f;
        }

        capturedProjMat = new Matrix4f(projMat);
        capturedBhViewPos = new Vec3(viewPos.x(), viewPos.y(), viewPos.z());
        capturedScreenX = screenX;
        capturedScreenY = screenY;

        if (mc.player != null && mc.player.tickCount % 20 == 0) {
            System.out.println("[Oriri Debug] P11: " + projMat.m11() + " P00: " + projMat.m00() + " viewY: " + viewPos.y() + " clipY: " + clipPos.y() + " screenY: " + screenY);
        }
    }

    private static void executePostChain(RenderLevelStageEvent event, Minecraft mc) {
        if (!activeThisFrame) {
            releaseChain();
            return;
        }

        if (loadFailed) return;

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        // ── Initialize PostChain ──────────────────────────────────────────────────
        if (postChain == null || cachedW != w || cachedH != h) {
            releaseChain();
            try {
                postChain = new PostChain(
                        mc.getTextureManager(),
                        mc.getResourceManager(),
                        mc.getMainRenderTarget(),
                        EFFECT_LOCATION);
                postChain.resize(w, h);
                cachedW = w;
                cachedH = h;
            } catch (Exception e) {
                LOGGER.warn("[Oriri] Could not load black hole distortion shader: {}", e.getMessage());
                loadFailed = true;
                return;
            }
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(mc.isPaused());

        try {
            RenderSystem.assertOnRenderThread();

            // We use reflection to get the passes list, find the blackhole_distort pass, and get its EffectInstance.
            List<?> passes = getPasses(postChain);
            if (passes != null) {
                Object blackHolePass = null;
                for (Object pass : passes) {
                    // Get the name via reflection or just assume it's the first one if we can't find getName
                    String name = "";
                    for (Method m : pass.getClass().getDeclaredMethods()) {
                        if (m.getName().equals("getName")) {
                            m.setAccessible(true);
                            name = (String) m.invoke(pass);
                            break;
                        }
                    }

                    if ("oririmod:blackhole_distort".equals(name)) {
                        blackHolePass = pass;
                        break;
                    }
                }

                if (blackHolePass != null) {
                    Object effectInstance = getEffectInstance(blackHolePass);
                    if (effectInstance != null) {
                        // Helper to set Matrix4f
                        setUniformMatrix(effectInstance, "InvWorldProjMat", new Matrix4f(capturedProjMat).invert());

                        // Helper to set vec3 and vec2
                        setUniformVec3(effectInstance, "BlackHoleViewPos", (float)capturedBhViewPos.x, (float)capturedBhViewPos.y, (float)capturedBhViewPos.z);
                        setUniformVec2(effectInstance, "BlackHoleScreenPos", capturedScreenX, capturedScreenY);
                        
                        // Screen Shake is now universally handled by OririClient.java using ViewportEvent.
                        // Setting this shader uniform to 0 to prevent double-shaking without shaders.
                        float shakeX = 0.0f;
                        float shakeY = 0.0f;
                        setUniformVec2(effectInstance, "ScreenShake", shakeX, shakeY);
                    }
                }
            }

            // Store current GL state before post processing
            mc.getMainRenderTarget().unbindWrite();
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();

            postChain.process(partialTick);

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            mc.getMainRenderTarget().bindWrite(false);

        } catch (Exception e) {
            LOGGER.warn("[Oriri] Black hole PostChain.process() failed: {}", e.getMessage());
            try { mc.getMainRenderTarget().bindWrite(false); } catch (Exception ignored) { }
            releaseChain();
            loadFailed = true;
        }
    }

    private static void releaseChain() {
        if (postChain != null) {
            try { postChain.close(); } catch (Exception ignored) { }
            postChain = null;
        }
    }

    private static List<?> getPasses(PostChain chain) throws Exception {
        for (Field f : PostChain.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (List<?>) f.get(chain);
            }
        }
        return null;
    }

    private static Object getEffectInstance(Object pass) throws Exception {
        for (Method m : pass.getClass().getDeclaredMethods()) {
            if (m.getReturnType().getSimpleName().equals("EffectInstance")) {
                m.setAccessible(true);
                return m.invoke(pass);
            }
        }
        return null;
    }

    private static void setUniformMatrix(Object effectInstance, String name, Matrix4f mat) {
        try {
            Object uniform = getUniform(effectInstance, name);
            if (uniform == null) return;
            for (Method m : uniform.getClass().getDeclaredMethods()) {
                if (m.getName().equals("set") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Matrix4f.class) {
                    m.setAccessible(true);
                    m.invoke(uniform, mat);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void setUniformVec3(Object effectInstance, String name, float x, float y, float z) {
        try {
            Object uniform = getUniform(effectInstance, name);
            if (uniform == null) return;
            for (Method m : uniform.getClass().getDeclaredMethods()) {
                if (m.getName().equals("set") && m.getParameterCount() == 3 && m.getParameterTypes()[0] == float.class) {
                    m.setAccessible(true);
                    m.invoke(uniform, x, y, z);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void setUniformVec2(Object effectInstance, String name, float x, float y) {
        try {
            Object uniform = getUniform(effectInstance, name);
            if (uniform == null) return;
            for (Method m : uniform.getClass().getDeclaredMethods()) {
                if (m.getName().equals("set") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == float.class) {
                    m.setAccessible(true);
                    m.invoke(uniform, x, y);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private static Object getUniform(Object effectInstance, String name) {
        try {
            for (Method m : effectInstance.getClass().getDeclaredMethods()) {
                if (m.getName().equals("safeGetUniform") || m.getName().equals("getUniform")) {
                    m.setAccessible(true);
                    return m.invoke(effectInstance, name);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private BlackHolePostFX() { /* static utility */ }
}
