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

public class DevilFruit extends Item {

    private static final ResourceLocation ENTITY_RANGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "devil_fruit_entity_range_boost");
    private static final ResourceLocation BLOCK_RANGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "devil_fruit_block_range_boost");

    public DevilFruit(Properties settings) {
        super(settings.food(ModFoods.DEVIL_FRUIT));
    }

    private boolean hasEaten(Player player) {
        return player.getPersistentData().getBoolean("eaten_devil_fruit");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            if (!hasEaten(player)) {
                player.getPersistentData().putBoolean("eaten_devil_fruit", true);

                // Entity Interaction Range
                AttributeInstance entityRangeAttribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                if (entityRangeAttribute != null) {
                    AttributeModifier entityRangeModifier = new AttributeModifier(
                            ENTITY_RANGE_MODIFIER_ID,
                            1.0,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    entityRangeAttribute.addPermanentModifier(entityRangeModifier);
                }

                // Block Interaction Range
                AttributeInstance blockRangeAttribute = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
                if (blockRangeAttribute != null) {
                    AttributeModifier blockRangeModifier = new AttributeModifier(
                            BLOCK_RANGE_MODIFIER_ID,
                            1.0,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    blockRangeAttribute.addPermanentModifier(blockRangeModifier);
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
        tooltipComponents.add(Component.translatable("tooltip.oririmod.devil_fruit.lore"));

        if (context.level() != null && context.level().isClientSide()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                AttributeInstance entityRangeAttribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                AttributeInstance blockRangeAttribute = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
                boolean hasModifier = (entityRangeAttribute != null && entityRangeAttribute.getModifier(ENTITY_RANGE_MODIFIER_ID) != null) ||
                        (blockRangeAttribute != null && blockRangeAttribute.getModifier(BLOCK_RANGE_MODIFIER_ID) != null);

                if (hasModifier) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten").withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.devil_fruit.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}