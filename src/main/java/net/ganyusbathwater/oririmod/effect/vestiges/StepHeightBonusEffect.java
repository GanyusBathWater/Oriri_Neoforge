package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class StepHeightBonusEffect implements VestigeEffect {
    private final float bonusPerLevel;

    public StepHeightBonusEffect(float bonusPerLevel) {
        this.bonusPerLevel = bonusPerLevel;
    }

    @Override
    public float stepHeightBonus(ServerPlayer player, ItemStack stack, int level) {
        return bonusPerLevel; // pro aktiver Stufe
    }
}
