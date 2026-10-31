package net.ganyusbathwater.oririmod.events.disaster;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * All natural disasters. Natural disasters run in their own slot (see {@link NaturalDisasterManager})
 * and can therefore coexist with the celestial world events (Blood Moon, Green Moon, Eclipse).
 */
public enum NaturalDisasterType {
    NONE(false),
    METEOR_SHOWER(true),
    EARTHQUAKE(true),
    LIGHTNING_STORM(true),
    TORNADO(true),
    SNOWSTORM(true);

    private final boolean implemented;

    NaturalDisasterType(boolean implemented) {
        this.implemented = implemented;
    }

    /** False for disasters that are planned but whose gameplay logic does not exist yet. */
    public boolean isImplemented() {
        return implemented;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("disaster.oririmod." + id());
    }

    /** Returns the config block of this disaster, or {@code null} for {@link #NONE}. */
    public OririConfig.DisasterSettings settings() {
        OririConfig.NaturalDisasters cfg = OririConfig.COMMON.naturalDisasters;
        return switch (this) {
            case METEOR_SHOWER -> cfg.meteorShower;
            case EARTHQUAKE -> cfg.earthquake;
            case LIGHTNING_STORM -> cfg.lightningStorm;
            case TORNADO -> cfg.tornado;
            case SNOWSTORM -> cfg.snowstorm;
            case NONE -> null;
        };
    }

    public static NaturalDisasterType byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return NONE;
        }
    }
}

