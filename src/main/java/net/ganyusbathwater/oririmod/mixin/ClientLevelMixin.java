package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    /**
     * Nur die Tages\-Skyhelligkeit dämpfen: injiziere am RETURN von getSkyDarken
     * und erhöhe den Dunkelheitswert nur bei aktivem ECLIPSE, Tageszeit und wenn
     * keine GUI offen ist.
     */
    @Inject(method = "getSkyDarken(F)F", at = @At("RETURN"), cancellable = true)
    private void oriri_modifySkyDarken(float partialTick, CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValueF();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && WorldEventManager.isEventActive(mc.level, WorldEventType.ECLIPSE)
                && mc.level.isDay()) {
            cir.setReturnValue(Math.min(original + 0.2F, 0.2F));
        }
    }

    /**
     * Override sky color to support shader packs during celestial events.
     * Iris/Oculus reads the sky color from here and passes it to the shader as a uniform.
     */
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void oriri_overrideSkyColorForShaders(net.minecraft.world.phys.Vec3 pPos, float pPartialTick, CallbackInfoReturnable<net.minecraft.world.phys.Vec3> cir) {
        net.ganyusbathwater.oririmod.util.ShaderCompatMode mode = net.ganyusbathwater.oririmod.config.OririConfig.COMMON.worldEvents.overrideVanillaSkyColorForShaders.get();
        boolean shouldOverride = false;
        
        if (mode == net.ganyusbathwater.oririmod.util.ShaderCompatMode.ON) {
            shouldOverride = true;
        } else if (mode == net.ganyusbathwater.oririmod.util.ShaderCompatMode.DYNAMIC) {
            shouldOverride = net.ganyusbathwater.oririmod.util.ShaderDetector.isShaderActive();
        }

        if (shouldOverride) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                if (WorldEventManager.isEventActive(mc.level, WorldEventType.BLOOD_MOON)) {
                    cir.setReturnValue(new net.minecraft.world.phys.Vec3(0.6, 0.0, 0.0));
                } else if (WorldEventManager.isEventActive(mc.level, WorldEventType.GREEN_MOON)) {
                    cir.setReturnValue(new net.minecraft.world.phys.Vec3(0.0, 0.6, 0.0));
                } else if (WorldEventManager.isEventActive(mc.level, WorldEventType.ECLIPSE)) {
                    cir.setReturnValue(new net.minecraft.world.phys.Vec3(0.02, 0.02, 0.02));
                }
            }
        }
    }
}