package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.util.StringRepresentable;

public enum ForcefieldVariant implements StringRepresentable {
    REPELLENT("repellent", 0xFF00FF00), // Green
    ATTRACTING("attracting", 0xFFFF0000), // Red
    PROTECTION("protection", 0xFF0000FF), // Blue
    MODIFIER("modifier", 0xFF800080); // Purple

    private final String name;
    private final int color;

    ForcefieldVariant(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getColor() {
        return this.color;
    }
}
