package net.ganyusbathwater.oririmod.item.custom.consumable;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.ModFoods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MiracleSeaweed extends Item {

    private static final ResourceLocation OXYGEN_BONUS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "miracle_seaweed_oxygen_boost");

    public MiracleSeaweed(Properties settings) {
        super(settings.food(ModFoods.MIRACLE_SEAWEED));
    }

    private boolean hasEaten(Player player) {
        return player.getPersistentData().getBoolean("eaten_miracle_seaweed");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            if (!hasEaten(player)) {
                player.getPersistentData().putBoolean("eaten_miracle_seaweed", true);

                AttributeInstance oxygenAttribute = player.getAttribute(Attributes.OXYGEN_BONUS);
                if (oxygenAttribute != null) {
                    AttributeModifier oxygenModifier = new AttributeModifier(
                            OXYGEN_BONUS_MODIFIER_ID,
                            2.0, // +2 Sauerstoffbonus
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    oxygenAttribute.addPermanentModifier(oxygenModifier);
                }
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
        tooltipComponents.add(Component.translatable("tooltip.oririmod.miracle_seaweed.lore"));

        if (context.level() != null && context.level().isClientSide()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                AttributeInstance oxygenAttribute = player.getAttribute(Attributes.OXYGEN_BONUS);
                boolean hasModifier = oxygenAttribute != null && oxygenAttribute.getModifier(OXYGEN_BONUS_MODIFIER_ID) != null;

                if (hasModifier) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten").withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.miracle_seaweed.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}