package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class KeepInventoryOnDeathEffect implements VestigeEffect {
    @Override
    public boolean keepInventoryOnDeath(ServerPlayer player, ItemStack stack, int level) {
        return true; // Aktiviert Keep-Inventory für diesen Spieler, wenn diese Stufe aktiv ist.
    }
}
