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
public final class AetherFogClient {
    private AetherFogClient() {}

    private static boolean isAether(Fluid f) {
        return f == ModFluids.AETHER_SOURCE.get() || f == ModFluids.AETHER_FLOWING.get();
    }

    private static boolean isCameraInAether(ViewportEvent event) {
        var camera = event.getCamera();
        if (camera == null) return false;

        var level = Minecraft.getInstance().level;
        if (level == null) return false;

        FluidState fs = level.getFluidState(camera.getBlockPosition());
        return isAether(fs.getType());
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isCameraInAether(event)) return;

        event.setNearPlaneDistance(0.25f);
        event.setFarPlaneDistance(1f);

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isCameraInAether(event)) return;

        event.setRed(0.20f);
        event.setGreen(0.45f);
        event.setBlue(0.95f);
    }
}