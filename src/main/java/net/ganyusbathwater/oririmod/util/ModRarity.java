package net.ganyusbathwater.oririmod.util;


import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public enum ModRarity {

    //Neue Raritäten
    COMMON("Common", "#AAAAAA"),
    UNCOMMON("Uncommon", "#55FF55"),
    RARE("Rare", "#5555FF"),
    MYTHIC("Mythic", "#AA00FF"),
    LEGENDARY("Legendary", "#FFD700"),
    GODLY("Godly", "#FF0000"),
    UNIQUE("Unique", "#40E0D0"),
    MAGICAL("Magical", "#8900DE");

    private final String displayName;
    private final TextColor color;

    ModRarity(String displayName, String hexColor) {
        this.displayName = displayName;
        this.color = TextColor.fromRgb(0xFFFFFF);
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextColor getColor() {
        return color;
    }

    public Component getTooltipText() {
        return Component.literal(displayName)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getValue())));
    }
}