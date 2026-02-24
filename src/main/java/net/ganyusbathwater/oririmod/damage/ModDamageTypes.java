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

    public static DamageSource getTrueDamage(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TRUE_DAMAGE), attacker);
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(TRUE_DAMAGE, new DamageType("true_damage", 0.1F));
    }
}
