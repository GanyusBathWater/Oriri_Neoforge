package net.ganyusbathwater.oririmod.item.custom.consumable;

import net.ganyusbathwater.oririmod.item.ModFoods;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MagicMushroom extends Item {

    public MagicMushroom(Properties settings) {
        super(settings.food(ModFoods.MAGIC_MUSHROOM));
    }

    private boolean hasEaten(Player player) {
        return player.getPersistentData().getBoolean("eaten_magic_mushroom");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            if (!hasEaten(player)) {
                player.getPersistentData().putBoolean("eaten_magic_mushroom", true);

                int currentMaxMana = ModManaUtil.getMaxMana(player);
                ModManaUtil.setMaxMana(player, currentMaxMana + 25);
            }
        }
        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide() && hasEaten(player)) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (player.canEat(false)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemStack);
        } else {
            return InteractionResultHolder.fail(itemStack);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.oririmod.magic_mushroom.lore"));

        if (context.level() != null && context.level().isClientSide()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                if (hasEaten(player)) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten").withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.magic_mushroom.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}