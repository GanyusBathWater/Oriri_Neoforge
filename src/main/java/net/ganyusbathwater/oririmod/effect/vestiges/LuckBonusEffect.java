package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class LuckBonusEffect implements VestigeEffect{
    private final float bonusPerLevel;

    public LuckBonusEffect(float bonusPerLevel) {
        this.bonusPerLevel = bonusPerLevel;
    }

    @Override
    public float LuckBonus(ServerPlayer player, ItemStack stack, int level) {
        return bonusPerLevel;
    }
}