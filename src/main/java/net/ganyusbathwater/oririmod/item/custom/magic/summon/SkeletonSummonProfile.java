package net.ganyusbathwater.oririmod.item.custom.magic.summon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;

public class SkeletonSummonProfile implements ISummonProfile {
    
    @Override
    public EntityType<? extends Mob> getSummonType(int level) {
        if (level >= 3) {
            return EntityType.STRAY;
        } else if (level == 2) {
            return EntityType.BOGGED;
        }
        return EntityType.SKELETON;
    }

    @Override
    public void applyUpgrades(Mob mob, int level, Player player) {
        if (level >= 2) {
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(player.level().registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.POWER), 2);
            bow.enchant(player.level().registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.PUNCH), 1);
            mob.setItemSlot(EquipmentSlot.MAINHAND, bow);
        }
        if (level >= 3) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
    }

    @Override
    public String getDamageTooltip(int level) {
        return level >= 2 ? "5.0" : "2.0";
    }
}
