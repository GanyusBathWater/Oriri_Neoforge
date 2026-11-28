package net.ganyusbathwater.oririmod.client.render.layer;

import net.ganyusbathwater.oririmod.config.OririClientConfig;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Shared client helper deciding helmet vs. head-cosmetic visibility based on
 * {@link OririClientConfig#getCosmeticHelmetMode()}.
 */
public final class CosmeticVisibilityHelper {

    private CosmeticVisibilityHelper() {}

    /**
     * Returns true if the player has any Essence cosmetic equipped that adds head geometry
     * (Kitsune ears, Elven ears, Lamia ears, Mermaid headfins, or Silent Nereid headfins).
     */
    public static boolean hasHeadCosmeticEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(ModItems.KITSUNE_BELL.get()).isPresent()
                        || inv.findFirstCurio(ModItems.ELVEN_JEWLERY.get()).isPresent()
                        || inv.findFirstCurio(ModItems.LAMIA_SCALE.get()).isPresent()
                        || inv.findFirstCurio(ModItems.MERMAID_SCALE.get()).isPresent()
                        || inv.findFirstCurio(ModItems.SILENT_NEREID_SCALE.get()).isPresent())
                .orElse(false);
    }

    /**
     * True when the player's helmet armor visual should be hidden to show their mythical head cosmetic cleanly.
     */
    public static boolean shouldHideHelmet(Player player) {
        return OririClientConfig.getCosmeticHelmetMode() == OririClientConfig.CosmeticHelmetMode.HIDE_HELMET
                && hasHeadCosmeticEquipped(player);
    }

    /**
     * True when cosmetic ears/headfins should be hidden because a helmet is equipped in {@link EquipmentSlot#HEAD}.
     */
    public static boolean shouldHideCosmeticHead(Player player) {
        return OririClientConfig.getCosmeticHelmetMode() == OririClientConfig.CosmeticHelmetMode.HIDE_COSMETIC_HEAD
                && !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
    }
}
