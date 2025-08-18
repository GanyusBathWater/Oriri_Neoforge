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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        Component displayName = stack.getHoverName()
                .copy()
                .setStyle(Style.EMPTY.withColor(rarity.getColor()));

        tooltipComponents.add(displayName);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy()
                .setStyle(Style.EMPTY.withColor(rarity.getColor()));
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        // Prüfen, ob der Angreifer ein Spieler ist
        if (attacker instanceof Player player) {
            // Fallgeschwindigkeit prüfen (negative y-Bewegung)
            double fallSpeed = player.getDeltaMovement().y;

                // Beispiel-Schwelle: schneller als -0.6 Blöcke/Tick nach unten
                if (fallSpeed < -0.6D) {
                    // Stunned Effekt für 5 Sekunden (100 Ticks) hinzufügen
                    target.addEffect(new MobEffectInstance(ModEffects.STUNNED_EFFECT, 100, 0));
                }

        }
        return super.hurtEnemy(stack, target, attacker);
    }
}