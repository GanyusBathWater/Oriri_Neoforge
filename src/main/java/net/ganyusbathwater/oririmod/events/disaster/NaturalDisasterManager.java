package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent state of the currently running natural disaster (stored in the Overworld).
 * Natural disasters use their own slot and do not block / get blocked by the celestial
 * {@link net.ganyusbathwater.oririmod.events.world.WorldEventManager} events.
 */
public class NaturalDisasterManager extends SavedData {
    private static final String DATA_NAME = "oririmod_natural_disasters";

    private NaturalDisasterType activeType = NaturalDisasterType.NONE;
    private int ticksRemaining = 0;
    private int duration = 0;
    /** Started by the debug tool with "ignore safety check" enabled. */
    private boolean forced = false;
    /** In-game day on which the natural roll last happened (prevents re-rolling every tick). */
    private long lastRolledDay = -1L;

    public static NaturalDisasterManager get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(NaturalDisasterManager::new, NaturalDisasterManager::load, null),
                DATA_NAME);
    }

    public NaturalDisasterType getActiveType() {
        return activeType;
    }

    public boolean isActive() {
        return activeType != NaturalDisasterType.NONE && ticksRemaining > 0;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isForced() {
        return forced;
    }

    long getLastRolledDay() {
        return lastRolledDay;
    }

    void setLastRolledDay(long day) {
        this.lastRolledDay = day;
        setDirty();
    }

    /**
     * Starts a disaster. Fails if another disaster is running or the type is not implemented.
     */
    public boolean start(ServerLevel level, NaturalDisasterType type, int durationTicks, boolean forced) {
        if (isActive() || type == NaturalDisasterType.NONE || !type.isImplemented()) {
            return false;
        }
        this.activeType = type;
        this.ticksRemaining = durationTicks;
        this.duration = durationTicks;
        this.forced = forced;
        setDirty();

        switch (type) {
            case METEOR_SHOWER -> MeteorShowerDisaster.onStart(level, this);
            case EARTHQUAKE -> EarthquakeDisaster.onStart(level, this);
            case LIGHTNING_STORM -> LightningStormDisaster.onStart(level, this);
            case TORNADO -> TornadoDisaster.onStart(level, this);
            case SNOWSTORM -> SnowstormDisaster.onStart(level, durationTicks);
            default -> {
            }
        }
        net.ganyusbathwater.oririmod.network.NetworkHandler.sendNaturalDisasterToAll(this.activeType, this.ticksRemaining, this.duration);
        return true;
    }

    public void stop(ServerLevel level) {
        NaturalDisasterType ended = this.activeType;
        this.activeType = NaturalDisasterType.NONE;
        this.ticksRemaining = 0;
        this.duration = 0;
        this.forced = false;
        setDirty();
        net.ganyusbathwater.oririmod.network.NetworkHandler.sendNaturalDisasterToAll(NaturalDisasterType.NONE, 0, 0);

        if (ended != NaturalDisasterType.NONE) {
            switch (ended) {
                case METEOR_SHOWER -> MeteorShowerDisaster.onStop(level);
                case EARTHQUAKE -> EarthquakeDisaster.onStop(level);
                case LIGHTNING_STORM -> LightningStormDisaster.onStop(level);
                case TORNADO -> TornadoDisaster.onStop(level);
                case SNOWSTORM -> SnowstormDisaster.onStop(level);
                default -> {
                }
            }
            if (OririConfig.COMMON.naturalDisasters.disasterAnnouncements.get()) {
                Component msg = Component.translatable("message.oririmod.disaster.end", ended.displayName())
                        .withStyle(ChatFormatting.GRAY);
                for (ServerPlayer player : level.players()) {
                    player.sendSystemMessage(msg);
                }
            }
        }
    }

    void tick(ServerLevel level) {
        if (activeType == NaturalDisasterType.NONE) {
            return;
        }
        if (ticksRemaining <= 0) {
            stop(level);
            return;
        }
        ticksRemaining--;
        if (ticksRemaining % 100 == 0) {
            setDirty();
            net.ganyusbathwater.oririmod.network.NetworkHandler.sendNaturalDisasterToAll(this.activeType, this.ticksRemaining, this.duration);
        }

        switch (activeType) {
            case METEOR_SHOWER -> MeteorShowerDisaster.tick(level, this);
            case EARTHQUAKE -> EarthquakeDisaster.tick(level, this);
            case LIGHTNING_STORM -> LightningStormDisaster.tick(level, this);
            case TORNADO -> TornadoDisaster.tick(level, this);
            case SNOWSTORM -> SnowstormDisaster.tick(level, ticksRemaining, duration);
            default -> {
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("ActiveType", activeType.name());
        tag.putInt("TicksRemaining", ticksRemaining);
        tag.putInt("Duration", duration);
        tag.putBoolean("Forced", forced);
        tag.putLong("LastRolledDay", lastRolledDay);
        return tag;
    }

    public static NaturalDisasterManager load(CompoundTag tag, HolderLookup.Provider provider) {
        NaturalDisasterManager m = new NaturalDisasterManager();
        m.activeType = NaturalDisasterType.byName(tag.getString("ActiveType"));
        m.ticksRemaining = tag.getInt("TicksRemaining");
        m.duration = tag.getInt("Duration");
        m.forced = tag.getBoolean("Forced");
        m.lastRolledDay = tag.contains("LastRolledDay") ? tag.getLong("LastRolledDay") : -1L;
        if (!m.activeType.isImplemented()) {
            m.activeType = NaturalDisasterType.NONE;
            m.ticksRemaining = 0;
        }
        return m;
    }
}

