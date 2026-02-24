package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.entity.ai.SummonedMobGoal;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.util.MagicIndicatorClientState;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A summoner-type magic weapon with charging mechanics.
 */
public class SummonerWeaponItem extends Item implements ModRarityCarrier {

    private static final String OWNER_TAG = "OririSummonerUUID";
    private static final String SUMMONED_TAG = "OririSummoned";
    private static final String TICKS_TAG = "OririSummonTicks";

    private final EntityType<? extends Mob> summonType;
    private final ModRarity rarity;
    private final int manaCost;
    private final int cooldownTicks;
    private final int summonDurationTicks;
    private final int chargeDurationTicks;

    private static final float SUMMON_MID_RADIUS = 3.0f;
    private static final float SUMMON_INNER_RADIUS = 2.0f;

    public SummonerWeaponItem(Properties properties, EntityType<? extends Mob> summonType,
            ModRarity rarity, int manaCost, int cooldownTicks, int summonDurationTicks, int chargeDurationTicks) {
        super(properties);
        this.summonType = summonType;
        this.rarity = rarity;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.summonDurationTicks = summonDurationTicks;
        this.chargeDurationTicks = chargeDurationTicks;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    public static int getUnlockedLevel(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            CompoundTag customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (customData.contains("oriri_level")) {
                return customData.getInt("oriri_level");
            }
        }
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        int unlockedLevel = Math.max(1, getUnlockedLevel(stack));

        tooltip.add(Component.translatable(this.getDescriptionId() + ".level", unlockedLevel));

        for (int i = 1; i <= Math.min(3, unlockedLevel); i++) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".level." + i + ".description"));
        }

        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore"));
        tooltip.addAll(buildModTooltip(stack, context, flag));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide)
            return;

        BlockHitResult hit = raycastToDistance(level, living, 12.0);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            MagicIndicatorClientState.stopFor(living);
            return;
        }

        BlockPos ground = hit.getBlockPos().relative(hit.getDirection());
        double topY = ground.getY() + 0.05; // Slightly above block base to avoid Z-fighting

        Vec3 groundCenter = new Vec3(ground.getX() + 0.5, topY, ground.getZ() + 0.5);

        ResourceLocation TEX_OUTER = ResourceLocation.fromNamespaceAndPath("oririmod",
                "textures/effect/magic_circles/arcane_outer.png");
        ResourceLocation TEX_MID = ResourceLocation.fromNamespaceAndPath("oririmod",
                "textures/effect/magic_circles/arcane_mid.png");
        ResourceLocation TEX_INNER = ResourceLocation.fromNamespaceAndPath("oririmod",
                "textures/effect/magic_circles/arcane_inner.png");

        MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                .duration(0)
                .distance(1.6f)
                .spin(4.0f)
                .worldAnchor(groundCenter);

        MagicIndicatorClientState.Indicator.Layer outer = new MagicIndicatorClientState.Indicator.Layer(
                TEX_OUTER, 1.0f, 10f, 0xFFFFFFFF, 0f,
                MagicIndicatorClientState.Anchor.PLAYER);
        MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                TEX_MID, SUMMON_MID_RADIUS, -6f, 0xFFFFFFFF, 0f,
                MagicIndicatorClientState.Anchor.WORLD);
        MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                TEX_INNER, SUMMON_INNER_RADIUS, 6f, 0xFFFFFFFF, 0f,
                MagicIndicatorClientState.Anchor.WORLD);

        MagicIndicatorClientState.startFor(living, b.addLayer(outer).addLayer(mid).addLayer(inner).build());
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide) {
            MagicIndicatorClientState.stopFor(living);
            return;
        }

        int usedTicks = getUseDuration(stack, living) - timeLeft;
        if (usedTicks < chargeDurationTicks) {
            return; // Not charged enough
        }

        if (!(living instanceof Player player)) {
            return;
        }

        if (!ModManaUtil.tryConsumeMana(player, manaCost)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        BlockHitResult hitResult = raycastToDistance(level, player, 12.0);
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        BlockPos spawnPos = hitResult.getBlockPos().relative(hitResult.getDirection());

        // Determine the actual EntityType based on weapon type and level
        int weaponLevel = Math.max(1, getUnlockedLevel(stack));
        EntityType<? extends Mob> actualType = this.summonType;
        if (actualType == EntityType.SKELETON) {
            if (weaponLevel == 2)
                actualType = (EntityType<? extends Mob>) EntityType.BOGGED;
            if (weaponLevel >= 3)
                actualType = (EntityType<? extends Mob>) EntityType.STRAY;
        }

        // Spawn the mob
        Mob summoned = actualType.create(serverLevel);
        if (summoned == null) {
            return;
        }

        // Position at the target location (centered)
        summoned.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), 0.0F);

        // Store ownership and lifespan data
        CompoundTag data = summoned.getPersistentData();
        data.putString(OWNER_TAG, player.getStringUUID());
        data.putBoolean(SUMMONED_TAG, true);
        data.putInt(TICKS_TAG, summonDurationTicks);

        // Prevent the summoned mob from despawning naturally
        summoned.setPersistenceRequired();

        // Rebuild AI: clear all existing goals and target goals, then add ours
        rebuildAI(summoned);

        // Apply Upgrades based on Level
        upgradeSummon(summoned, weaponLevel, player);

        // Glowing effect so the summoner can track the mob
        summoned.addEffect(new MobEffectInstance(MobEffects.GLOWING, summonDurationTicks, 0, false, false));

        serverLevel.addFreshEntity(summoned);

        player.getCooldowns().addCooldown(this, cooldownTicks);
    }

    private void upgradeSummon(Mob summoned, int level, Player player) {
        if (level <= 1 && summoned.getType() == EntityType.SLIME) {
            ((Slime) summoned).setSize(2, true);
        } else if (level <= 1 && summoned.getType() == EntityType.MAGMA_CUBE) {
            ((Slime) summoned).setSize(2, true);
        }

        if (level >= 2) {
            if (summoned.getType() == EntityType.ZOMBIE) {
                summoned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            } else if (summoned.getType() == EntityType.BOGGED || summoned.getType() == EntityType.STRAY) {
                ItemStack bow = new ItemStack(Items.BOW);
                bow.enchant(player.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.POWER), 2);
                bow.enchant(player.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.PUNCH), 1);
                summoned.setItemSlot(EquipmentSlot.MAINHAND, bow);
            } else if (summoned.getType() == EntityType.IRON_GOLEM) {
                summoned.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 0, false, false));
            } else if (summoned.getType() == EntityType.BLAZE) {
                AttributeInstance maxHealth = summoned.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.addPermanentModifier(
                            new AttributeModifier(ResourceLocation.withDefaultNamespace("blaze_level2_health"), 20.0,
                                    AttributeModifier.Operation.ADD_VALUE));
                    summoned.setHealth(summoned.getMaxHealth());
                }
            } else if (summoned.getType() == EntityType.SLIME || summoned.getType() == EntityType.MAGMA_CUBE) {
                ((Slime) summoned).setSize(4, true);
            }
        }

        if (level >= 3) {
            if (summoned.getType() == EntityType.ZOMBIE) {
                summoned.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                summoned.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                summoned.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                summoned.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            } else if (summoned.getType() == EntityType.STRAY) {
                summoned.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                summoned.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                summoned.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                summoned.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            } else if (summoned.getType() == EntityType.IRON_GOLEM) {
                AttributeInstance maxHealth = summoned.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.addPermanentModifier(
                            new AttributeModifier(ResourceLocation.withDefaultNamespace("golem_level3_health"), 50.0,
                                    AttributeModifier.Operation.ADD_VALUE));
                    summoned.setHealth(summoned.getMaxHealth());
                }
            } else if (summoned.getType() == EntityType.BLAZE) {
                AttributeInstance maxHealth = summoned.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.addPermanentModifier(
                            new AttributeModifier(ResourceLocation.withDefaultNamespace("blaze_level3_health"), 40.0,
                                    AttributeModifier.Operation.ADD_VALUE));
                    summoned.setHealth(summoned.getMaxHealth());
                }
            } else if (summoned.getType() == EntityType.SLIME || summoned.getType() == EntityType.MAGMA_CUBE) {
                AttributeInstance maxHealth = summoned.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.addPermanentModifier(
                            new AttributeModifier(ResourceLocation.withDefaultNamespace("slime_level3_health"), 40.0,
                                    AttributeModifier.Operation.ADD_VALUE));
                    summoned.setHealth(summoned.getMaxHealth());
                }
            }
        }
    }

    public static void rebuildAI(Mob mob) {
        // Clear all existing target goals
        new ArrayList<>(mob.targetSelector.getAvailableGoals())
                .forEach(g -> mob.targetSelector.removeGoal(g.getGoal()));

        // Clear all existing goals
        new ArrayList<>(mob.goalSelector.getAvailableGoals()).forEach(g -> mob.goalSelector.removeGoal(g.getGoal()));

        // Add basic movement/behavior goals
        mob.goalSelector.addGoal(0, new FloatGoal(mob));
        if (mob instanceof PathfinderMob pathfinderMob) {
            mob.goalSelector.addGoal(2, new MeleeAttackGoal(pathfinderMob, 1.0D, true));
            mob.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(pathfinderMob, 1.0D));
            mob.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinderMob) {
                @Override
                protected boolean canAttack(LivingEntity target,
                        net.minecraft.world.entity.ai.targeting.TargetingConditions conditions) {
                    if (target == null)
                        return false;
                    String ownerStr = mob.getPersistentData().getString(OWNER_TAG);
                    if (!ownerStr.isEmpty()) {
                        if (target.getStringUUID().equals(ownerStr))
                            return false;
                        if (target.getPersistentData().getString(OWNER_TAG).equals(ownerStr))
                            return false;
                    }
                    return super.canAttack(target, conditions);
                }
            }.setAlertOthers());
        }
        mob.goalSelector.addGoal(6, new RandomLookAroundGoal(mob));

        // Add our custom targeting
        mob.targetSelector.addGoal(2, new SummonedMobGoal(mob));
    }

    private static BlockHitResult raycastToDistance(Level level, LivingEntity living, double range) {
        Vec3 eye = living.getEyePosition(1.0f);
        Vec3 look = living.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        return level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY,
                living));
    }

    private static double getTopSurfaceY(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty())
            return pos.getY() + 1.0;
        return pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
    }
}
