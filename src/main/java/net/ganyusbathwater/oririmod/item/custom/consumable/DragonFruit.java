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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

public class DragonFruit extends Item {

    public DragonFruit(Properties settings) {
        super(settings.food(ModFoods.DRAGON_FRUIT));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        // Die Logik wird serverseitig ausgeführt, um Manipulationen zu verhindern
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.getBoolean("eaten_dragon_fruit")) {
                persistentData.putBoolean("eaten_dragon_fruit", true);

                CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
                    inventory.addPermanentSlotModifier(
                            "vestiges",
                            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestiges_unlock"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                });
            }
        }
        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        CompoundTag persistentData = player.getPersistentData();

        if (persistentData.getBoolean("eaten_dragon_fruit")) {
            // Blockiert die Verwendung komplett, ohne eine Animation zu zeigen.
            return InteractionResultHolder.pass(itemStack);
        }

        // Ruft die Standard-Essenslogik auf, wenn der Spieler essen kann.
        if (player.canEat(false)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemStack);
        } else {
            // Verhindert die Verwendung, wenn der Spieler nicht essen kann (z.B. voller Hunger).
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
                if (persistentData.getBoolean("eaten_dragon_fruit")) {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.dragon_fruit.lore"));
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.consumable.eaten"));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.dragon_fruit.lore"));
                    tooltipComponents.add(Component.translatable("tooltip.oririmod.dragon_fruit.uneaten").withStyle(ChatFormatting.GRAY));
                }
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}