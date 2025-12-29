package net.ganyusbathwater.oririmod.events.vestiges;

import net.minecraft.world.level.Level;

public final class VestigeDayNightEvents {

    private VestigeDayNightEvents() {}

    public enum DayNightStatus {
        DAY,
        NIGHT,
        UNKNOWN
    }

    /**
     * Minecraft-Tagzeit: 0..23999 (\`dayTime % 24000\`)
     * Tag ist grob 0..11999, Nacht 12000..23999.
     */
    public static DayNightStatus getStatus(Level level) {
        if (level == null) return DayNightStatus.UNKNOWN;

        long time = level.getDayTime() % 24000L;

        // 0..11999 = Tag, 12000..23999 = Nacht
        return time < 12000L ? DayNightStatus.DAY : DayNightStatus.NIGHT;
    }

    public static boolean isDay(Level level) {
        return getStatus(level) == DayNightStatus.DAY;
    }

    public static boolean isNight(Level level) {
        return getStatus(level) == DayNightStatus.NIGHT;
    }
}