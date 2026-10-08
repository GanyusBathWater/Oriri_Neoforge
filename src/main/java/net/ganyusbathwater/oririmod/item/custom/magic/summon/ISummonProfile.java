package net.ganyusbathwater.oririmod.item.custom.magic.summon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public interface ISummonProfile {
    /** Returns the base entity type to spawn at the given level. */
    EntityType<? extends Mob> getSummonType(int level);
    
    /** Applies gear, potion effects, and attributes to the spawned mob. */
    void applyUpgrades(Mob mob, int level, Player player);
    
    /** Returns the damage value for the tooltip. */
    String getDamageTooltip(int level);
}
