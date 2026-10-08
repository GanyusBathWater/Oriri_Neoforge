package net.ganyusbathwater.oririmod.item.custom.magic.summon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;

public class SlimeSummonProfile implements ISummonProfile {
    
    public static final double LEVEL_3_HEALTH_BONUS = 40.0;
    
    private final EntityType<? extends Slime> slimeType;

    public SlimeSummonProfile(EntityType<? extends Slime> slimeType) {
        this.slimeType = slimeType;
    }

    @Override
    public EntityType<? extends Mob> getSummonType(int level) {
        return slimeType;
    }

    @Override
    public void applyUpgrades(Mob mob, int level, Player player) {
        if (mob instanceof Slime slime) {
            if (level <= 1) {
                slime.setSize(2, true);
            } else {
                slime.setSize(4, true);
            }
            
            if (level >= 3) {
                AttributeInstance maxHealth = slime.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.addPermanentModifier(
                            new AttributeModifier(ResourceLocation.withDefaultNamespace(slimeType == EntityType.MAGMA_CUBE ? "magmacube_level3_health" : "slime_level3_health"), LEVEL_3_HEALTH_BONUS,
                                    AttributeModifier.Operation.ADD_VALUE));
                    slime.setHealth(slime.getMaxHealth());
                }
            }
        }
    }

    @Override
    public String getDamageTooltip(int level) {
        return level >= 2 ? "4.0" : "2.0";
    }
}
