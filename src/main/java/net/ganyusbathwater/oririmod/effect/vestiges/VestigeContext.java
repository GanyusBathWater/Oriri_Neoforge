package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record VestigeContext(
        Player player,
        Level level,
        ItemStack stack,
        int levelUnlocked,
        boolean isClient
) {}
