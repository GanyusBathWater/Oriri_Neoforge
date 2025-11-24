// java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Zentraler Manager für alle Vestige-Cooldowns.
 * Speichert Cooldowns in Millisekunden pro Spieler, pro Vestige-Key, pro Level,
 * rechnet aber nach außen in Ticks (20 Ticks = 1 Sekunde).
 */
public final class VestigeCooldownManager {

    /**
     * Struktur:
     *  Spieler-UUID -> (Vestige-Key -> long[] perLevelNextAllowedMillis)
     *  perLevelNextAllowedMillis[level] = Zeitpunkt in System.currentTimeMillis(),
     *  ab dem die Fähigkeit wieder nutzbar ist.
     */
    private static final Map<UUID, Map<ResourceLocation, long[]>> COOLDOWNS = new HashMap<>();

    private VestigeCooldownManager() {}

    private static Map<ResourceLocation, long[]> getOrCreatePlayerMap(Player player) {
        return COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
    }

    private static long[] getOrCreateArray(Player player, ResourceLocation key, int neededLevels) {
        Map<ResourceLocation, long[]> perVestige = getOrCreatePlayerMap(player);
        long[] arr = perVestige.get(key);
        if (arr == null || arr.length < neededLevels + 1) {
            // Index 0 bleibt ungenutzt, Levels starten ab 1
            long[] newArr = new long[neededLevels + 1];
            if (arr != null) {
                System.arraycopy(arr, 0, newArr, 0, arr.length);
            }
            arr = newArr;
            perVestige.put(key, arr);
        }
        return arr;
    }

    /**
     * Prüft, ob Level bereit ist (ohne zu konsumieren).
     */
    public static boolean isReady(Player player, ResourceLocation key, int level) {
        if (level <= 0) return false;
        Map<ResourceLocation, long[]> perVestige = getOrCreatePlayerMap(player);
        long[] arr = perVestige.get(key);
        if (arr == null || level >= arr.length) return true; // kein Eintrag = bereit
        long now = System.currentTimeMillis();
        return now >= arr[level];
    }

    /**
     * Konsumiert den Cooldown für den angegebenen Level.
     * cooldownTicks: gewünschte Dauer in *Ticks* (20 Ticks = 1 Sekunde).
     * Gibt true zurück, wenn der Cooldown bereit war und jetzt gesetzt wurde.
     */
    public static boolean consume(Player player, ResourceLocation key, int level, int cooldownTicks, boolean force) {
        if (level <= 0 || cooldownTicks <= 0) return false;
        long[] arr = getOrCreateArray(player, key, level);

        long now = System.currentTimeMillis();
        long nextAllowed = arr[level];

        if (!force && now < nextAllowed) {
            return false;
        }

        long cooldownMillis = (long) cooldownTicks * 50L; // 1 Tick = 50 ms bei 20 TPS
        arr[level] = now + cooldownMillis;
        return true;
    }

    /**
     * Gibt verbleibende Ticks zurück (0 wenn bereit oder kein Eintrag).
     */
    public static int getRemaining(Player player, ResourceLocation key, int level) {
        if (level <= 0) return 0;
        Map<ResourceLocation, long[]> perVestige = getOrCreatePlayerMap(player);
        long[] arr = perVestige.get(key);
        if (arr == null || level >= arr.length) return 0;

        long now = System.currentTimeMillis();
        long nextAllowed = arr[level];
        if (now >= nextAllowed) return 0;

        long remainingMillis = nextAllowed - now;
        // zurück nach außen in "Ticks" umrechnen (nur für Anzeige/Logik)
        long remainingTicks = remainingMillis / 50L;
        if (remainingTicks <= 0) return 0;
        return remainingTicks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remainingTicks;
    }

    /**
     * Baut ein int[] für alle bekannten Level dieses Vestige-Keys (für HUD-Sync).
     * Index 0 bleibt ungenutzt.
     */
    public static int[] buildRemainingArray(Player player, ResourceLocation key, int maxLevel) {
        int[] result = new int[maxLevel + 1];
        for (int lvl = 1; lvl <= maxLevel; lvl++) {
            result[lvl] = getRemaining(player, key, lvl);
        }
        return result;
    }

    /**
     * Optional: beim Logout/Death o.ä. aufrufen, um Speicher zu leeren.
     */
    public static void clearPlayer(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
    }
}