// java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class KeepInventoryOnDeathEffect implements VestigeEffect {

    private final boolean keepHotbar;
    private final boolean keepMainInventory;
    private final boolean keepArmor;
    private final boolean keepOffhand;

    public KeepInventoryOnDeathEffect(boolean keepHotbar,
                                           boolean keepMainInventory,
                                           boolean keepArmor,
                                           boolean keepOffhand) {
        this.keepHotbar = keepHotbar;
        this.keepMainInventory = keepMainInventory;
        this.keepArmor = keepArmor;
        this.keepOffhand = keepOffhand;
    }

    /**
     * Wird von deinem Death-/Clone-Event-Handler aufgerufen.
     * Kopiert nur die gewünschten Slots vom alten auf den neuen Spieler.
     */
    public void copyKeptItems(ServerPlayer from, ServerPlayer to) {
        // Wenn Gamerule keepInventory aktiv ist, NICHTS machen
        if (from.level().getGameRules().getBoolean(
                net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        Inventory invFrom = from.getInventory();
        Inventory invTo = to.getInventory();

        // Schnellleiste (0..8)
        if (keepHotbar) {
            for (int slot = 0; slot < 9; slot++) {
                invTo.setItem(slot, invFrom.getItem(slot).copy());
            }
        }

        // Haupt-Inventar (9..35)
        if (keepMainInventory) {
            for (int slot = 9; slot < 36; slot++) {
                invTo.setItem(slot, invFrom.getItem(slot).copy());
            }
        }

        // Rüstung
        if (keepArmor) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    ItemStack fromStack = from.getItemBySlot(slot);
                    to.setItemSlot(slot, fromStack.copy());
                }
            }
        }

        // Offhand
        if (keepOffhand) {
            ItemStack fromOff = from.getOffhandItem();
            to.setItemSlot(EquipmentSlot.OFFHAND, fromOff.copy());
        }
    }

    @Override
    public boolean keepInventoryOnDeath(ServerPlayer player, ItemStack stack, int level) {
        // Signalisiere deinem System: dieser Effekt aktiviert eine Form
        // von Keep-Inventory (aber Vanilla-Gamerule wird vorher geprüft)
        boolean keepRule = player.level().getGameRules()
                .getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
        return !keepRule;
    }

    @Override
    public void tick(ServerPlayer player, ItemStack stack, int lvl) {
        // Kein Tick nötig
    }
}