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
        if (!(event.getEntity() instanceof Mob mob))
            return;
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(SUMMONED_TAG))
            return;

        Entity attacker = event.getSource().getEntity();
        if (attacker != null) {
            String ownerUUID = data.getString(OWNER_TAG);

            // If the attacker is the owner -> cancel the damage to prevent friendly fire
            // and aggro
            if (!ownerUUID.isEmpty() && attacker.getStringUUID().equals(ownerUUID)) {
                event.setCanceled(true);
            }

            // If the attacker is another summoned mob from the exact same owner -> cancel
            // damage
            if (attacker instanceof Mob attackingMob && attackingMob.getPersistentData().getBoolean(SUMMONED_TAG)) {
                String attackerOwnerUUID = attackingMob.getPersistentData().getString(OWNER_TAG);
                if (!ownerUUID.isEmpty() && !attackerOwnerUUID.isEmpty() && ownerUUID.equals(attackerOwnerUUID)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) {
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
