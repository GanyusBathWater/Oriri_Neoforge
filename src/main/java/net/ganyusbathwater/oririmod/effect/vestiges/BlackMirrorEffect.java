// java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class BlackMirrorEffect implements VestigeEffect {

    // Eindeutiger Key für diesen Vestige-Cooldown
    public static final ResourceLocation KEY_MIRROR =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "mirror_of_the_black_sun");

    // CD pro Level, in Ticks
    public static final int[] COOLDOWN_PER_LEVEL = {
            0,
            20 * 90,
            20 * 60,
            20 * 45
    };

    @Override
    public void onDamaged(ServerPlayer player, ItemStack stack, int level, float amount) {
        // bewusst leer: die eigentliche Mirror-Logik liegt im Event (VestigeEvents.onIncomingDamage)
    }

    /**
     * Wird aus dem Damage-Event gerufen:
     * Prüft, ob der Treffer "gültig" ist und ob der Cooldown bereit ist,
     * und setzt den Cooldown neu, wenn verbraucht.
     */
    public static boolean shouldNegateDamage(ServerPlayer player, int level, float amount) {
        if (!isValidHit(player, level, amount)) return false;
        int cooldown = getCooldownForLevel(level);
        // versucht zu konsumieren; nur wenn bereit, wird CD neu gesetzt
        return VestigeCooldownManager.consume(player, KEY_MIRROR, level, cooldown, false);
    }

    /**
     * Für HUD / Visualisierung: nur "bereit" prüfen, ohne zu konsumieren.
     */
    public static boolean isReadyForVisual(Player player, int level, float maxPotentialDamage) {
        if (!isValidHit(player, level, maxPotentialDamage)) return false;
        return VestigeCooldownManager.isReady(player, KEY_MIRROR, level);
    }

    private static boolean isValidHit(Player player, int level, float amount) {
        Objects.requireNonNull(player, "player");
        if (amount <= 0.0F) return false;
        if (level <= 0 || level >= COOLDOWN_PER_LEVEL.length) return false;
        float maxHealth = player.getMaxHealth();
        // z.B. keine One-Shot-Überkills zulassen, wie du es vorher hattest
        return amount <= maxHealth;
    }

    private static int getCooldownForLevel(int level) {
        if (level <= 0 || level >= COOLDOWN_PER_LEVEL.length) return 0;
        return COOLDOWN_PER_LEVEL[level];
    }

    /**
     * Nach erfolgreichem Cheat-Death vom Event aufgerufen.
     */
    public static void applyMirrorInvuln(ServerPlayer player) {
        player.invulnerableTime = 20;
        player.hurtTime = 0;
    }
}