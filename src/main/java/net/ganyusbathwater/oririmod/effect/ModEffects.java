package net.ganyusbathwater.oririmod.effect;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
        public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister
                        .create(BuiltInRegistries.MOB_EFFECT, OririMod.MOD_ID);

        public static final Holder<MobEffect> STUNNED_EFFECT = MOB_EFFECTS.register("stunned",
                        () -> new StunnedEffect(MobEffectCategory.HARMFUL, 0x000000));

        public static final Holder<MobEffect> STUN_IMMUNITY_EFFECT = MOB_EFFECTS.register("stun_immunity",
                        () -> new StunImmunityEffect(MobEffectCategory.BENEFICIAL, 0xCCCCCC));

        public static final Holder<MobEffect> BROKEN_EFFECT = MOB_EFFECTS.register("broken",
                        () -> new BrokenEffect(MobEffectCategory.HARMFUL, 0x000000));

        public static final Holder<MobEffect> CHARMED_EFFECT = MOB_EFFECTS.register("charmed",
                        () -> new BrokenEffect(MobEffectCategory.HARMFUL, 0xFF69B4));

        public static final Holder<MobEffect> MOB_SENSE_EFFECT = MOB_EFFECTS.register("mob_sense",
                        () -> new MobSenseEffect(MobEffectCategory.BENEFICIAL, 0xffffff));

        public static final Holder<MobEffect> ANTI_HEAL_EFFECT = MOB_EFFECTS.register("anti_heal",
                        () -> new AntiHealEffect(MobEffectCategory.HARMFUL, 0x8B0000));

        public static final Holder<MobEffect> THREAT_DETECTION_EFFECT = MOB_EFFECTS.register("threat_detection",
                        () -> new ThreatDetectionEffect(MobEffectCategory.HARMFUL, 0xCC0000));

        public static final Holder<MobEffect> COLD_AURA_EFFECT = MOB_EFFECTS.register("cold_aura",
                        () -> new ColdAuraEffect(MobEffectCategory.HARMFUL, 0x00FFFF));

        public static final Holder<MobEffect> FORCEFIELD_PENALTY_HEALTH = MOB_EFFECTS.register("forcefield_penalty_health",
                        () -> new ForcefieldPenaltyEffect(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, "forcefield_penalty_health", -0.05, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        public static final Holder<MobEffect> FORCEFIELD_PENALTY_ARMOR = MOB_EFFECTS.register("forcefield_penalty_armor",
                        () -> new ForcefieldPenaltyEffect(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, "forcefield_penalty_armor", -0.05, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        public static final Holder<MobEffect> FORCEFIELD_PENALTY_ARMOR_TOUGHNESS = MOB_EFFECTS.register("forcefield_penalty_armor_toughness",
                        () -> new ForcefieldPenaltyEffect(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, "forcefield_penalty_armor_toughness", -0.05, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        public static final Holder<MobEffect> FORCEFIELD_PENALTY_ATTACK_DAMAGE = MOB_EFFECTS.register("forcefield_penalty_attack_damage",
                        () -> new ForcefieldPenaltyEffect(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, "forcefield_penalty_attack_damage", -0.05, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        public static final Holder<MobEffect> FORCEFIELD_PENALTY_ATTACK_SPEED = MOB_EFFECTS.register("forcefield_penalty_attack_speed",
                        () -> new ForcefieldPenaltyEffect(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, "forcefield_penalty_attack_speed", -0.05, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        public static final Holder<MobEffect> FORCEFIELD_PENALTY_BURNING_TIME = MOB_EFFECTS.register("forcefield_penalty_burning_time",
                        () -> new ForcefieldPenaltyEffect(net.minecraft.world.entity.ai.attributes.Attributes.BURNING_TIME, "forcefield_penalty_burning_time", 0.05, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        public static final Holder<MobEffect> FORCEFIELD_PENALTY_FALL_DAMAGE = MOB_EFFECTS.register("forcefield_penalty_fall_damage",
                        () -> new ForcefieldPenaltyEffect(net.minecraft.world.entity.ai.attributes.Attributes.FALL_DAMAGE_MULTIPLIER, "forcefield_penalty_fall_damage", 0.05, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        public static void registerEffects(IEventBus eventBus) {
                MOB_EFFECTS.register(eventBus);
                OririMod.LOGGER.info("Registering Mod Effects for " + OririMod.MOD_ID);
        }
}