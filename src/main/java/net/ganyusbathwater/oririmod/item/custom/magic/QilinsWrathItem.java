package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.entity.AirSliceEntity;
import net.ganyusbathwater.oririmod.item.custom.CustomSwordItem;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QilinsWrathItem extends CustomSwordItem {
    private static final int MANA_COST = 60;

    public QilinsWrathItem(Tier pTier, Properties pProperties, ModRarity pRarity) {
        super(pTier, pProperties, pRarity);
    }

    @Override
    public int getUseDuration(ItemStack pStack, net.minecraft.world.entity.LivingEntity pEntity) {
        return 72000;
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack pStack) {
        return net.minecraft.world.item.UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack stack = pPlayer.getItemInHand(pHand);
        pPlayer.startUsingItem(pHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, net.minecraft.world.entity.LivingEntity pEntityLiving, int pTimeLeft) {
        if (!(pEntityLiving instanceof Player pPlayer)) {
            return;
        }

        int chargeTime = this.getUseDuration(pStack, pEntityLiving) - pTimeLeft;
        if (chargeTime < 10) { // Require holding for at least 0.5s
            return;
        }

        if (!pLevel.isClientSide) {
            int actualManaCost = ModManaUtil.getActualManaCost(MANA_COST, pStack, null);
            
            if (ModManaUtil.tryConsumeMana((ServerPlayer) pPlayer, actualManaCost, pStack)) {
                // Spawn Air Slice
                AirSliceEntity slice = new AirSliceEntity(pLevel, pPlayer);
                slice.setHomingEnabled(false);
                slice.setCanBreakBlocks(true);
                slice.setColor(0xFF0000); // Red
                slice.setDamage(12.0f);
                
                slice.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 1.5F, 1.0F);
                slice.setRollAngle(pPlayer.getRandom().nextFloat() * 360f);

                pLevel.addFreshEntity(slice);

                pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
                
                pPlayer.getCooldowns().addCooldown(this, 20);
            } else {
                pPlayer.displayClientMessage(Component.translatable("message.oririmod.not_enough_mana").withStyle(ChatFormatting.RED), true);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, net.minecraft.world.item.Item.TooltipContext pContext, java.util.List<Component> pTooltipComponents, net.minecraft.world.item.TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pIsAdvanced);
        
        int actualManaCost = ModManaUtil.getActualManaCost(MANA_COST, pStack, pContext);
        pTooltipComponents.add(Component.translatable("tooltip.oririmod.mana_cost", actualManaCost).withStyle(ChatFormatting.GRAY));
        pTooltipComponents.add(Component.translatable("tooltip.oririmod.qilins_wrath.desc").withStyle(ChatFormatting.DARK_RED));
    }
}
