package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.ganyusbathwater.oririmod.dungeon.DungeonManager;
import net.ganyusbathwater.oririmod.dungeon.stage.AbstractDungeonStage;
import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DungeonSkipperItem extends Item {

    public DungeonSkipperItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide() && pPlayer instanceof ServerPlayer sp) {
            DungeonInstance instance = DungeonManager.get(sp.serverLevel()).getInstanceForPlayer(sp.getUUID());
            if (instance != null) {
                if (sp.isShiftKeyDown()) {
                    instance.setDebugLoggingEnabled(!instance.isDebugLoggingEnabled());
                    sp.sendSystemMessage(Component.literal(instance.isDebugLoggingEnabled() ? "§aDungeon Debug Logging Enabled." : "§cDungeon Debug Logging Disabled."));
                } else {
                    DungeonStage stage = instance.getActiveStage();
                    if (stage instanceof AbstractDungeonStage abstractStage) {
                        abstractStage.forceComplete();
                        sp.sendSystemMessage(Component.literal("§aForce completed stage: §f" + stage.getDefinition().getStageId()));
                    } else if (stage == null) {
                        sp.sendSystemMessage(Component.literal("§cNo active stage to skip."));
                    } else {
                        sp.sendSystemMessage(Component.literal("§cCannot skip this stage type."));
                    }
                }
            } else {
                if (sp.isShiftKeyDown()) {
                    DungeonManager manager = DungeonManager.get(sp.serverLevel());
                    manager.toggleNextDungeonSpawnAllSecrets();
                    sp.sendSystemMessage(Component.literal(manager.isNextDungeonSpawnAllSecrets() ? "§d[DEBUG] Next dungeon will spawn ALL secret markers!" : "§7[DEBUG] Next dungeon will spawn normal secrets (3)."));
                } else {
                    sp.sendSystemMessage(Component.literal("§cYou are not in an active dungeon instance. Shift-click to toggle Debug Secrets Mode."));
                }
            }
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }
}
