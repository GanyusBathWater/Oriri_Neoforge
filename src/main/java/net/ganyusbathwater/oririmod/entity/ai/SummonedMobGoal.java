package net.ganyusbathwater.oririmod.entity.ai;

import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.util.SummonTargetMode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Targeting goal for summoned mobs.
 * Reads the target mode from config and the owner UUID from the mob's
 * persistent data.
 * Always excludes the summoner from targeting.
 */
public class SummonedMobGoal extends NearestAttackableTargetGoal<LivingEntity> {

    private static final String OWNER_TAG = "OririSummonerUUID";

    @Nullable
    private final UUID ownerUUID;

    public SummonedMobGoal(Mob mob) {
        super(mob, LivingEntity.class, 10, true, false, (target) -> {
            if (target == null)
                return false;
            String ownerStr = mob.getPersistentData().getString(OWNER_TAG);
            if (!ownerStr.isEmpty()) {
                if (target.getStringUUID().equals(ownerStr))
                    return false; // don't attack owner
                if (target.getPersistentData().getString(OWNER_TAG).equals(ownerStr))
                    return false; // don't attack allied summons
                
                // Don't attack dungeon party members or their summons
                if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    try {
                        java.util.UUID oId = java.util.UUID.fromString(ownerStr);
                        net.ganyusbathwater.oririmod.dungeon.DungeonInstance inst = net.ganyusbathwater.oririmod.dungeon.DungeonManager.get(serverLevel).getInstanceForPlayer(oId);
                        if (inst != null) {
                            if (target instanceof Player && inst.getPlayers().contains(target.getUUID())) return false;
                            String targetOwnerStr = target.getPersistentData().getString(OWNER_TAG);
                            if (!targetOwnerStr.isEmpty() && inst.getPlayers().contains(java.util.UUID.fromString(targetOwnerStr))) return false;
                        }
                    } catch (Exception ignored) {}
                }
            }

            SummonTargetMode mode = OririConfig.COMMON.summoner.targetMode.get();
            switch (mode) {
                case HOSTILE_ONLY:
                    return target instanceof net.minecraft.world.entity.monster.Enemy;
                case ALL_MOBS:
                    return !(target instanceof Player);
                case PVP:
                    if (target instanceof Player player) {
                        return !player.isCreative() && !player.isSpectator();
                    }
                    return true;
                default:
                    return false;
            }
        });

        // Read owner UUID from persistent data
        String uuidStr = mob.getPersistentData().getString(OWNER_TAG);
        this.ownerUUID = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
    }

    @Override
    protected boolean canAttack(@Nullable LivingEntity target,
            net.minecraft.world.entity.ai.targeting.TargetingConditions conditions) {
        if (target == null)
            return false;
        // Never attack the summoner
        if (ownerUUID != null && target.getUUID().equals(ownerUUID))
            return false;

        // Never attack other mobs summoned by the same owner or party members
        if (ownerUUID != null) {
            String targetOwnerStr = target.getPersistentData().getString(OWNER_TAG);
            if (!targetOwnerStr.isEmpty() && targetOwnerStr.equals(ownerUUID.toString())) {
                return false;
            }
            if (this.mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                try {
                    net.ganyusbathwater.oririmod.dungeon.DungeonInstance inst = net.ganyusbathwater.oririmod.dungeon.DungeonManager.get(serverLevel).getInstanceForPlayer(ownerUUID);
                    if (inst != null) {
                        if (target instanceof Player && inst.getPlayers().contains(target.getUUID())) return false;
                        if (!targetOwnerStr.isEmpty() && inst.getPlayers().contains(java.util.UUID.fromString(targetOwnerStr))) return false;
                    }
                } catch (Exception ignored) {}
            }
        }

        SummonTargetMode mode = OririConfig.COMMON.summoner.targetMode.get();

        switch (mode) {
            case HOSTILE_ONLY:
                return target instanceof net.minecraft.world.entity.monster.Enemy && super.canAttack(target, conditions);
            case ALL_MOBS:
                // All mobs but never players
                return !(target instanceof Player) && super.canAttack(target, conditions);
            case PVP:
                // Everything except the owner (already filtered above)
                return super.canAttack(target, conditions);
            default:
                return false;
        }
    }
}
