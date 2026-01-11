package net.ganyusbathwater.oririmod.item.custom.consumable;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.ModFoods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.getBoolean("eaten_calcium_currant")) {
                persistentData.putBoolean("eaten_calcium_currant", true);

                AttributeInstance safeFallAttribute = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
                if (safeFallAttribute != null) {
                    // Entfernen, falls bereits vorhanden, um Duplikate zu vermeiden
                    safeFallAttribute.removeModifier(SAFE_FALL_MODIFIER_ID);

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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        CompoundTag persistentData = player.getPersistentData();

        if (persistentData.getBoolean("eaten_calcium_currant")) {
            return InteractionResultHolder.pass(itemStack);
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
        Level level = Minecraft.getInstance().level;
        if (level != null && level.isClientSide()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                CompoundTag persistentData = player.getPersistentData();
                if (persistentData.getBoolean("eaten_calcium_currant")) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.calcium_currant.lore"));
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten"));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.calcium_currant.lore"));
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.calcium_currant.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}