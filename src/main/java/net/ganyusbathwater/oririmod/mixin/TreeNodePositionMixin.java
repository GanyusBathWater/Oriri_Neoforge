package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.item.custom.ArcanaItem;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arranges the Major Arcana advancement tree ({@code oririmod:arcana/root}) into a true
 * pixel-circular clock wheel:
 * <ul>
 *   <li>Center: {@code oririmod:arcana/root} (starting point)</li>
 *   <li>Ring slots 1..22 clockwise: {@code The Fool} (0) through {@code The World} (XXI)</li>
 *   <li>Ring slot 23 (12 o'clock, directly above center): {@code Master of the Major Arcana}</li>
 * </ul>
 */
@Mixin(TreeNodePosition.class)
public abstract class TreeNodePositionMixin {

    // Exact pixel coordinates of the wheel center (keeps minX/minY >= 16px for clean clamping)
    private static final int WHEEL_CENTER_PX_X = 152;
    private static final int WHEEL_CENTER_PX_Y = 150;
    // Pixel radius chosen so 23 slots have ~36px center-to-center spacing (26px icon + 10px gap)
    private static final double WHEEL_PIXEL_RADIUS = 132.0D;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private static void oriri$layoutMajorArcanaCircle(AdvancementNode root, CallbackInfo ci) {
        if (!ArcanaItem.ROOT_ADVANCEMENT_ID.equals(root.holder().id())) {
            return;
        }

        // Starting node right in the middle of the circle.
        // Add +0.5px before dividing by 28/27 so AdvancementWidget's Mth.floor(x*28) / Mth.floor(y*27)
        // lands on the exact target integer pixel with perfect left/right symmetry.
        root.advancement().display().ifPresent(d -> d.setLocation(
                (WHEEL_CENTER_PX_X + 0.5F) / 28.0F,
                (WHEEL_CENTER_PX_Y + 0.5F) / 27.0F));

        ArcanaItem.ArcanaType[] types = ArcanaItem.ArcanaType.values();
        int totalRingSlots = types.length + 1; // 22 Major Arcana + 1 Mastery slot at 12 o'clock

        for (AdvancementNode child : root.children()) {
            ResourceLocation id = child.holder().id();
            int clockSlot = resolveClockSlot(id, types, totalRingSlots);
            if (clockSlot < 0) {
                continue;
            }

            // Screen coords (+X right, +Y down): -PI/2 is 12 o'clock (directly above center).
            // Increasing angle moves clockwise; slot 1 starts just past 12 o'clock and
            // slot == totalRingSlots lands exactly at 12 o'clock (-PI/2 + 2*PI).
            double angle = -Math.PI * 0.5D + (Math.PI * 2.0D * clockSlot) / totalRingSlots;
            int px = WHEEL_CENTER_PX_X + (int) Math.round(Math.cos(angle) * WHEEL_PIXEL_RADIUS);
            int py = WHEEL_CENTER_PX_Y + (int) Math.round(Math.sin(angle) * WHEEL_PIXEL_RADIUS);

            float x = (px + 0.5F) / 28.0F;
            float y = (py + 0.5F) / 27.0F;

            child.advancement().display().ifPresent(d -> d.setLocation(x, y));
        }

        ci.cancel();
    }

    private static int resolveClockSlot(ResourceLocation id, ArcanaItem.ArcanaType[] types, int totalRingSlots) {
        if (ArcanaItem.MASTERY_ADVANCEMENT_ID.equals(id)) {
            return totalRingSlots; // 12 o'clock (last position, directly above center)
        }
        for (int i = 0; i < types.length; i++) {
            if (types[i].getAdvancementId().equals(id)) {
                return i + 1; // 1..22 clockwise from 12 o'clock
            }
        }
        return -1;
    }
}
