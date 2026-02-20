package net.ganyusbathwater.oririmod.client.render.world;

import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CustomDimensionSpecialEffects extends DimensionSpecialEffects {

    private float transitionProgress = 0.0f;
    private static final int TRANSITION_DURATION = 100;

    public CustomDimensionSpecialEffects() {
        super(192.0F, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        updateTransitionProgress();
        if (transitionProgress > 0) {
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            WorldEventType activeEvent = level != null ? WorldEventManager.getActiveEvent(level) : WorldEventType.NONE;
            WorldEventType targetEventType = level != null && WorldEventManager.isAnyEventActive(level) ? activeEvent
                    : WorldEventType.NONE;
            Vector3f eventColor = getEventSkyColor(targetEventType);
            Vector3f originalColor = new Vector3f((float) fogColor.x, (float) fogColor.y, (float) fogColor.z);
            originalColor.lerp(eventColor, transitionProgress);
            return new Vec3(originalColor.x, originalColor.y, originalColor.z);
        }
        return fogColor;
    }

    public float getTransitionProgress() {
        return transitionProgress;
    }

    public void updateTransitionProgress() {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null)
            return;

        int ticksRemaining = WorldEventManager.getTicksRemaining(level);
        int totalDuration = WorldEventManager.getEventDuration(level);

        if (WorldEventManager.isAnyEventActive(level) && totalDuration > 0) {
            boolean isFadingIn = (totalDuration - ticksRemaining) < TRANSITION_DURATION;
            boolean isFadingOut = ticksRemaining < TRANSITION_DURATION;

            if (isFadingIn) {
                transitionProgress = (float) (totalDuration - ticksRemaining) / TRANSITION_DURATION;
            } else if (isFadingOut) {
                transitionProgress = (float) ticksRemaining / TRANSITION_DURATION;
            } else {
                transitionProgress = 1.0f;
            }
        } else {
            if (transitionProgress > 0) {
                transitionProgress -= 1.0f / TRANSITION_DURATION;
            }
        }
        transitionProgress = Math.min(1.0f, Math.max(0.0f, transitionProgress));
    }

    public Vector3f getEventSkyColor(WorldEventType eventType) {
        return switch (eventType) {
            case BLOOD_MOON -> new Vector3f(0.6F, 0.1F, 0.1F);
            case GREEN_MOON -> new Vector3f(0.1F, 0.6F, 0.1F);
            case ECLIPSE -> new Vector3f(0.1f, 0.1f, 0.15f);
            default -> new Vector3f(1.0f, 1.0f, 1.0f);
        };
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }
}