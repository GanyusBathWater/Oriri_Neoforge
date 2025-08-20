package net.ganyusbathwater.oririmod.util;


import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public enum ModRarity {

    //New Raritys
    COMMON("Common", "#AAAAAA"),
    UNCOMMON("Uncommon", "#55FF55"),
    RARE("Rare", "#5555FF"),
    MYTHIC("Mythic", "#AA00FF"),
    LEGENDARY("Legendary", "#FFD700"),
    GODLY("Godly", "#FF0000"),
    UNIQUE("Unique", "#40E0D0"),
    MAGICAL("Magical", "#8900DE");

    private final String displayName;
    private final String hexColor;

    ModRarity(String displayName, String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    public String displayName() { return displayName; }
    public String hex() { return hexColor; }

    public int rgb() {
        String s = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        return (int) Long.parseLong(s, 16) & 0xFFFFFF;
    }

    public TextColor textColor() {
        return TextColor.fromRgb(rgb());
    }

    public Component coloredDisplayName() {
        return Component.literal(displayName).setStyle(Style.EMPTY.withColor(textColor()));
    }
}