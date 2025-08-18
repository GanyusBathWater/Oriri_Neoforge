package net.ganyusbathwater.oririmod.util;


import net.minecraft.world.entity.player.Player;

public class ModManaUtil {
    private static final String MANA_KEY = "mana";
    private static final int MAX_MANA = 100;

    public static int getMana(Player player) {
        return player.getPersistentData().getInt(MANA_KEY);
    }

    public static void setMana(Player player, int mana) {
        int clamped = Math.max(0, Math.min(MAX_MANA, mana));
        player.getPersistentData().putInt(MANA_KEY, clamped);
    }

    public static boolean hasEnoughMana(Player player, int amount) {
        return getMana(player) >= amount;
    }

    public static void reduceMana(Player player, int amount) {
        setMana(player, getMana(player) - amount);
    }

    public static void restoreMana(Player player, int amount) {
        setMana(player, getMana(player) + amount);
    }
}
