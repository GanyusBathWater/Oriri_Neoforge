package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.entity.custom.IOririBoss;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SoulShardItem extends Item {

    public static final String TAG_CAPTURED_ENTITY = "captured_entity";
    public static final String TAG_SOUL_KILLS = "soul_kills";
    public static final String TAG_SOUL_REQUIRED = "soul_required";
    public static final String TAG_SOUL_COMPLETED = "soul_completed";

    public static final int KILLS_COMMON = 32;
    public static final int KILLS_MID_TIER = 16;
    public static final int KILLS_STRONG_RARE = 8;

    private static final Set<EntityType<?>> STRONG_RARE_MOBS = Set.of(
            EntityType.ENDERMAN,
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.WITHER_SKELETON,
            EntityType.EVOKER,
            EntityType.RAVAGER,
            EntityType.PIGLIN_BRUTE,
            EntityType.SHULKER,
            EntityType.PHANTOM,
            EntityType.HOGLIN,
            EntityType.ZOGLIN,
            EntityType.BREEZE,
            EntityType.VINDICATOR,
            EntityType.WITCH,
            EntityType.GUARDIAN,
            EntityType.IRON_GOLEM
    );

    private static final Set<EntityType<?>> MID_TIER_MOBS = Set.of(
            EntityType.CREEPER,
            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.SLIME,
            EntityType.MAGMA_CUBE,
            EntityType.PILLAGER,
            EntityType.DROWNED,
            EntityType.STRAY,
            EntityType.BOGGED,
            EntityType.HUSK,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.PIGLIN,
            EntityType.VEX,
            EntityType.SILVERFISH,
            EntityType.ENDERMITE,
            EntityType.POLAR_BEAR,
            EntityType.LLAMA,
            EntityType.PANDA,
            EntityType.STRIDER,
            EntityType.TURTLE,
            EntityType.SNIFFER,
            EntityType.CAMEL
    );

    public SoulShardItem(Properties properties) {
        super(properties);
    }

    public static boolean isBlockedBoss(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        return entity instanceof IOririBoss
                || type.is(Tags.EntityTypes.BOSSES)
                || type == EntityType.ENDER_DRAGON
                || type == EntityType.WITHER
                || type == EntityType.WARDEN
                || type == EntityType.ELDER_GUARDIAN;
    }

    public static int getRequiredKills(EntityType<?> type, @Nullable LivingEntity entity) {
        if (STRONG_RARE_MOBS.contains(type)) {
            return KILLS_STRONG_RARE;
        }
        if (MID_TIER_MOBS.contains(type)) {
            return KILLS_MID_TIER;
        }
        if (entity != null) {
            float maxHp = entity.getMaxHealth();
            if (maxHp >= 40.0F) {
                return KILLS_STRONG_RARE;
            }
            if (maxHp >= 24.0F) {
                return KILLS_MID_TIER;
            }
        }
        return KILLS_COMMON;
    }

    public static boolean hasBoundEntity(ItemStack stack) {
        if (!stack.is(ModItems.SOUL_SHARD.get())) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(TAG_CAPTURED_ENTITY) && !tag.getString(TAG_CAPTURED_ENTITY).isEmpty();
    }

    @Nullable
    public static String getBoundEntityId(ItemStack stack) {
        if (!stack.is(ModItems.SOUL_SHARD.get())) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(TAG_CAPTURED_ENTITY) ? tag.getString(TAG_CAPTURED_ENTITY) : null;
    }

    public static int getKills(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, tag.getInt(TAG_SOUL_KILLS));
    }

    public static int getRequiredKills(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TAG_SOUL_REQUIRED)) {
            return Math.max(1, tag.getInt(TAG_SOUL_REQUIRED));
        }
        if (tag.contains(TAG_CAPTURED_ENTITY)) {
            Optional<EntityType<?>> opt = EntityType.byString(tag.getString(TAG_CAPTURED_ENTITY));
            if (opt.isPresent()) {
                return getRequiredKills(opt.get(), null);
            }
        }
        return KILLS_COMMON;
    }

    public static boolean isCompleted(ItemStack stack) {
        if (!hasBoundEntity(stack)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getBoolean(TAG_SOUL_COMPLETED)) {
            return true;
        }
        int required = getRequiredKills(stack);
        return tag.getInt(TAG_SOUL_KILLS) >= required;
    }

    public static boolean isReadyForHarvester(ItemStack stack) {
        return stack.is(ModItems.SOUL_SHARD.get()) && isCompleted(stack);
    }

    /**
     * Increments the kill counter on a bound Soul Shard.
     *
     * @return {@code true} if this kill just completed (empowered) the shard.
     */
    public static boolean addKill(ItemStack stack) {
        if (!hasBoundEntity(stack) || isCompleted(stack)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int required = getRequiredKills(stack);
        int nextKills = Math.min(required, Math.max(0, tag.getInt(TAG_SOUL_KILLS)) + 1);
        tag.putInt(TAG_SOUL_KILLS, nextKills);
        tag.putInt(TAG_SOUL_REQUIRED, required);
        boolean completedNow = nextKills >= required;
        tag.putBoolean(TAG_SOUL_COMPLETED, completedNow);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return completedNow;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isCompleted(stack) || super.isFoil(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!entity.isAlive() || entity instanceof Player) {
            return InteractionResult.PASS;
        }

        if (hasBoundEntity(stack)) {
            return InteractionResult.PASS;
        }

        if (isBlockedBoss(entity)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("tooltip.oririmod.soul_shard_boss_blocked").withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        if (!player.level().isClientSide) {
            int required = getRequiredKills(entity.getType(), entity);
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putString(TAG_CAPTURED_ENTITY, EntityType.getKey(entity.getType()).toString());
            tag.putInt(TAG_SOUL_KILLS, 0);
            tag.putInt(TAG_SOUL_REQUIRED, required);
            tag.putBoolean(TAG_SOUL_COMPLETED, false);

            ItemStack boundShard = stack.copy();
            boundShard.setCount(1);
            boundShard.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (stack.isEmpty()) {
                player.setItemInHand(hand, boundShard);
            } else if (!player.getInventory().add(boundShard)) {
                player.drop(boundShard, false);
            }

            player.level().playSound(null, entity.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.9F, 1.15F);
            player.displayClientMessage(
                    Component.translatable("message.oririmod.soul_shard_bound",
                            entity.getType().getDescription(), 0, required).withStyle(ChatFormatting.AQUA),
                    true);
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (hasBoundEntity(stack)) {
            String entityId = getBoundEntityId(stack);
            Optional<EntityType<?>> optType = entityId != null ? EntityType.byString(entityId) : Optional.empty();
            Component mobName = optType.isPresent()
                    ? optType.get().getDescription().copy().withStyle(ChatFormatting.WHITE)
                    : Component.literal(String.valueOf(entityId)).withStyle(ChatFormatting.WHITE);
            tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_shard_entity", mobName)
                    .withStyle(ChatFormatting.GRAY));

            int kills = getKills(stack);
            int required = getRequiredKills(stack);
            if (isCompleted(stack)) {
                tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_shard_empowered", required, required)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_shard_ready_harvester")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_shard_progress", kills, required)
                        .withStyle(ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_shard_needs_collector")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.oririmod.soul_shard_empty")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, tooltipContext, tooltipComponents, tooltipFlag);
    }
}
