package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.MagicBoltAbility;
import net.ganyusbathwater.oririmod.util.MagicIndicatorClientState;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class MagicBoltItem extends Item implements ModRarityCarrier {
    private final MagicBoltAbility ability;
    private final int cooldown;
    private final ModRarity rarity;

    public MagicBoltItem(Properties props, MagicBoltAbility ability, int cooldown, ModRarity rarity) {
        super(props);
        this.ability = ability;
        this.cooldown = cooldown;
        this.rarity = rarity;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // langes Aufladen (wie Bogen)
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // optionale Lade-Animation
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // NICHT schießen – nur Nutzung starten (damit Aufladen + onUseTick laufen)
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide) return;

        // Nur einmal beim Start der Nutzung aktivieren
        if (remainingUseDuration == getUseDuration(stack, living) - 1) {
            MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                    .duration(0)      // 0 => solange genutzt
                    .distance(1.6f);  // Basis-Distanz vor den Augen

            MagicIndicatorClientState.Indicator.Layer outer = new MagicIndicatorClientState.Indicator.Layer(
                    ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_outer.png"),
                    1.40f,  6f, 0xCCFFFFFF, 0f
            );
            MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                    ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_mid.png"),
                    1.00f, -8f, 0xAA66CCFF, 0f
            );
            MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                    ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_inner.png"),
                    0.25f, 12f, 0xFFFFFFFF, 0.0f
            );

            var indicator = b.addLayer(outer).addLayer(mid).addLayer(inner).build();
            MagicIndicatorClientState.startFor(living, indicator);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide) {
            net.ganyusbathwater.oririmod.util.MagicIndicatorClientState.stopFor(living);
            return;
        }
        int usedTicks = getUseDuration(stack, living) - timeLeft;
        int minCharge = 10;
        if (usedTicks < minCharge) return;

        net.ganyusbathwater.oririmod.entity.MagicBoltEntity bolt =
                new net.ganyusbathwater.oririmod.entity.MagicBoltEntity(level, living);
        bolt.setAbility(this.ability);

        // Hier pro Ability schneller/langsamer machen
        float speed = switch (this.ability) {
            case SONIC -> 5.0F;
            case BLAZE -> 2.2F;
            case EXPLOSIVE -> 2.0F; // wird in der Entity noch *0.85f reduziert
            case ENDER -> 1.3F;
            case NORMAL -> 1.6F;
        };

        bolt.launchStraight(living, speed);
        level.addFreshEntity(bolt);

        if (living instanceof net.minecraft.world.entity.player.Player p) {
            p.getCooldowns().addCooldown(this, cooldown);
        }
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
