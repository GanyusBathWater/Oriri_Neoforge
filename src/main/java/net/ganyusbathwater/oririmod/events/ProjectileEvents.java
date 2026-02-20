package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.enchantment.ModEnchantments;
import net.ganyusbathwater.oririmod.util.ISniperArrow;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Optional;

@EventBusSubscriber(modid = "oririmod")
public class ProjectileEvents {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();

        if (!(e instanceof AbstractArrow arrow))
            return;

        Entity ownerEntity = arrow.getOwner();
        if (!(ownerEntity instanceof LivingEntity shooter))
            return;

        boolean hasSniper = shooterHasSniperEnchantment(shooter);
        if (!hasSniper)
            return;

        if (arrow instanceof ISniperArrow sniperArrow) {
            // Set speed using the mixin interface, which handles sync and physics
            sniperArrow.oririmod$setSniperSpeed((float) arrow.getDeltaMovement().length());
        }
    }

    private static boolean shooterHasSniperEnchantment(LivingEntity shooter) {
        RegistryAccess access = shooter.level().registryAccess();
        Registry<Enchantment> enchReg = access.registryOrThrow(Registries.ENCHANTMENT);

        Optional<Holder.Reference<Enchantment>> sniperRef = enchReg.getHolder(ModEnchantments.SNIPER);
        if (sniperRef.isEmpty())
            return false;
        Holder<Enchantment> SNIPER_HOLDER = sniperRef.get();

        ItemStack main = shooter.getMainHandItem();
        if (EnchantmentHelper.getTagEnchantmentLevel(SNIPER_HOLDER, main) > 0)
            return true;

        ItemStack off = shooter.getOffhandItem();
        return EnchantmentHelper.getTagEnchantmentLevel(SNIPER_HOLDER, off) > 0;
    }
}