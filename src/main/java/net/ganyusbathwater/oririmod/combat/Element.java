package net.ganyusbathwater.oririmod.combat;

public enum Element {
    PHYSICAL(""),
    FIRE("\uE003"),
    NATURE("\uE005"),
    EARTH("\uE002"),
    WATER("\uE006"),
    LIGHT("\uE004"),
    DARKNESS("\uE001"),
    TRUE_DAMAGE("");

    private final String icon;

    Element(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }
}
