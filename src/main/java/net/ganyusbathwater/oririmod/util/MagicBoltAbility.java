package net.ganyusbathwater.oririmod.util;

public enum MagicBoltAbility {
    BLAZE, // blaze fireball
    EXPLOSIVE,
    NORMAL, // normal projektil
    ENDER, // Enderpearl
    METEOR,
    SONIC, // Warden Sonic Boom
    AMATEUR_FIREBALL,
    APPRENTICE_FIREBALL,
    JOURNEYMAN_FIREBALL,
    WISE_FIREBALL,
    ETERNAL_ICE;

    public static MagicBoltAbility fromId(int id) {
        MagicBoltAbility[] vals = values();
        return id >= 0 && id < vals.length ? vals[id] : NORMAL;
    }
}
