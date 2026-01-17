package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class CustomSwordItem extends SwordItem implements ModRarityCarrier {
    private final ModRarity rarity;

    public CustomSwordItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties);
        this.rarity = rarity;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.is(ModItems.STELLA_PERDITOR.get())) {
            Component baseName = super.getName(stack);

            // Zeitbasierte Phase 0..1 (Geschwindigkeit über Divisor steuern)
            long now = System.currentTimeMillis();
            float phase = (now % 4000L) / 4000.0f; // 4 Sekunden für eine komplette Runde

            // HSV (Hue 0..1, Saturation 1, Value 1) -> RGB
            int rgb = hsvToRgb(phase, 1.0f, 1.0f);

            return baseName.copy().withStyle(style -> style
                    .withColor(rgb)
                    .withBold(true)
            );
        }

        return super.getName(stack);
    }

    /**
     * Einfache HSV->RGB Konvertierung (h,s,v in [0,1]).
     * Gibt eine 0xRRGGBB-Farbe zurück.
     */
    private static int hsvToRgb(float h, float s, float v) {
        float r, g, b;

        int i = (int) (h * 6.0f);
        float f = (h * 6.0f) - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);

        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            case 5 -> { r = v; g = p; b = q; }
            default -> { r = v; g = t; b = p; }
        }

        int ri = (int)(r * 255.0f) & 0xFF;
        int gi = (int)(g * 255.0f) & 0xFF;
        int bi = (int)(b * 255.0f) & 0xFF;

        return (ri << 16) | (gi << 8) | bi;
    }


    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
