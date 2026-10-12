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

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker instanceof Player player) {
            float attackStrength = player.getAttackStrengthScale(0.5F);
            if (attackStrength >= 0.8F) {
                long currentTick = attacker.level().getGameTime();
                CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = data.copyTag();
                long lastHitTick = tag.getLong(NBT_LAST_HIT_TICK);
                int hits = tag.getInt(NBT_CONSECUTIVE_HITS);
                int reqHits = getRequiredHits(stack);

                // If not yet charged and more than 100 ticks (5 seconds) elapsed without hitting, reset combo
                if (hits < reqHits && hits > 0 && (currentTick - lastHitTick > 100L)) {
                    hits = 0;
                }

                if (hits < reqHits) {
                    hits++;
                    tag.putInt(NBT_CONSECUTIVE_HITS, hits);
                    tag.putLong(NBT_LAST_HIT_TICK, currentTick);
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                    if (hits >= reqHits) {
                        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.6F);
                    }
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player) {
            int hits = getConsecutiveHits(stack);
            int reqHits = getRequiredHits(stack);
            if (hits > 0 && hits < reqHits) {
                CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                long lastHitTick = data.copyTag().getLong(NBT_LAST_HIT_TICK);
                if (level.getGameTime() - lastHitTick > 100L) {
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

                    tornado.shootFromOwner(player, 1.25F, 1.0F);
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
            tooltipComponents.add(Component.translatable("tooltip.oririmod.jade_oath.charged")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.oririmod.jade_oath.hits", hits, reqHits)
                    .withStyle(ChatFormatting.GRAY));
        }

        TooltipHelper.addEmptyLine(tooltipComponents);
        TooltipHelper.addLore(tooltipComponents, "tooltip.oririmod.jade_oath.lore");

        tooltipComponents.addAll(buildModTooltip(stack, context, tooltipFlag));
    }
}
