// java
package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class VestigeAttributeApplier {

    private static final ResourceLocation STEP_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_step_bonus");

    private static final ResourceLocation HEALTH_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_health_bonus");

    private VestigeAttributeApplier() {}

    public static void apply(ServerPlayer player, VestigeBonusSnapshot bonuses) {
        applyHealthBonus(player, bonuses.healthBonus());
        applyStepBonus(player, bonuses.stepHeightBonus());
    }

    private static void applyHealthBonus(ServerPlayer player, double healthBonus) {
        AttributeInstance healthInst = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthInst == null) return;

        // alten eigenen Modifier entfernen
        AttributeModifier old = healthInst.getModifier(HEALTH_BONUS_ID);
        if (old != null) {
            healthInst.removeModifier(old);
        }

        // neuen setzen, falls nötig
        if (healthBonus > 0.0D) {
            AttributeModifier mod = new AttributeModifier(
                    HEALTH_BONUS_ID,
                    healthBonus,
                    AttributeModifier.Operation.ADD_VALUE
            );
            healthInst.addTransientModifier(mod);
        }

        // WICHTIG: kein setHealth / kein Clamp hier!
    }

    private static void applyStepBonus(ServerPlayer player, float stepBonus) {
        AttributeInstance stepInst = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepInst == null) return;

        AttributeModifier old = stepInst.getModifier(STEP_BONUS_ID);
        if (old != null) {
            stepInst.removeModifier(old);
        }

        if (stepBonus > 0.0F) {
            AttributeModifier mod = new AttributeModifier(
                    STEP_BONUS_ID,
                    (double) stepBonus,
                    AttributeModifier.Operation.ADD_VALUE
            );
            stepInst.addTransientModifier(mod);
        }
    }
}