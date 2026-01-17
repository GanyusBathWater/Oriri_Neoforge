package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.world.entity.player.Player;

public class CandyBagEffect implements VestigeEffect {

    private final int intervalTicks;

    public CandyBagEffect(int intervalTicks) {
        this.intervalTicks = Math.max(1, intervalTicks);
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        if (player.tickCount % intervalTicks != 0) return;

        // 1 "Saturation point" \=\= +1 FoodLevel (Hungerleiste)
        // (Sättigungs\-Float bleibt unverändert; falls gewünscht, kann zusätzlich addSaturation genutzt werden.)
        var food = player.getFoodData();
        int before = food.getFoodLevel();
        if (before >= 20) return;

        food.setFoodLevel(Math.min(20, before + 1));
    }
}