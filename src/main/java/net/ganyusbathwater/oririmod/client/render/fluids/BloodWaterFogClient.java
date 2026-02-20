package net.ganyusbathwater.oririmod.client.render.fluids;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.fluid.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class BloodWaterFogClient {
    private BloodWaterFogClient() {
    }

    private static boolean isBloodWater(Fluid f) {
        return f == ModFluids.BLOOD_WATER_SOURCE.get() || f == ModFluids.BLOOD_WATER_FLOWING.get();
    }

    private static boolean isCameraInBloodWater(ViewportEvent event) {
        var camera = event.getCamera();
        if (camera == null)
            return false;

        var level = Minecraft.getInstance().level;
        if (level == null)
            return false;

        FluidState fs = level.getFluidState(camera.getBlockPosition());
        return isBloodWater(fs.getType());
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isCameraInBloodWater(event))
            return;

        // "Foggy vision" - starts very close and ends close
        event.setNearPlaneDistance(0.25f);
        event.setFarPlaneDistance(3.0f);

        event.setCanceled(true); // Cancel vanilla fog rendering
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isCameraInBloodWater(event))
            return;

        // Dark red color: R=0.2, G=0.0, B=0.0
        event.setRed(0.2f);
        event.setGreen(0.0f);
        event.setBlue(0.0f);
    }
}
