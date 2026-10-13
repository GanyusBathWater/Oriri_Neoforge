package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.entity.TornadoEntity;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class JadeOathItem extends CustomSwordItem {

    public static final String NBT_LEVEL = "oriri_level";
    public static final String NBT_CONSECUTIVE_HITS = "consecutive_hits";
    public static final String NBT_LAST_HIT_TICK = "last_hit_tick";
    private static final long MIN_TICKS_BETWEEN_HITS = 4L;
    private static final long COMBO_TIMEOUT_TICKS = 300L; // 15 seconds

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Combo data changes on every hit; without this the sword would bob down/up each time.
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    public JadeOathItem(Tier tier, Properties properties, ModRarity rarity) {
        super(tier, properties, rarity);
    }

    public static int getLevel(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(NBT_LEVEL) ? Math.max(1, tag.getInt(NBT_LEVEL)) : 1;
    }

    public static int getRequiredHits(ItemStack stack) {
        return getLevel(stack) >= 2 ? 3 : 5;
    }

    public static int getConsecutiveHits(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt(NBT_CONSECUTIVE_HITS);
    }

    public static boolean isCharged(ItemStack stack) {
        return getConsecutiveHits(stack) >= getRequiredHits(stack);
    }

    // ── Native Item Progress Bar (renders directly over the item in inventory & hotbar) ──

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getConsecutiveHits(stack) > 0 || stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int hits = getConsecutiveHits(stack);
        if (hits > 0) {
            int req = getRequiredHits(stack);
            return Math.min(13, Math.round(13.0F * Math.min(hits, req) / (float) req));
        }
        return super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int hits = getConsecutiveHits(stack);
        if (hits > 0) {
            // Bright aqua/cyan when fully charged, vibrant jade green while charging
            return isCharged(stack) ? 0x00FFFF : 0x2ECC71;
        }
        return super.getBarColor(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        registerHit(stack, attacker);
        return super.hurtEnemy(stack, target, attacker);
    }

    /** Registers one melee combo hit. Server side only. */
    public static void registerHit(ItemStack stack, LivingEntity attacker) {
        if (attacker.level().isClientSide || !(attacker instanceof Player player)) return;

        long now = attacker.level().getGameTime();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        long lastHitTick = tag.getLong(NBT_LAST_HIT_TICK);
        if (lastHitTick > 0 && now - lastHitTick < MIN_TICKS_BETWEEN_HITS) return;

        int hits = tag.getInt(NBT_CONSECUTIVE_HITS);
        int reqHits = getRequiredHits(stack);
        if (hits >= reqHits) return; // already fully charged

        // Combo expired (15s without a hit) -> start over
        if (hits > 0 && lastHitTick > 0 && now - lastHitTick > COMBO_TIMEOUT_TICKS) {
            hits = 0;
        }

        hits++;
        tag.putInt(NBT_CONSECUTIVE_HITS, hits);
        tag.putLong(NBT_LAST_HIT_TICK, now);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Feedback
        if (hits >= reqHits) {
            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.6F);
            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.3F);
            player.displayClientMessage(
                    Component.translatable("tooltip.oririmod.jade_oath.charged_actionbar"),
                    true
            );
        } else {
            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.displayClientMessage(
                    Component.translatable("tooltip.oririmod.jade_oath.hits_actionbar", hits, reqHits),
                    true
            );
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player) {
            int hits = getConsecutiveHits(stack);
            int reqHits = getRequiredHits(stack);
            if (hits > 0 && hits < reqHits) {
                CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                long lastHitTick = data.copyTag().getLong(NBT_LAST_HIT_TICK);
                if (lastHitTick > 0 && level.getGameTime() - lastHitTick > COMBO_TIMEOUT_TICKS) {
                    CompoundTag tag = data.copyTag();
                    tag.putInt(NBT_CONSECUTIVE_HITS, 0);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isCharged(stack)) {
            if (!player.getCooldowns().isOnCooldown(this)) {
                if (!level.isClientSide) {
                    int lvl = getLevel(stack);
                    // Level 3 deals 2 hearts (4.0F) and has a 3-block pull radius
                    float damage = (lvl >= 3) ? 4.0F : 3.0F;
                    float pullRadius = (lvl >= 3) ? 3.0F : 2.0F;

                    TornadoEntity tornado = new TornadoEntity(level, player);
                    tornado.withGravity(true)
                            .withDamage(damage)
                            .withPullRadius(pullRadius)
                            .withCanPullOwner(false);

                    tornado.shootFromOwner(player, 0.5F, 0.0F);
                    level.addFreshEntity(tornado);

                    // Reset consecutive hits
                    CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                    CompoundTag tag = data.copyTag();
                    tag.putInt(NBT_CONSECUTIVE_HITS, 0);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                    // 5-second cooldown
                    player.getCooldowns().addCooldown(this, 100);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.2F, 1.0F);
                player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int currentLevel = getLevel(stack);
        int hits = getConsecutiveHits(stack);
        int reqHits = getRequiredHits(stack);

        TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.jade_oath.ability");

        if (currentLevel > 1) {
            TooltipHelper.addEmptyLine(tooltipComponents);
            TooltipHelper.addLevelInfo(tooltipComponents, "item.oririmod.jade_oath", currentLevel);
        }

        TooltipHelper.addEmptyLine(tooltipComponents);
        if (hits >= reqHits) {
            tooltipComponents.add(Component.translatable("tooltip.oririmod.jade_oath.status_charged").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.oririmod.jade_oath.status_combo", hits, reqHits).withStyle(ChatFormatting.GREEN));
        }

        TooltipHelper.addEmptyLine(tooltipComponents);
        TooltipHelper.addLore(tooltipComponents, "tooltip.oririmod.jade_oath.lore");

        tooltipComponents.addAll(buildModTooltip(stack, context, tooltipFlag));
    }
}
