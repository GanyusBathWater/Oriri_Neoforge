package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class AoETestItem extends Item {
    public AoETestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !context.getLevel().isClientSide) {
            BlockPos targetPos = context.getClickedPos();
            ServerLevel serverLevel = (ServerLevel) context.getLevel();

            // Spawn a 5-block radius red indicator for 60 ticks (3 seconds)
            // Color is ARGB: 0x88FF0000 (semi-transparent red)
            NetworkHandler.sendAoEIndicatorToPlayersAround(serverLevel, targetPos, 5.0f, 60, 0x88FF0000);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
