package net.ganyusbathwater.oririmod.client.render.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.CraterWorkerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class CraterHudOverlay {

    private CraterHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        AABB searchBox = mc.player.getBoundingBox().inflate(200.0D);
        List<CraterWorkerEntity> workers = mc.level.getEntitiesOfClass(CraterWorkerEntity.class, searchBox);

        float maxIntensity = 0.0f;

        for (CraterWorkerEntity worker : workers) {
            // Only flash during the first 2 seconds (40 ticks)
            if (worker.tickCount <= 40) {
                double distance = mc.player.distanceTo(worker);
                boolean inBlastZone = distance < worker.getRadius() + 5.0;

                net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
                net.minecraft.world.phys.Vec3 toCrater = worker.position().subtract(eye).normalize();
                net.minecraft.world.phys.Vec3 look = mc.player.getViewVector(1.0f);
                double dot = look.dot(toCrater);
                boolean isLooking = dot > 0.2;

                if (inBlastZone || isLooking) {
                    // Check Line of Sight
                    net.minecraft.world.phys.HitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(eye, worker.position(), net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
                    boolean hasLineOfSight = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getLocation().distanceTo(worker.position()) < 15.0;

                    if (inBlastZone || hasLineOfSight) {
                        float progress = (float) worker.tickCount / 40.0f;
                        float intensity = 1.0f - progress;
                        if (intensity > maxIntensity) {
                            maxIntensity = intensity;
                        }
                    }
                }
            }
        }

        if (maxIntensity > 0.0f) {
            renderWhiteout(event.getGuiGraphics(), maxIntensity);
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        AABB searchBox = mc.player.getBoundingBox().inflate(128.0D);
        List<CraterWorkerEntity> workers = mc.level.getEntitiesOfClass(CraterWorkerEntity.class, searchBox);
        
        float shakeAmount = 0;
        for (CraterWorkerEntity worker : workers) {
            // First 3 seconds (60 ticks)
            if (worker.tickCount <= 60) {
                double dist = mc.player.distanceTo(worker);
                if (dist < 100.0) {
                    float intensity = 1.0f - ((float) worker.tickCount / 60.0f);
                    float distFalloff = (float) (1.0 - (dist / 100.0));
                    shakeAmount = Math.max(shakeAmount, intensity * distFalloff);
                }
            }
        }
        
        if (shakeAmount > 0) {
            float yawShake = (mc.player.getRandom().nextFloat() - 0.5f) * 6.0f * shakeAmount;
            float pitchShake = (mc.player.getRandom().nextFloat() - 0.5f) * 6.0f * shakeAmount;
            
            event.setYaw(event.getYaw() + yawShake);
            event.setPitch(event.getPitch() + pitchShake);
            event.setRoll(event.getRoll() + yawShake * 0.5f); // Adds a disorienting tilt
        }
    }

    private static void renderWhiteout(GuiGraphics guiGraphics, float intensity) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float alpha = intensity;
        float colorValue = intensity > 0.8f ? 1.0f : (intensity * 0.5f);

        guiGraphics.setColor(colorValue, colorValue, colorValue, alpha);
        guiGraphics.fill(0, 0, width, height, 0xFFFFFFFF);

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
