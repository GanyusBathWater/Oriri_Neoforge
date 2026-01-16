package net.ganyusbathwater.oririmod.events.world;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
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
     * Reduziert die verbleibenden Ticks für das aktive Event.
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

    public void startEvent(WorldEventType type, int durationTicks, ServerLevel level) {
        if (activeEvent != WorldEventType.NONE) {
            return; // Verhindert das Überschreiben eines aktiven Events
        }
        activeEvent = type;
        ticksRemaining = durationTicks;
        eventDuration = durationTicks; // Setzt die Gesamtdauer des Events
        setDirty();
        NetworkHandler.sendWorldEventToAll(activeEvent, ticksRemaining, eventDuration);
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
}