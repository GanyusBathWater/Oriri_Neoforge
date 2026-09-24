package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DungeonMarkerItem extends Item {
    public static final java.util.Map<java.util.UUID, net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload> LAST_CONFIG = new java.util.HashMap<>();

    public DungeonMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(face);

        Entity entity = ModEntities.DUNGEON_MARKER.get().create(serverLevel);
        if (entity != null) {
            entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            entity.setYRot(context.getPlayer().getYRot());
            entity.setXRot(context.getPlayer().getXRot());
            entity.setCustomName(net.minecraft.network.chat.Component.literal("§d[Dungeon Marker]"));
            entity.setCustomNameVisible(true);
            
            if (entity instanceof net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity marker) {
                net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload lastConfig = LAST_CONFIG.get(context.getPlayer().getUUID());
                if (lastConfig != null) {
                    marker.setStageId(lastConfig.stageId());
                    marker.setStageType(lastConfig.stageType());
                    marker.setRole(lastConfig.role());
                    
                    net.minecraft.nbt.CompoundTag extra = marker.getExtraData();
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_ENEMY_TYPE, lastConfig.enemyType());
                    extra.putInt(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_COUNT, lastConfig.count());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_SWITCH_ID, lastConfig.switchId());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_LOOT_TABLE, lastConfig.lootTable());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_BOSS_ID, lastConfig.bossId());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_OBJECTIVE, lastConfig.objectiveText());
                }
            }
            
            serverLevel.addFreshEntity(entity);
            if (!context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
            
            context.getPlayer().displayClientMessage(
                net.minecraft.network.chat.Component.literal("§aPlaced Dungeon Marker! §7Use /data merge entity to edit its NBT. Left-click to destroy."), 
                false
            );
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
        }

        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 spawnPos = player.position().add(0, player.getEyeHeight(), 0).add(look.scale(3));

        Entity entity = ModEntities.DUNGEON_MARKER.get().create(serverLevel);
        if (entity != null) {
            entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            entity.setYRot(player.getYRot());
            entity.setXRot(player.getXRot());
            entity.setCustomName(net.minecraft.network.chat.Component.literal("§d[Dungeon Marker]"));
            entity.setCustomNameVisible(true);
            
            if (entity instanceof net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity marker) {
                net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload lastConfig = LAST_CONFIG.get(player.getUUID());
                if (lastConfig != null) {
                    marker.setStageId(lastConfig.stageId());
                    marker.setStageType(lastConfig.stageType());
                    marker.setRole(lastConfig.role());
                    
                    net.minecraft.nbt.CompoundTag extra = marker.getExtraData();
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_ENEMY_TYPE, lastConfig.enemyType());
                    extra.putInt(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_COUNT, lastConfig.count());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_SWITCH_ID, lastConfig.switchId());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_LOOT_TABLE, lastConfig.lootTable());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_BOSS_ID, lastConfig.bossId());
                    extra.putString(net.ganyusbathwater.oririmod.dungeon.entity.DungeonMarkerEntity.TAG_OBJECTIVE, lastConfig.objectiveText());
                }
            }
            
            serverLevel.addFreshEntity(entity);
            if (!player.isCreative()) {
                player.getItemInHand(hand).shrink(1);
            }
            
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§aPlaced Dungeon Marker! §7Use /data merge entity to edit its NBT. Left-click to destroy."), 
                false
            );
            return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
