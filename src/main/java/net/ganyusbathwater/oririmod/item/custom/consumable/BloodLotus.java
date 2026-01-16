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

public class BloodLotus extends Item {

    private static final ResourceLocation MAX_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "blood_lotus_health_boost");

    public BloodLotus(Properties settings) {
        super(settings.food(ModFoods.BLOOD_LOTUS));
    }

    private boolean hasHealthModifier(Player player) {
        AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        return healthAttribute != null && healthAttribute.getModifier(MAX_HEALTH_MODIFIER_ID) != null;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null && !hasHealthModifier(player)) {
                AttributeModifier healthModifier = new AttributeModifier(
                        MAX_HEALTH_MODIFIER_ID,
                        4.0, // 2 Herzen = 4 Lebenspunkte
                        AttributeModifier.Operation.ADD_VALUE
                );
                healthAttribute.addPermanentModifier(healthModifier);
                player.heal(4.0F);
            }
        }
        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (hasHealthModifier(player)) {
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
        tooltipComponents.add(Component.translatable("tooltip.oririmod.blood_lotus.lore"));

        // Greift nur auf dem Client auf den Spieler zu.
        // context.level() ist auf dem Client nicht null.
        if (context.level() != null && context.level().isClientSide()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                if (hasHealthModifier(player)) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten").withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.blood_lotus.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}