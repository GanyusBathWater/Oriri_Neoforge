package net.ganyusbathwater.oririmod.events;


import net.ganyusbathwater.oririmod.enchantment.ModEnchantments;
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

// Passe package / ModID an
@EventBusSubscriber(modid = "oririmod")
public class ProjectileEvents {

    // optional: Name des NBT-Keys, um das Verhalten persistent zu markieren
    private static final String NBT_SNIPER_NO_GRAV = "OririMod:SniperNoGrav";

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();

        // interessiert uns nur bei Pfeilen (AbstractArrow deckt ArrowEntity, SpectralArrow, TippedArrow)
        if (!(e instanceof AbstractArrow arrow)) return;

        // Owner des Pfeils (kann null sein)
        Entity ownerEntity = arrow.getOwner();
        if (!(ownerEntity instanceof LivingEntity shooter)) return;

        // Auf beiden Seiten prüfen (Client + Server) — setNoGravity ist synchronisiert vom Server, aber
        // wir setzen hier trotzdem auf beiden Seiten, um visuelle Desyncs zu vermeiden.
        // Prüfen, ob Shooter ein Item mit Sniper hat:
        boolean hasSniper = shooterHasSniperEnchantment(shooter);
        if (!hasSniper) return;

        // Pfeil keine Gravitation geben
        arrow.setNoGravity(true);

        // optional: Markieren, damit du es später z.B. im Tick/Impact wiederfinden oder zurücksetzen kannst
        arrow.getPersistentData().putBoolean(NBT_SNIPER_NO_GRAV, true);

    }

    private static boolean shooterHasSniperEnchantment(LivingEntity shooter) {
        // 1) Holder<Enchantment> aus deinem ResourceKey<Enchantment> holen
        RegistryAccess access = shooter.level().registryAccess();
        Registry<Enchantment> enchReg = access.registryOrThrow(Registries.ENCHANTMENT);

        Optional<Holder.Reference<Enchantment>> sniperRef = enchReg.getHolder(ModEnchantments.SNIPER);
        if (sniperRef.isEmpty()) return false;
        Holder<Enchantment> SNIPER_HOLDER = sniperRef.get();

        // 2) Auf beiden Händen prüfen (Bogen/Armbrust kann in Main- oder Offhand sein)
        ItemStack main = shooter.getMainHandItem();
        if (EnchantmentHelper.getTagEnchantmentLevel(SNIPER_HOLDER, main) > 0) return true;

        ItemStack off = shooter.getOffhandItem();
        return EnchantmentHelper.getTagEnchantmentLevel(SNIPER_HOLDER, off) > 0;
    }
}