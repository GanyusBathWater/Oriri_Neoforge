package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffects;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.Holder;

import java.util.List;

public class Spring extends VestigeItem {

    public Spring(Item.Properties props) {
        super(props, List.of(
                List.of(jumpBoostEffect()),
                List.of(stepHeightEffect()),
                List.of(noFallDamageEffect())
        ));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        this.setUnlockedLevel(stack, this.getMaxLevel());
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide) return;
        if (this.getUnlockedLevel(stack) < this.getMaxLevel()) {
            this.setUnlockedLevel(stack, this.getMaxLevel());
        }
    }

    private static VestigeEffect jumpBoostEffect() {
        return VestigeEffects.mobEffect(MobEffects.JUMP, 0, 200);
    }

    private static VestigeEffect stepHeightEffect() {
        return VestigeEffects.stepHeight(1.0F);
    }

    private static VestigeEffect noFallDamageEffect() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                if (player.fallDistance > 0.0F) {
                    player.fallDistance = 0.0F;
                }
            }
        };
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.spring";
    }
}