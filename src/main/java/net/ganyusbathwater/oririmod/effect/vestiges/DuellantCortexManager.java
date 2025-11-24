// java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DuellantCortexManager {

    private static final Map<UUID, Integer> HOSTILE_COUNT = new HashMap<>();

    // einfacher Client-Cache (nur LocalPlayer nutzen)
    private static int CLIENT_HOSTILE_COUNT = 0;

    private DuellantCortexManager() {}

    public static void setHostileCount(ServerPlayer player, int count) {
        int clamped = Math.max(0, count);
        HOSTILE_COUNT.put(player.getUUID(), clamped);
    }

    public static int getHostileCount(ServerPlayer player) {
        int value = HOSTILE_COUNT.getOrDefault(player.getUUID(), 0);
        return value;
    }

    public static void clear(ServerPlayer player) {
        HOSTILE_COUNT.remove(player.getUUID());
    }

    // --- Client-API ---

    public static void setClientHostileCount(int count) {
        CLIENT_HOSTILE_COUNT = Math.max(0, count);
    }

    public static int getClientHostileCount() {
        return CLIENT_HOSTILE_COUNT;
    }
}