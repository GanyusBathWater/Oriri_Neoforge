package net.ganyusbathwater.oririmod.item.custom.magic.summon;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

public class EyeOfDesolationSummonProfile implements ISummonProfile {
    
    @Override
    public EntityType<? extends Mob> getSummonType(int level) {
        return ModEntities.EYE_OF_DESOLATION.get();
    }

    @Override
    public void applyUpgrades(Mob mob, int level, Player player) {
        if (level >= 2) {
            AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.addPermanentModifier(
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("eod_level2_armor"), 0.5D,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        if (level >= 3) {
            AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.addPermanentModifier(
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("eod_level3_damage"), 1.0D,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    @Override
    public String getDamageTooltip(int level) {
        return level >= 3 ? "8.0" : "4.0"; // Base damage is 4.0, doubles at level 3
    }
}
