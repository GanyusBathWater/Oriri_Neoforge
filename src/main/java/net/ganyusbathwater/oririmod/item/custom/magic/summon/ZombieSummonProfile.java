package net.ganyusbathwater.oririmod.item.custom.magic.summon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ZombieSummonProfile implements ISummonProfile {
    
    @Override
    public EntityType<? extends Mob> getSummonType(int level) {
        return EntityType.ZOMBIE;
    }

    @Override
    public void applyUpgrades(Mob mob, int level, Player player) {
        if (level >= 2) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
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
        return level >= 2 ? "9.0" : "3.0";
    }
}
