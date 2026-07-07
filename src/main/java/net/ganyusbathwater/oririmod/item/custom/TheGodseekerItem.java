package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.world.GodsTrialData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TheGodseekerItem extends Item {

    public TheGodseekerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (!player.hasPermissions(2)) {
                player.displayClientMessage(Component.literal("Only operators can use this item.").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(itemstack);
            }

            ServerLevel serverLevel = (ServerLevel) level;

            // Lockout Mechanism: Check if any player on the server is near a boss or in a boss arena
            boolean isBossFightActive = false;
            for (ServerPlayer serverPlayer : serverLevel.getServer().getPlayerList().getPlayers()) {
                ServerLevel playerLevel = serverPlayer.serverLevel();
                
                // Check for Ender Dragon in the same dimension
                List<EnderDragon> dragons = playerLevel.getEntitiesOfClass(EnderDragon.class, serverPlayer.getBoundingBox().inflate(256.0D));
                if (!dragons.isEmpty() && dragons.stream().anyMatch(Entity::isAlive)) {
                    isBossFightActive = true;
                    break;
                }

                // Check for Wither in the same dimension
                List<WitherBoss> withers = playerLevel.getEntitiesOfClass(WitherBoss.class, serverPlayer.getBoundingBox().inflate(128.0D));
                if (!withers.isEmpty() && withers.stream().anyMatch(Entity::isAlive)) {
                    isBossFightActive = true;
                    break;
                }
            }

            if (isBossFightActive) {
                player.displayClientMessage(Component.literal("The Gods refuse, unless the right circumstances are given").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 1.0F, 0.5F);
                return InteractionResultHolder.fail(itemstack);
            }

            // Toggle Difficulty
            GodsTrialData data = GodsTrialData.get(serverLevel);
            boolean newState = !data.isActive();
            data.setActive(newState);

            if (newState) {
                serverLevel.getServer().setDifficulty(net.minecraft.world.Difficulty.HARD, true);
                serverLevel.getServer().getWorldData().setDifficultyLocked(true);
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(Component.literal("The Gods' gaze falls upon this world. God's Trial has begun!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 0.5F);
            } else {
                serverLevel.getServer().getWorldData().setDifficultyLocked(false);
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(Component.literal("The Gods avert their gaze. God's Trial has ended.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.oririmod.the_godseeker.lore").withStyle(ChatFormatting.DARK_GRAY));
    }
}
