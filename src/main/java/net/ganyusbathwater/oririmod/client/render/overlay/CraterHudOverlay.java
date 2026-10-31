package net.ganyusbathwater.oririmod.client.render.overlay;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.CraterWorkerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class CraterHudOverlay {

    private CraterHudOverlay() {
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
}
