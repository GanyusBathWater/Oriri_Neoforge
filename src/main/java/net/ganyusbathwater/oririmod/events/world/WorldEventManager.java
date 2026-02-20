package net.ganyusbathwater.oririmod.events.world;

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

    // Instanz-Variablen für Server-Daten (pro Dimension)
    private WorldEventType activeEvent = WorldEventType.NONE;
    private int ticksRemaining = 0;
    private int eventDuration = 0;

    // Statische Klasse für Client-Daten (global für den Client-Spieler)
    public static class ClientWorldEventData {
        private static WorldEventType activeEvent = WorldEventType.NONE;
        private static int ticksRemaining = 0;
        private static int eventDuration = 0;

        public static void update(WorldEventType type, int ticks, int duration) {
            activeEvent = type;
            ticksRemaining = ticks;
            eventDuration = duration;
        }
    }

    /**
     * Aktualisiert das aktive Event auf dem Client. Wird vom Netzwerk-Handler
     * aufgerufen.
     */
    public static void updateClientEvent(WorldEventType eventType, int newTicksRemaining, int duration) {
        ClientWorldEventData.update(eventType, newTicksRemaining, duration);
    }

    /**
     * Serverseitiger Tick mit Level: prüft Dimension und Tageszeit und beendet
     * Event sofort,
     * falls es nicht mehr zur aktuellen Tageszeit passt oder nicht in der Overworld
     * läuft.
     */
    public static void tick(ServerLevel level) {
        WorldEventManager manager = get(level);
        manager.tickInstance(level);
    }

    private void tickInstance(ServerLevel level) {
        // Wenn kein Event aktiv, nichts zu tun
        if (activeEvent == WorldEventType.NONE)
            return;

        // Events dürfen nur in der Overworld laufen -> sonst stoppen
        // (Sollte eigentlich gar nicht erst starten, aber zur Sicherheit)
        if (!isOverworld(level)) {
            stopEvent(level);
            return;
        }

        // Falls die Tageszeit nicht mehr passt -> stoppen
        if (!isValidTimeForEvent(activeEvent, level)) {
            stopEvent(level);
            return;
        }

        // Normales Tick-Verhalten
        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining % 20 == 0) {
                setDirty(); // Speichern sicherstellen
                NetworkHandler.sendWorldEventToDimension(level, activeEvent, ticksRemaining, eventDuration);
            }
        } else {
            stopEvent(level);
        }
    }

    /**
     * Prüft, ob ein bestimmtes Event aktiv ist (Level-abhängig).
     */
    public static boolean isEventActive(Level level, WorldEventType eventType) {
        if (level.isClientSide()) {
            return ClientWorldEventData.activeEvent == eventType && ClientWorldEventData.ticksRemaining > 0;
        }
        if (level instanceof ServerLevel serverLevel) {
            WorldEventManager manager = get(serverLevel);
            return manager.activeEvent == eventType && manager.ticksRemaining > 0;
        }
        return false;
    }

    /**
     * Prüft, ob irgendein Event aktiv ist.
     */
    public static boolean isAnyEventActive(Level level) {
        if (level.isClientSide()) {
            return ClientWorldEventData.activeEvent != WorldEventType.NONE && ClientWorldEventData.ticksRemaining > 0;
        }
        if (level instanceof ServerLevel serverLevel) {
            WorldEventManager manager = get(serverLevel);
            return manager.activeEvent != WorldEventType.NONE && manager.ticksRemaining > 0;
        }
        return false;
    }

    public static WorldEventType getActiveEvent(Level level) {
        if (level.isClientSide()) {
            return ClientWorldEventData.activeEvent;
        }
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel).activeEvent;
        }
        return WorldEventType.NONE;
    }

    public static int getTicksRemaining(Level level) {
        if (level.isClientSide()) {
            return ClientWorldEventData.ticksRemaining;
        }
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel).ticksRemaining;
        }
        return 0;
    }

    public static int getEventDuration(Level level) {
        if (level.isClientSide()) {
            return ClientWorldEventData.eventDuration;
        }
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel).eventDuration;
        }
        return 0;
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
        eventDuration = durationTicks;
        setDirty();
        NetworkHandler.sendWorldEventToDimension(level, activeEvent, ticksRemaining, eventDuration);
        return true;
    }

    public void stopEvent(ServerLevel level) {
        activeEvent = WorldEventType.NONE;
        ticksRemaining = 0;
        eventDuration = 0;
        setDirty();
        NetworkHandler.sendWorldEventToDimension(level, WorldEventType.NONE, 0, 0);
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag, HolderLookup.Provider pProvider) {
        pCompoundTag.putString("activeEvent", activeEvent.name());
        pCompoundTag.putInt("ticksRemaining", ticksRemaining);
        pCompoundTag.putInt("eventDuration", eventDuration);
        return pCompoundTag;
    }

    public static WorldEventManager load(CompoundTag pCompoundTag, HolderLookup.Provider pProvider) {
        WorldEventManager manager = new WorldEventManager();
        try {
            manager.activeEvent = WorldEventType.valueOf(pCompoundTag.getString("activeEvent"));
        } catch (IllegalArgumentException e) {
            manager.activeEvent = WorldEventType.NONE;
        }
        manager.ticksRemaining = pCompoundTag.getInt("ticksRemaining");
        manager.eventDuration = pCompoundTag.getInt("eventDuration");
        return manager;
    }

    public static WorldEventManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(WorldEventManager::new, WorldEventManager::load, DataFixTypes.SAVED_DATA_MAP_DATA),
                DATA_NAME);
    }

    /**
     * Bestimmt, ob das Event zur aktuellen Tageszeit gestartet/weiterlaufen darf.
     */
    private static boolean isValidTimeForEvent(WorldEventType type, ServerLevel level) {
        if (type == null || type == WorldEventType.NONE)
            return false;
        long timeOfDay = level.getDayTime() % 24000L;

        switch (type) {
            case BLOOD_MOON:
                // Nachtfenster: ~13000 - 23000
                return timeOfDay >= 13000L && timeOfDay < 23000L;
            case ECLIPSE:
                // Tagfenster: ~0 - 12000
                return timeOfDay >= 0L && timeOfDay < 12000L;
            case GREEN_MOON:
                return timeOfDay >= 13000L && timeOfDay < 23000L;
            default:
                return true;
        }
    }

    /**
     * Prüft, ob das angegebene Level die Overworld ist.
     */
    private static boolean isOverworld(ServerLevel level) {
        // level.dimension() gibt den ResourceKey<Level> zurück
        return level != null && level.dimension() == Level.OVERWORLD;
    }
}