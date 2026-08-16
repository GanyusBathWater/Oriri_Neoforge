package net.ganyusbathwater.oririmod.item.custom.consumable;

import net.ganyusbathwater.oririmod.item.ModFoods;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class NormalDragonFruitItem extends Item {
    public NormalDragonFruitItem(Properties properties) {
        super(properties.food(ModFoods.DRAGON_FRUIT));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            if (level.random.nextFloat() < 0.25f) {
                // 25% chance to cleanse one random negative effect
                List<MobEffectInstance> negativeEffects = new ArrayList<>();
                for (MobEffectInstance instance : entity.getActiveEffects()) {
                    if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        negativeEffects.add(instance);
                    }
                }

                if (!negativeEffects.isEmpty()) {
                    MobEffectInstance toRemove = negativeEffects.get(level.random.nextInt(negativeEffects.size()));
                    entity.removeEffect(toRemove.getEffect());
                }
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.oririmod.normal_dragon_fruit.cleanse").withStyle(ChatFormatting.GREEN));
    }
}
