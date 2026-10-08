package net.ganyusbathwater.oririmod.item.custom.magic.summon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class IronGolemSummonProfile implements ISummonProfile {
    
    public static final double LEVEL_3_HEALTH_BONUS = 50.0;

    @Override
    public EntityType<? extends Mob> getSummonType(int level) {
        return EntityType.IRON_GOLEM;
    }

    @Override
    public void applyUpgrades(Mob mob, int level, Player player) {
        if (level >= 2) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 0, false, false));
        }
        if (level >= 3) {
            AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.addPermanentModifier(
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("golem_level3_health"), LEVEL_3_HEALTH_BONUS,
                                AttributeModifier.Operation.ADD_VALUE));
                mob.setHealth(mob.getMaxHealth());
            }
        }
    }

    @Override
    public String getDamageTooltip(int level) {
        return level >= 2 ? "18.0" : "15.0";
    }
}
