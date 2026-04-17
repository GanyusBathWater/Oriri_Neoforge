package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.entity.MagicProjectileEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class MagicDebugStickItem extends Item {
    private static final String NBT_ELEMENT = "selected_element";

    public MagicDebugStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleElement(stack, player);
            }
            return InteractionResultHolder.success(stack);
        } else {
            if (!level.isClientSide) {
                shootProjectile(level, player, stack);
            }
            return InteractionResultHolder.success(stack);
        }
    }

    private void cycleElement(ItemStack stack, Player player) {
        Element current = getSelectedElement(stack);
        Element[] elements = Element.values();
        int nextIndex = (current.ordinal() + 1) % elements.length;
        
        // Skip PHYSICAL as requested
        if (elements[nextIndex] == Element.PHYSICAL) {
            nextIndex = (nextIndex + 1) % elements.length;
        }

        setSelectedElement(stack, elements[nextIndex]);
        player.displayClientMessage(Component.literal("Selected Element: " + elements[nextIndex].name()), true);
    }

    private void shootProjectile(Level level, Player player, ItemStack stack) {
        Element element = getSelectedElement(stack);
        MagicProjectileEntity projectile = new MagicProjectileEntity(level, player, 0, 0, 0);
        projectile.setElement(element);
        projectile.setDamage(4.0F);

        // Position at eye height and offset forward so it doesn't hit the player
        net.minecraft.world.phys.Vec3 view = player.getViewVector(1.0f);
        projectile.setPos(player.getX() + view.x * 1.5D, player.getEyeY() + view.y * 1.5D, player.getZ() + view.z * 1.5D);

        projectile.shoot(view.x, view.y, view.z, 1.5F, 0.0F);

        level.addFreshEntity(projectile);
    }

    public static Element getSelectedElement(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_ELEMENT)) {
                return Element.values()[tag.getInt(NBT_ELEMENT)];
            }
        }
        return Element.FIRE;
    }

    public static void setSelectedElement(ItemStack stack, Element element) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt(NBT_ELEMENT, element.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Selected Element: ").append(Component.literal(getSelectedElement(stack).name())));
        tooltip.add(Component.literal("Right-click to shoot"));
        tooltip.add(Component.literal("Sneak + Right-click to cycle elements"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
