package net.ganyusbathwater.oririmod.item.custom.magic.summon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class BlazeSummonProfile implements ISummonProfile {
    
    public static final double LEVEL_2_HEALTH_BONUS = 20.0;
    public static final double LEVEL_3_HEALTH_BONUS = 40.0;

    @Override
    public EntityType<? extends Mob> getSummonType(int level) {
        return EntityType.BLAZE;
    }

    @Override
    public void applyUpgrades(Mob mob, int level, Player player) {
        if (level >= 2) {
            AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.addPermanentModifier(
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("blaze_level2_health"), LEVEL_2_HEALTH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE));
                mob.setHealth(mob.getMaxHealth());
            }
        }
        if (level >= 3) {
            AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.addPermanentModifier(
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("blaze_level3_health"), LEVEL_3_HEALTH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE));
                mob.setHealth(mob.getMaxHealth());
            }
        }
    }

    @Override
    public String getDamageTooltip(int level) {
        return "6.0";
    }
}
