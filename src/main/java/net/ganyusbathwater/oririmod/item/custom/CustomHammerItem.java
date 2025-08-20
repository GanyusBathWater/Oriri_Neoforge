package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CustomHammerItem extends MaceItem implements ModRarityCarrier {
    private final ModRarity rarity;

    public CustomHammerItem(Properties properties, ModRarity rarity) {
        super(properties);
        this.rarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (attacker instanceof Player player) {
            double fallSpeed = player.getDeltaMovement().y;

                // threshold: faster than -0.6 blocks/tick down
                if (fallSpeed < -0.6D) {
                    // Add stunned effect for 5 seconds (100 ticks)
                    target.addEffect(new MobEffectInstance(ModEffects.STUNNED_EFFECT, 100, 0));
                }

        }
        return super.hurtEnemy(stack, target, attacker);
    }
}