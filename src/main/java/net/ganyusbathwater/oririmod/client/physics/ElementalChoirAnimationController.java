package net.ganyusbathwater.oririmod.client.physics;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * 100% procedural animation controller for the Elemental Choir weapon.
 * Replaces the multi-controller GeckoLib JSON state machine to guarantee:
 * - Zero animation lockouts or stuck hold_on_last_frame states
 * - Smooth continuous orbit angle integration (never snaps when speeding up/slowing down)
 * - Clean alternating left-to-right and right-to-left horizontal slashes aligned to crosshair yaw
 * - Smooth blend in/out for the right-click buzzsaw special attack
 * - Immunity to tick freezes and client/server double-trigger desyncs
 */
public final class ElementalChoirAnimationController {

    private ElementalChoirAnimationController() {}

    public static final long SWING_DURATION_MS = 500L;

    private static final float IDLE_ORBIT_DEG_PER_SEC = 90.0F;     // 360 deg every 4.0s
    private static final float SPECIAL_ORBIT_DEG_PER_SEC = 340.0F; // Rapid buzzsaw spin

    private static final Map<Player, ChoirState> STATES = new WeakHashMap<>();

    private static final class ChoirState {
        long lastFrameMs = -1L;
        float orbitAngleDeg = 0.0F;

        // Smooth special-attack buzzsaw blend [0..1]
        float specialBlend = 0.0F;

        // Active swing attack state
        long swingStartMs = -1L;
        boolean swingRight = false;
        boolean wasRemoteSwinging = false;
    }

    private static ChoirState getOrCreate(Player player) {
        return STATES.computeIfAbsent(player, p -> new ChoirState());
    }

    /**
     * Triggered locally on left-click swing. Alternates left/right sweep cleanly with zero controller locking.
     */
    public static void triggerSwing(Player player, boolean isRightSweep) {
        if (player == null) return;
        ChoirState state = getOrCreate(player);
        state.swingStartMs = Util.getMillis();
        state.swingRight = isRightSweep;
    }

    /**
     * Triggered via network sync for remote players in multiplayer, alternating sweep direction automatically.
     */
    public static void triggerRemoteSwing(Player player) {
        if (player == null) return;
        ChoirState state = getOrCreate(player);
        state.swingStartMs = Util.getMillis();
        state.swingRight = !state.swingRight;
    }

    /**
     * Applies procedural bone transforms to the Elemental Choir model every render frame.
     */
    public static void applyAnimation(Player player, Item choirItem,
                                      Function<String, GeoBone> boneFinder,
                                      float partialTick) {
        GeoBone weapons = boneFinder.apply("weapons");
        GeoBone attackFolder = boneFinder.apply("animation_folder_attack");
        GeoBone earth = boneFinder.apply("earth_sword");
        GeoBone fire = boneFinder.apply("fire_sword");
        GeoBone water = boneFinder.apply("water_sword");
        GeoBone nature = boneFinder.apply("nature_sword");

        if (weapons == null || attackFolder == null) {
            return;
        }

        long nowMs = Util.getMillis();

        // Inventory / GUI / dropped item preview (no player context): gentle clean idle spin
        if (player == null) {
            float guiAngle = (float) ((nowMs * 0.09D) % 360.0D);
            resetBone(attackFolder);
            resetBone(earth);
            resetBone(fire);
            resetBone(water);
            resetBone(nature);
            setBoneFromBlockbench(weapons, 0.0F, guiAngle, 0.0F, 0.0F, 0.0F, 0.0F);
            return;
        }

        ChoirState state = getOrCreate(player);
        float dtSec = (state.lastFrameMs < 0L)
                ? 0.0F
                : Mth.clamp((nowMs - state.lastFrameMs) / 1000.0F, 0.0F, 0.05F);
        state.lastFrameMs = nowMs;

        // Fallback trigger for remote players in multiplayer whose onEntitySwing doesn't run locally
        boolean remoteSwinging = player.swinging && player.swingTime <= 1;
        if (remoteSwinging && !state.wasRemoteSwinging) {
            if (state.swingStartMs < 0L || (nowMs - state.swingStartMs) >= SWING_DURATION_MS) {
                state.swingStartMs = nowMs;
                state.swingRight = !state.swingRight;
            }
        }
        state.wasRemoteSwinging = remoteSwinging;

        // 1. Evaluate Special Attack (holding right-click buzzsaw)
        boolean isSpecialActive = player.isUsingItem() && player.getUseItem().is(choirItem);
        float specialRate = isSpecialActive ? (dtSec / 0.30F) : -(dtSec / 0.25F);
        state.specialBlend = Mth.clamp(state.specialBlend + specialRate, 0.0F, 1.0F);
        float specialEase = smoothStep(state.specialBlend);

        // 2. Evaluate Active Left/Right Swing Attack
        float attackBlend = 0.0F;
        float sweepAngleDeg = 0.0F;
        if (state.swingStartMs >= 0L) {
            long elapsed = nowMs - state.swingStartMs;
            if (elapsed >= 0L && elapsed < SWING_DURATION_MS) {
                float p = (float) elapsed / (float) SWING_DURATION_MS;
                // Snappy ramp-in (first 14%) and smooth settle-out (last 22%) back into idle orbit
                float inFactor = Mth.clamp(p / 0.14F, 0.0F, 1.0F);
                float outFactor = Mth.clamp((1.0F - p) / 0.22F, 0.0F, 1.0F);
                attackBlend = smoothStep(Math.min(inFactor, outFactor));

                // Smooth eased 180-degree horizontal slash across the player's front
                float sweepEase = smoothStep(p);
                sweepAngleDeg = state.swingRight
                        ? Mth.lerp(sweepEase, 0.0F, -180.0F)   // Right-to-left sweep
                        : Mth.lerp(sweepEase, -180.0F, 0.0F);  // Left-to-right sweep
            } else {
                state.swingStartMs = -1L;
            }
        }

        // If a swing is active, smoothly yield special pose so they never fight over the same bones
        float effectiveSpecial = specialEase * (1.0F - attackBlend);

        // 3. Integrate Continuous Idle / Buzzsaw Orbit Angle (pauses smoothly during swings)
        float omega = Mth.lerp(effectiveSpecial, IDLE_ORBIT_DEG_PER_SEC, SPECIAL_ORBIT_DEG_PER_SEC);
        state.orbitAngleDeg = (state.orbitAngleDeg + omega * dtSec * (1.0F - attackBlend)) % 360.0F;

        // 4. Weapons Root Rotation: Smoothly blend between continuous orbit and crosshair-locked aim during swings
        float idleYawRad = weapons.getInitialSnapshot().getRotY() - state.orbitAngleDeg * Mth.DEG_TO_RAD;
        float headRot = Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot());
        float aimYawRad = (180.0F - headRot) * Mth.DEG_TO_RAD;

        float finalWeaponsYawRad;
        if (attackBlend > 0.001F) {
            float deltaRad = Mth.wrapDegrees((aimYawRad - idleYawRad) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            finalWeaponsYawRad = idleYawRad + deltaRad * attackBlend;
        } else {
            finalWeaponsYawRad = idleYawRad;
        }
        weapons.setRotX(weapons.getInitialSnapshot().getRotX());
        weapons.setRotY(finalWeaponsYawRad);
        weapons.setRotZ(weapons.getInitialSnapshot().getRotZ());
        weapons.setPosX(0.0F);
        weapons.setPosY(0.0F);
        weapons.setPosZ(0.0F);

        // 5. Attack Folder Sweep Rotation
        setBoneFromBlockbench(attackFolder, 0.0F, sweepAngleDeg * attackBlend, 0.0F, 0.0F, 0.0F, 0.0F);

        // 6. Individual Sword Bones (blending Idle floating bob + Special Buzzsaw + Normal Slash)
        float bobPhase = (float) ((nowMs * 0.0032D) % (Math.PI * 2.0D));
        float idleWeight = (1.0F - attackBlend) * (1.0F - effectiveSpecial);

        // Earth Sword
        applySwordPose(earth,
                0.0F, 0.0F, 0.0F,
                0.0F, Mth.sin(bobPhase) * 1.1F * idleWeight, 0.0F,
                0.0F, 0.0F, -90.0F, 5.0F, -13.0F, 0.0F, effectiveSpecial,
                0.0F, 0.0F, 110.0F, -35.0F, 6.0F, 0.0F, attackBlend);

        // Fire Sword
        applySwordPose(fire,
                0.0F, 0.0F, 0.0F,
                0.0F, Mth.sin(bobPhase + Mth.HALF_PI) * 1.1F * idleWeight, 0.0F,
                -90.0F, 0.0F, 0.0F, 0.0F, -13.0F, -5.0F, effectiveSpecial,
                0.0F, 90.0F, 110.0F, -21.0F, -1.0F, 15.0F, attackBlend);

        // Water Sword (note: water_sword has baked rest rotation [0, -90, 0] preserved via initialSnapshot)
        applySwordPose(water,
                0.0F, 0.0F, 0.0F,
                0.0F, Mth.sin(bobPhase + Mth.PI) * 1.1F * idleWeight, 0.0F,
                90.0F, 0.0F, 0.0F, -5.0F, -13.0F, 0.0F, effectiveSpecial,
                110.0F, 0.0F, 0.0F, -5.0F, -13.0F, 0.0F, attackBlend);

        // Nature Sword
        applySwordPose(nature,
                0.0F, 0.0F, 0.0F,
                0.0F, Mth.sin(bobPhase + Mth.PI * 1.5F) * 1.1F * idleWeight, 0.0F,
                90.0F, 0.0F, 0.0F, 0.0F, -13.0F, 5.0F, effectiveSpecial,
                0.0F, -90.0F, 110.0F, -21.0F, -8.0F, -14.0F, attackBlend);
    }

    private static void applySwordPose(GeoBone bone,
                                       float idleRx, float idleRy, float idleRz,
                                       float idlePx, float idlePy, float idlePz,
                                       float specRx, float specRy, float specRz,
                                       float specPx, float specPy, float specPz, float specW,
                                       float atkRx, float atkRy, float atkRz,
                                       float atkPx, float atkPy, float atkPz, float atkW) {
        if (bone == null) return;
        float rx = idleRx + specRx * specW + atkRx * atkW;
        float ry = idleRy + specRy * specW + atkRy * atkW;
        float rz = idleRz + specRz * specW + atkRz * atkW;
        float px = idlePx + specPx * specW + atkPx * atkW;
        float py = idlePy + specPy * specW + atkPy * atkW;
        float pz = idlePz + specPz * specW + atkPz * atkW;
        setBoneFromBlockbench(bone, rx, ry, rz, px, py, pz);
    }

    /**
     * Applies Blockbench-convention degrees [rx, ry, rz] and position [px, py, pz] offsets
     * onto a GeoBone while preserving its initialSnapshot rest pose.
     * GeckoLib negates X and Y rotations and negates X position relative to Blockbench JSON values.
     */
    private static void setBoneFromBlockbench(GeoBone bone,
                                              float rxDeg, float ryDeg, float rzDeg,
                                              float px, float py, float pz) {
        var init = bone.getInitialSnapshot();
        bone.setRotX(init.getRotX() - rxDeg * Mth.DEG_TO_RAD);
        bone.setRotY(init.getRotY() - ryDeg * Mth.DEG_TO_RAD);
        bone.setRotZ(init.getRotZ() + rzDeg * Mth.DEG_TO_RAD);
        bone.setPosX(-px);
        bone.setPosY(py);
        bone.setPosZ(pz);
    }

    private static void resetBone(GeoBone bone) {
        if (bone == null) return;
        var init = bone.getInitialSnapshot();
        bone.setRotX(init.getRotX());
        bone.setRotY(init.getRotY());
        bone.setRotZ(init.getRotZ());
        bone.setPosX(0.0F);
        bone.setPosY(0.0F);
        bone.setPosZ(0.0F);
    }

    private static float smoothStep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
