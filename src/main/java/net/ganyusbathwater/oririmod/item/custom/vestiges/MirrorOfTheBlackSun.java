package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.BlackMirrorEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class MirrorOfTheBlackSun extends VestigeItem {

    private static final BlackMirrorEffect BLACK_MIRROR_EFFECT = new BlackMirrorEffect();

    public MirrorOfTheBlackSun(Properties props) {
        super(props, List.of(
                // Level 1: Cheat-Death mit 1,5 Minuten CD
                List.of(BLACK_MIRROR_EFFECT),
                // Level 2: Cheat-Death mit 1 Minute CD
                List.of(BLACK_MIRROR_EFFECT),
                // Level 3: Cheat-Death mit 45 Sekunden CD
                List.of(BLACK_MIRROR_EFFECT)
        ));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        this.setUnlockedLevel(stack, this.getMaxLevel());
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide) return;
        if (this.getUnlockedLevel(stack) < this.getMaxLevel()) {
            this.setUnlockedLevel(stack, this.getMaxLevel());
        }
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.mirror_of_the_black_sun";
    }
}
