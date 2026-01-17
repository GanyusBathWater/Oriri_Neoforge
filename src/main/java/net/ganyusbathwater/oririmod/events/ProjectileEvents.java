package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = "oririmod")
public class ProjectileEvents {

    private static final String NBT_SNIPER_NO_GRAV = "OririMod:SniperNoGrav";

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();

        if (!(e instanceof AbstractArrow arrow)) return;

        Entity ownerEntity = arrow.getOwner();
        if (!(ownerEntity instanceof LivingEntity shooter)) return;

        boolean hasSniper = shooterHasSniperEnchantment(shooter);
        if (!hasSniper) return;

        arrow.setNoGravity(true);

        CompoundTag tag = arrow.getPersistentData();
        tag.putBoolean("sniper_nodrag", true);
        Vec3 v = arrow.getDeltaMovement();
        tag.putDouble("sniper_vx", v.x);
        tag.putDouble("sniper_vy", v.y);
        tag.putDouble("sniper_vz", v.z);

        arrow.getPersistentData().putBoolean(NBT_SNIPER_NO_GRAV, true);
    }

    private static boolean shooterHasSniperEnchantment(LivingEntity shooter) {
        RegistryAccess access = shooter.level().registryAccess();
        Registry<Enchantment> enchReg = access.registryOrThrow(Registries.ENCHANTMENT);

        Optional<Holder.Reference<Enchantment>> sniperRef = enchReg.getHolder(ModEnchantments.SNIPER);
        if (sniperRef.isEmpty()) return false;
        Holder<Enchantment> SNIPER_HOLDER = sniperRef.get();

        ItemStack main = shooter.getMainHandItem();
        if (EnchantmentHelper.getTagEnchantmentLevel(SNIPER_HOLDER, main) > 0) return true;

        ItemStack off = shooter.getOffhandItem();
        return EnchantmentHelper.getTagEnchantmentLevel(SNIPER_HOLDER, off) > 0;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level.isClientSide()) return;

        for (Player player : level.players()) {
            AABB area = player.getBoundingBox().inflate(64);
            List<AbstractArrow> arrows = level.getEntitiesOfClass(
                    AbstractArrow.class,
                    area,
                    a -> a.getPersistentData().getBoolean("sniper_nodrag")
            );


            if (arrows.isEmpty()) return;

            for (AbstractArrow arrow : arrows) {
                CompoundTag tag = arrow.getPersistentData();

                if (arrow.onGround()) continue;

                double vx = tag.contains("sniper_vx") ? tag.getDouble("sniper_vx") : arrow.getDeltaMovement().x;
                double vy = tag.contains("sniper_vy") ? tag.getDouble("sniper_vy") : arrow.getDeltaMovement().y;
                double vz = tag.contains("sniper_vz") ? tag.getDouble("sniper_vz") : arrow.getDeltaMovement().z;

                arrow.setDeltaMovement(new Vec3(vx, vy, vz));
                arrow.setNoGravity(true);
            }
        }
    }
}