// java
package net.ganyusbathwater.oririmod.util.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class VestigeManager {
    private static final float VANILLA_STEP_HEIGHT = 0.6F;
    private static final UUID STEP_BONUS_UUID = UUID.fromString("e8b2f8ad-9f4b-4a4e-8e3a-6f2f5b59b7b1"); // ungenutzt
    private static final ResourceLocation STEP_BONUS_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_step_bonus");

    // Neu: gemeinsame ID für Health‑Bonus (muss mit HeartContainer übereinstimmen)
    private static final ResourceLocation HEALTH_BONUS_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_health_bonus");

    public static void tick(ServerPlayer player) {
        float stepBonus = 0F;

        // Health‑Bonus immer zuerst entfernen (falls kein Item aktiv ist, bleibt nichts zurück)
        var healthInst = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthInst != null) {
            healthInst.removeModifier(HEALTH_BONUS_ID);
        }

        for (ItemStack stack : ExtraInventoryUtil.readExtraInventory(player)) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof VestigeItem vestige) {
                vestige.applyTick(player, stack);
                stepBonus += vestige.sumStepHeightBonus(player, stack);
            }
        }

        var inst = player.getAttribute(Attributes.STEP_HEIGHT);
        if (inst != null) {
            inst.removeModifier(STEP_BONUS_ID);
            if (stepBonus > 0F) {
                AttributeModifier mod = new AttributeModifier(
                        STEP_BONUS_ID,
                        (double) stepBonus,
                        AttributeModifier.Operation.ADD_VALUE
                );
                inst.addTransientModifier(mod);
            }
        }

        // Gesundheit nach allen Änderungen klemmen (z. B. wenn kein HeartContainer aktiv ist)
        float max = player.getMaxHealth();
        if (player.getHealth() > max) {
            player.setHealth(max);
        }
    }

    public static boolean hasKeepInventory(ServerPlayer player) {
        for (ItemStack stack : ExtraInventoryUtil.readExtraInventory(player)) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof VestigeItem vestige) {
                if (vestige.grantsKeepInventory(player, stack)) return true;
            }
        }
        return false;
    }
}