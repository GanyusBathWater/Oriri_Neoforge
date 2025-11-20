// java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Witherrose extends VestigeItem {

    public Witherrose(Item.Properties props) {
        super(props, List.of(
                List.of(witherImmunityEffect()),
                List.of(witherDefenseMarker()),
                List.of(witherOffenseMarker())
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

    private static VestigeEffect witherImmunityEffect() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                if (player.hasEffect(MobEffects.WITHER)) {
                    player.removeEffect(MobEffects.WITHER);
                }
            }
        };
    }

    // Level 2/3: Leere Effekte, dienen nur dazu, dass das Level im Vestige-System existiert.
    private static VestigeEffect witherDefenseMarker() {
        return new VestigeEffect() {
            // keine Overrides nötig, Marker für externen Damage-Handler
        };
    }

    private static VestigeEffect witherOffenseMarker() {
        return new VestigeEffect() {
            // keine Overrides nötig, Marker für externen Damage-Handler
        };
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.wither_rose";
    }
}