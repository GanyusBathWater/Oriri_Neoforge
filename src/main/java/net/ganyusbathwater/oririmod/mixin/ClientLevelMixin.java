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
}