package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Handles the lifespan countdown for summoned mobs.
 * Every server tick, decrements the remaining ticks on mobs tagged
 * "OririSummoned"
 * and discards them when they expire. Also prevents friendly fire from the
 * owner.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public class SummonerEvents {

    private static final String SUMMONED_TAG = "OririSummoned";
    private static final String TICKS_TAG = "OririSummonTicks";
    private static final String OWNER_TAG = "OririSummonerUUID";

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        
        String targetOwnerStr = "";
        if (target instanceof net.minecraft.world.entity.player.Player tp) targetOwnerStr = tp.getStringUUID();
        else if (target instanceof Mob tm && tm.getPersistentData().getBoolean(SUMMONED_TAG)) targetOwnerStr = tm.getPersistentData().getString(OWNER_TAG);
        
        String attackerOwnerStr = "";
        if (attacker instanceof net.minecraft.world.entity.player.Player ap) attackerOwnerStr = ap.getStringUUID();
        else if (attacker instanceof Mob am && am.getPersistentData().getBoolean(SUMMONED_TAG)) attackerOwnerStr = am.getPersistentData().getString(OWNER_TAG);
        
        if (!targetOwnerStr.isEmpty() && !attackerOwnerStr.isEmpty() && areAllies(targetOwnerStr, attackerOwnerStr, target.level())) {
            event.setCanceled(true);
        }
    }
    
    private static boolean areAllies(String uuid1, String uuid2, net.minecraft.world.level.Level level) {
        if (uuid1.equals(uuid2)) return true;
        if (level instanceof ServerLevel serverLevel) {
            try {
                java.util.UUID u1 = java.util.UUID.fromString(uuid1);
                java.util.UUID u2 = java.util.UUID.fromString(uuid2);
                net.ganyusbathwater.oririmod.dungeon.DungeonInstance inst = net.ganyusbathwater.oririmod.dungeon.DungeonManager.get(serverLevel).getInstanceForPlayer(u1);
                return inst != null && inst.getPlayers().contains(u2);
            } catch (Exception ignored) {}
        }
        return false;
    }

    @SubscribeEvent
    public static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) {
            if (mob instanceof net.ganyusbathwater.oririmod.entity.custom.VenomousPlantEntity) {
                return; // Stationary plants manage their own AI goals when summoned
            }
            CompoundTag data = mob.getPersistentData();
            if (data.getBoolean(SUMMONED_TAG)) {
                net.ganyusbathwater.oririmod.item.custom.magic.SummonerWeaponItem.rebuildAI(mob);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            // Iterate over all entities in loaded chunks
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob))
                    continue;
                CompoundTag data = mob.getPersistentData();
                if (!data.getBoolean(SUMMONED_TAG))
                    continue;

                int remaining = data.getInt(TICKS_TAG);
                remaining--;

                if (remaining <= 0) {
                    mob.discard();
                } else {
                    data.putInt(TICKS_TAG, remaining);
                }
            }
        }
    }
}
