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

    private static final ResourceLocation LUCK_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_luck_bonus");

    private VestigeAttributeApplier() {}

    public static void apply(ServerPlayer player, VestigeBonusSnapshot bonuses) {
        applyHealthBonus(player, bonuses.healthBonus());
        applyStepBonus(player, bonuses.stepHeightBonus());
        applyLuckBonus(player, bonuses.luckBonus());
    }

    private static void applyHealthBonus(ServerPlayer player, double healthBonus) {
        AttributeInstance healthInst = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthInst == null) return;

        AttributeModifier old = healthInst.getModifier(HEALTH_BONUS_ID);
        if (old != null) {
            healthInst.removeModifier(old);
        }

        if (healthBonus > 0.0D) {
            AttributeModifier mod = new AttributeModifier(
                    HEALTH_BONUS_ID,
                    healthBonus,
                    AttributeModifier.Operation.ADD_VALUE
            );
            healthInst.addTransientModifier(mod);
        }
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

    private static void applyLuckBonus(ServerPlayer player, float luckBonus) {
        AttributeInstance luckInst = player.getAttribute(Attributes.LUCK);
        if (luckInst == null) return;

        AttributeModifier old = luckInst.getModifier(LUCK_BONUS_ID);
        if (old != null) {
            luckInst.removeModifier(old);
        }

        if (luckBonus > 0.0F) {
            AttributeModifier mod = new AttributeModifier(
                    LUCK_BONUS_ID,
                    (double) luckBonus,
                    AttributeModifier.Operation.ADD_VALUE
            );
            luckInst.addTransientModifier(mod);
        }
    }
}