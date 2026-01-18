package net.ganyusbathwater.oririmod.events.world;

import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class WorldEventManager extends SavedData {
    private static final String DATA_NAME = "oririmod_world_events";
    private static WorldEventType activeEvent = WorldEventType.NONE;
    private static int ticksRemaining = 0;
    private static int eventDuration = 0;

    /**
     * Aktualisiert das aktive Event auf dem Client. Wird vom Netzwerk-Handler aufgerufen.
     */
    public static void updateEvent(WorldEventType eventType, int newTicksRemaining, int duration) {
        activeEvent = eventType;
        ticksRemaining = newTicksRemaining;
        eventDuration = duration;
    }

    /**
     * Abwärtskompatible tick\-Methode ohne Level (bestehendes Verhalten).
     * Wenn möglich sollte die tick(ServerLevel) Variante vom Tick\-Handler aufgerufen werden.
     */
    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        } else if (activeEvent != WorldEventType.NONE) {
            activeEvent = WorldEventType.NONE;
            eventDuration = 0;
        }
    }

    /**
     * Serverseitiger Tick mit Level: prüft Dimension und Tageszeit und beendet Event sofort,
     * falls es nicht mehr zur aktuellen Tageszeit passt oder nicht in der Overworld läuft.
     */
    public static void tick(ServerLevel level) {
        // Wenn kein Event aktiv, nichts zu tun
        if (activeEvent == WorldEventType.NONE) return;

        // Events dürfen nur in der Overworld laufen -> sonst stoppen
        if (!isOverworld(level)) {
            WorldEventManager manager = get(level);
            manager.stopEvent(level);
            return;
        }

        // Falls die Tageszeit nicht mehr passt -> stoppen
        if (!isValidTimeForEvent(activeEvent, level)) {
            WorldEventManager manager = get(level);
            manager.stopEvent(level);
            return;
        }

        // Normales Tick-Verhalten
        if (ticksRemaining > 0) {
            ticksRemaining--;
        } else {
            WorldEventManager manager = get(level);
            manager.stopEvent(level);
        }
    }

    /**
     * Prüft, ob ein bestimmtes Event aktiv ist.
     */
    public static boolean isEventActive(WorldEventType eventType) {
        return activeEvent == eventType && ticksRemaining > 0;
    }

    /**
     * Prüft, ob irgendein Event aktiv ist.
     */
    public static boolean isAnyEventActive() {
        return activeEvent != WorldEventType.NONE && ticksRemaining > 0;
    }

    public static WorldEventType getActiveEvent() {
        return activeEvent;
    }

    public static int getTicksRemaining() {
        return ticksRemaining;
    }

    public static int getEventDuration() {
        return eventDuration;
    }

    /**
     * Versucht ein Event zu starten. Gibt true zurück, wenn gestartet, false sonst
     * (bereits aktiv, falsche Tageszeit oder nicht in Overworld).
     */
    public boolean startEvent(WorldEventType type, int durationTicks, ServerLevel level) {
        if (activeEvent != WorldEventType.NONE) {
            return false; // Verhindert das Überschreiben eines aktiven Events
        }
        if (!isOverworld(level)) {
            return false; // Nur Overworld erlaubt
        }
        if (!isValidTimeForEvent(type, level)) {
            return false; // Falsche Tageszeit
        }
        activeEvent = type;
        ticksRemaining = durationTicks;
        eventDuration = durationTicks; // Setzt die Gesamtdauer des Events
        setDirty();
        NetworkHandler.sendWorldEventToAll(activeEvent, ticksRemaining, eventDuration);
        return true;
    }

    public void stopEvent(ServerLevel level) {
        activeEvent = WorldEventType.NONE;
        ticksRemaining = 0;
        eventDuration = 0; // Setzt die Gesamtdauer zurück
        setDirty();
        NetworkHandler.sendWorldEventToAll(WorldEventType.NONE, 0, 0);
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag, HolderLookup.Provider pProvider) {
        pCompoundTag.putString("activeEvent", activeEvent.name());
        pCompoundTag.putInt("ticksRemaining", ticksRemaining);
        pCompoundTag.putInt("eventDuration", eventDuration); // Speichert die Gesamtdauer des Events
        return pCompoundTag;
    }

    public static WorldEventManager load(CompoundTag pCompoundTag, HolderLookup.Provider pProvider) {
        WorldEventManager manager = new WorldEventManager();
        activeEvent = WorldEventType.valueOf(pCompoundTag.getString("activeEvent"));
        ticksRemaining = pCompoundTag.getInt("ticksRemaining");
        eventDuration = pCompoundTag.getInt("eventDuration"); // Lädt die Gesamtdauer des Events
        return manager;
    }

    public static WorldEventManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(new Factory<>(WorldEventManager::new, WorldEventManager::load, DataFixTypes.SAVED_DATA_MAP_DATA), DATA_NAME);
    }

    /**
     * Bestimmt, ob das Event zur aktuellen Tageszeit gestartet/weiterlaufen darf.
     * Zusätzlich wird geprüft, ob das Level die Overworld ist.
     */
    private static boolean isValidTimeForEvent(WorldEventType type, ServerLevel level) {
        if (type == null || type == WorldEventType.NONE) return false;
        if (!isOverworld(level)) return false; // nur Overworld erlaubt
        long timeOfDay = level.getDayTime() % 24000L;

        switch (type) {
            case BLOOD_MOON:
                // Nachtfenster: ~13000 - 23000
                return timeOfDay >= 13000L && timeOfDay < 23000L;
            case ECLIPSE:
                // Tagfenster: ~0 - 12000
                return timeOfDay >= 0L && timeOfDay < 12000L;
            // Weitere Events hier mit passenden Zeitfenstern ergänzen
            default:
                return true; // Standard: jederzeit erlaubt (aber nur Overworld wegen oben)
        }
    }

    /**
     * Prüft, ob das angegebene Level die Overworld ist.
     */
    private static boolean isOverworld(ServerLevel level) {
        return level != null && level.dimension() == Level.OVERWORLD;
    }
}