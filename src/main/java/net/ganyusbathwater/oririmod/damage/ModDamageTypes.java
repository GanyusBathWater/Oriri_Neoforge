package net.ganyusbathwater.oririmod.damage;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ModDamageTypes {

    public static final ResourceKey<DamageType> TRUE_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "true_damage"));

    public static final ResourceKey<DamageType> ELEMENT_FIRE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_fire"));
    public static final ResourceKey<DamageType> ELEMENT_WATER = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_water"));
    public static final ResourceKey<DamageType> ELEMENT_NATURE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_nature"));
    public static final ResourceKey<DamageType> ELEMENT_EARTH = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_earth"));
    public static final ResourceKey<DamageType> ELEMENT_LIGHT = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_light"));
    public static final ResourceKey<DamageType> ELEMENT_DARKNESS = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_darkness"));

    public static DamageSource getTrueDamage(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TRUE_DAMAGE), attacker);
    }

    public static DamageSource getElementalDamage(Level level, Entity attacker, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),
                attacker);
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(TRUE_DAMAGE, new DamageType("true_damage", 0.1F));
        context.register(ELEMENT_FIRE, new DamageType("element_fire", 0.1F));
        context.register(ELEMENT_WATER, new DamageType("element_water", 0.1F));
        context.register(ELEMENT_NATURE, new DamageType("element_nature", 0.1F));
        context.register(ELEMENT_EARTH, new DamageType("element_earth", 0.1F));
        context.register(ELEMENT_LIGHT, new DamageType("element_light", 0.1F));
        context.register(ELEMENT_DARKNESS, new DamageType("element_darkness", 0.1F));
    }
}
