package net.ganyusbathwater.oririmod.item.custom.cosmetic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class LamiaScaleItem extends Item implements ICurioItem {

    // Default nature emerald green for un-dyed scales
    public static final int DEFAULT_COLOR = 0x2D7A47;

    public LamiaScaleItem(Properties properties) {
        super(properties);
    }

    public static int getColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return DEFAULT_COLOR;
        DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor != null) {
            return dyedColor.rgb();
        }
        return DEFAULT_COLOR;
    }
}
