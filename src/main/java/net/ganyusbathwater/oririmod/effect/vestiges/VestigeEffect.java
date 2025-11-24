// java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface VestigeEffect {
    default void tick(ServerPlayer player, ItemStack stack, int level) {}

    default boolean keepInventoryOnDeath(ServerPlayer player, ItemStack stack, int level) { return false; }

    default double healthBonus(ServerPlayer player, ItemStack stack, int level) { return 0.0D; }

    default float stepHeightBonus(ServerPlayer player, ItemStack stack, int level) { return 0.0F; }

    default float LuckBonus(ServerPlayer player, ItemStack stack, int level) { return 0.0F; }

    default void onDamaged(ServerPlayer player, ItemStack stack, int level, float amount) {}

    default void onRemovedFromExtraInventory(ServerPlayer player, ItemStack stack, int level) {}
}