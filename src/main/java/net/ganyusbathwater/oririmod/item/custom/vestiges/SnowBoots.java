// java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class SnowBoots extends VestigeItem {

    public SnowBoots(Item.Properties props) {
        super(props, List.of(
                List.of(speedOnColdBlocks()),      // Level 1
                List.of(noPowderSnowSink()),       // Level 2
                List.of(noKnockbackOnColdBlocks()) // Level 3
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

    // Level 1: Speed auf allen Blöcken im Tag (Snow-Layer + Snow-Block etc.)
    private static VestigeEffect speedOnColdBlocks() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                ServerLevel level = player.serverLevel();

                BlockPos below = player.blockPosition().below();
                BlockPos onPos = player.getOnPos();

                boolean valid =
                        level.getBlockState(below).is(ModTags.Blocks.SNOW_BOOTS_VALID_BLOCKS) ||
                                level.getBlockState(onPos).is(ModTags.Blocks.SNOW_BOOTS_VALID_BLOCKS);

                if (!valid) return;

                int amplifier = 0; // Speed I
                int duration = 40;

                MobEffectInstance existing = player.getEffect(MobEffects.MOVEMENT_SPEED);
                if (existing == null
                        || existing.getAmplifier() < amplifier
                        || existing.getDuration() <= duration / 2) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED,
                            duration,
                            amplifier,
                            true,
                            false,
                            true
                    ));
                }
            }
        };
    }

    // Level 2: nicht in Powder Snow versinken
    private static VestigeEffect noPowderSnowSink() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                ServerLevel level = player.serverLevel();
                BlockPos pos = player.blockPosition();

                if (!level.getBlockState(pos).is(Blocks.POWDER_SNOW)) {
                    return;
                }

                player.setIsInPowderSnow(false);

                if (player.getDeltaMovement().y < 0.1D) {
                    player.setDeltaMovement(
                            player.getDeltaMovement().x,
                            0.1D,
                            player.getDeltaMovement().z
                    );
                    player.hasImpulse = true;
                }
            }
        };
    }

    // Level 3: Kein (bzw. stark reduzierter) Rückstoß auf kalten Blöcken
    private static VestigeEffect noKnockbackOnColdBlocks() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                ServerLevel level = player.serverLevel();

                BlockPos below = player.blockPosition().below();
                BlockPos onPos = player.getOnPos();

                boolean onColdBlock =
                        level.getBlockState(below).is(ModTags.Blocks.SNOW_BOOTS_VALID_BLOCKS) ||
                                level.getBlockState(onPos).is(ModTags.Blocks.SNOW_BOOTS_VALID_BLOCKS);

                if (!onColdBlock) return;

                // Eingehenden Knockback stark dämpfen:
                var motion = player.getDeltaMovement();

                // Horizontal fast auf 0 ziehen; Y nicht anfassen (für Hits, Sprünge etc.)
                double factor = 0.1D; // 0.0D = kompletter Knockback aus, 0.1D = minimal
                double newVx = motion.x * factor;
                double newVz = motion.z * factor;

                player.setDeltaMovement(newVx, motion.y, newVz);
                player.hasImpulse = true;
            }
        };
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.snow_boots";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }
}