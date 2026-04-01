package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class CustomScytheItem extends SwordItem implements net.ganyusbathwater.oririmod.util.ModRarityCarrier {
    private final net.ganyusbathwater.oririmod.util.ModRarity rarity;

    public CustomScytheItem(Tier tier, Properties properties, net.ganyusbathwater.oririmod.util.ModRarity rarity) {
        super(tier, properties);
        this.rarity = rarity;
    }

    @Override
    public net.ganyusbathwater.oririmod.util.ModRarity getModRarity() {
        return rarity;
    }

    public static ItemAttributeModifiers createScytheAttributes(Tier tier, int attackDamage, float attackSpeed, float sweepingRatio) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, (double) ((float) attackDamage + tier.getAttackDamageBonus()), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, (double) attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.SWEEPING_DAMAGE_RATIO, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "scythe_sweep"), (double) sweepingRatio, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return super.hurtEnemy(stack, target, attacker);
    }

    public void applyScytheEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Can be overridden by specific scythes to apply mob effects
    }

    @Override
    public @NotNull AABB getSweepHitBox(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target) {
        // Vanilla sweep hitbox is target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D)
        // User requested approx 2 blocks extension (1.0 + 2.0 = 3.0)
        return target.getBoundingBox().inflate(3.0D, 0.25D, 3.0D);
    }
}

