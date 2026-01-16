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
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

public class DragonFruit extends Item {

    private static final ResourceLocation VESTIGES_UNLOCK_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestiges_unlock");
    private static final ResourceLocation LUCK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dragon_fruit_luck");

    public DragonFruit(Properties settings) {
        super(settings.food(ModFoods.DRAGON_FRUIT));
    }

    private boolean hasEaten(Player player) {
        return player.getPersistentData().getBoolean("eaten_dragon_fruit");
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            if (!hasEaten(player)) {
                player.getPersistentData().putBoolean("eaten_dragon_fruit", true);

                // Slot-Modifikator für Curios hinzufügen
                CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
                    inventory.addPermanentSlotModifier(
                            "vestiges",
                            VESTIGES_UNLOCK_ID,
                            1,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                });

                // Attribut-Modifikator als Indikator für den Client hinzufügen
                AttributeInstance luckAttribute = player.getAttribute(Attributes.LUCK);
                if (luckAttribute != null && luckAttribute.getModifier(LUCK_MODIFIER_ID) == null) {
                    AttributeModifier luckModifier = new AttributeModifier(
                            LUCK_MODIFIER_ID,
                            0.0, // Wert ist egal, dient nur als Marker
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    luckAttribute.addPermanentModifier(luckModifier);
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
        tooltipComponents.add(Component.translatable("tooltip.oririmod.dragon_fruit.lore"));

        if (context.level() != null && context.level().isClientSide()) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                // Überprüfen, ob der Attribut-Modifikator vorhanden ist, da dies clientseitig zuverlässig ist
                AttributeInstance luckAttribute = player.getAttribute(Attributes.LUCK);
                boolean hasModifier = luckAttribute != null && luckAttribute.getModifier(LUCK_MODIFIER_ID) != null;

                if (hasModifier) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten").withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.dragon_fruit.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}