package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class CandyBag extends VestigeItem {

    private static final String TAG_SATURATION_TICKS = "CandyBagSaturationTicks";

    public CandyBag(Item.Properties props) {
        super(props, List.of(
                List.of(hungerImmunityEffect()),      // Level 1: Hunger-Immunität
                List.of(saturationEffect(1)),         // Level 2: +1 Sättigung pro Minute
                List.of(saturationEffect(2))          // Level 3: +2 Sättigung pro Minute
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

    private static VestigeEffect hungerImmunityEffect() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                if (player.hasEffect(MobEffects.HUNGER)) {
                    player.removeEffect(MobEffects.HUNGER);
                }
            }
        };
    }

    private static VestigeEffect saturationEffect(int saturationPerMinute) {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                if (player.level().isClientSide) return;

                CompoundTag data = player.getPersistentData();
                int ticks = data.contains(TAG_SATURATION_TICKS)
                        ? data.getInt(TAG_SATURATION_TICKS)
                        : 0;
                ticks++;

                if (ticks >= 1200) {
                    ticks = 0;
                    giveSaturation(player, saturationPerMinute);
                }

                data.putInt(TAG_SATURATION_TICKS, ticks);
            }

            @Override
            public void onRemovedFromExtraInventory(ServerPlayer player, ItemStack stack, int lvl) {
                CompoundTag data = player.getPersistentData();
                data.remove(TAG_SATURATION_TICKS);
            }

            private void giveSaturation(ServerPlayer player, int amount) {
                FoodData foodData = player.getFoodData();
                for (int i = 0; i < amount; i++) {
                    foodData.eat(1, 0.5F);
                }
            }
        };
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.candy_bag";
    }
}