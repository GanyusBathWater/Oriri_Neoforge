package net.ganyusbathwater.oririmod.item.custom.consumable;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.ModFoods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
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

public class CalciumCurrant extends Item {

    private static final ResourceLocation SAFE_FALL_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "calcium_currant_safe_fall_boost");

    public CalciumCurrant(Properties settings) {
        super(settings.food(ModFoods.CALCIUM_CURRANT));
    }

    private boolean hasEaten(Player player) {
        return player.getPersistentData().getBoolean("eaten_calcium_currant");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            if (!hasEaten(player)) {
                player.getPersistentData().putBoolean("eaten_calcium_currant", true);

                AttributeInstance safeFallAttribute = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
                if (safeFallAttribute != null) {
                    AttributeModifier safeFallModifier = new AttributeModifier(
                            SAFE_FALL_MODIFIER_ID,
                            3.0, // +3 sichere Fallhöhe
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    safeFallAttribute.addPermanentModifier(safeFallModifier);
                }
            }
        }
        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Die Überprüfung sollte nur auf dem Server stattfinden, da getPersistentData serverseitig ist.
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.oririmod.calcium_currant.lore"));

        if (context.level() != null && context.level().isClientSide()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {

                AttributeInstance safeFallAttribute = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
                boolean hasModifier = safeFallAttribute != null && safeFallAttribute.getModifier(SAFE_FALL_MODIFIER_ID) != null;

                if (hasModifier) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten").withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.calcium_currant.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}