package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;

public final class StriderScaleEffect implements VestigeEffect {

    private static final ResourceLocation ARMOR_MOD_ID = ResourceLocation.fromNamespaceAndPath(
            OririMod.MOD_ID,
            "vestige_strider_scale_armor"
    );

    private static final ResourceLocation ATTACK_DAMAGE_MOD_ID = ResourceLocation.fromNamespaceAndPath(
            OririMod.MOD_ID,
            "vestige_strider_scale_attack_damage"
    );

    private final int effectLevel;

    public StriderScaleEffect(int effectLevel) {
        this.effectLevel = effectLevel;
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        int unlocked = Math.min(ctx.levelUnlocked(), 3);
        if (unlocked <= 0) return;

        // nur höchste Instanz schreibt
        if (this.effectLevel != unlocked) return;

        if (unlocked >= 1) {
            applyArmorInNether(player);
        }
        if (unlocked >= 2) {
            applyAttackDamageInNether(player);
        }
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        tick(ctx);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.player() == null) return;
        removeArmorModifier(ctx.player());
        removeAttackDamageModifier(ctx.player());
    }

    private static void applyArmorInNether(Player player) {
        if (!isInNether(player)) {
            removeArmorModifier(player);
            return;
        }

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) return;

        armor.removeModifier(ARMOR_MOD_ID);
        armor.addTransientModifier(new AttributeModifier(ARMOR_MOD_ID, 5.0d, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void applyAttackDamageInNether(Player player) {
        if (!isInNether(player)) {
            removeAttackDamageModifier(player);
            return;
        }

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;

        attackDamage.removeModifier(ATTACK_DAMAGE_MOD_ID);
        attackDamage.addTransientModifier(new AttributeModifier(
                ATTACK_DAMAGE_MOD_ID,
                2.0d,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static boolean isInNether(Player player) {
        Level level = player.level();
        return level != null
                && level.dimensionTypeRegistration() != null
                && level.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER);
    }

    private static void removeArmorModifier(Player player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        armor.removeModifier(ARMOR_MOD_ID);
    }

    private static void removeAttackDamageModifier(Player player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;
        attackDamage.removeModifier(ATTACK_DAMAGE_MOD_ID);
    }
}