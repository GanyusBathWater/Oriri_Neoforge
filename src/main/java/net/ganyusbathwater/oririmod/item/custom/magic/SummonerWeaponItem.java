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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
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

    private final net.ganyusbathwater.oririmod.item.custom.magic.summon.ISummonProfile profile;
    private final ModRarity rarity;
    private final int manaCost;
    private final int cooldownTicks;
    private final int summonDurationTicks;
    private final int chargeDurationTicks;

    private static final float SUMMON_MID_RADIUS = 3.0f;
    private static final float SUMMON_INNER_RADIUS = 2.0f;

    public SummonerWeaponItem(Properties properties, net.ganyusbathwater.oririmod.item.custom.magic.summon.ISummonProfile profile,
            ModRarity rarity, int manaCost, int cooldownTicks, int summonDurationTicks, int chargeDurationTicks) {
        super(properties);
        this.profile = profile;
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
        String descriptionId = this.getDescriptionId();

        net.ganyusbathwater.oririmod.util.TooltipHelper.addLevelInfo(tooltip, descriptionId, Math.min(3, unlockedLevel));

        // Element
        String elementKey = descriptionId + ".element";
        // Element is handled by TooltipHandler

        // Mana Cost
        int actualManaCost = net.ganyusbathwater.oririmod.mana.ModManaUtil.getActualManaCost(this.manaCost, stack, context);
        net.ganyusbathwater.oririmod.util.TooltipHelper.addManaCost(tooltip, actualManaCost);

        // Damage
        tooltip.add(Component.translatable("tooltip.oririmod.damage", this.profile.getDamageTooltip(unlockedLevel)).withStyle(net.minecraft.ChatFormatting.GRAY));

        // Lore
        String loreKey = descriptionId + ".lore";
        net.ganyusbathwater.oririmod.util.TooltipHelper.addLore(tooltip, loreKey);
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

        MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                TEX_MID, SUMMON_MID_RADIUS, -6f, 0xFFFFFFFF, 0f,
                MagicIndicatorClientState.Anchor.WORLD);
        MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                TEX_INNER, SUMMON_INNER_RADIUS, 6f, 0xFFFFFFFF, 0f,
                MagicIndicatorClientState.Anchor.WORLD);

        MagicIndicatorClientState.startFor(living, b.addLayer(mid).addLayer(inner).build());
        MagicIndicatorClientState.spawnChargingParticles(level, living);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide) {
            MagicIndicatorClientState.stopFor(living);
            return;
        }

        int usedTicks = getUseDuration(stack, living) - timeLeft;
        int castingLevel = living.level() != null ? getCastingLevel(stack, living.level()) : 0;
        
        int actualCharge = chargeDurationTicks;
        if (castingLevel > 0) {
            actualCharge = Math.max(1, (int)(chargeDurationTicks * (1.0f - (castingLevel * 0.15f))));
        }

        if (usedTicks < actualCharge) {
            return; // Not charged enough
        }

        if (!(living instanceof Player player)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        BlockHitResult hitResult = raycastToDistance(level, player, 12.0);
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        // Determine the actual EntityType based on weapon type and level
        int weaponLevel = Math.max(1, getUnlockedLevel(stack));
        EntityType<? extends Mob> actualType = this.profile.getSummonType(weaponLevel);

        // Enforce summon cap using attachment
        java.util.List<java.util.UUID> activeSummons = player.getData(net.ganyusbathwater.oririmod.attachment.ModAttachments.ACTIVE_SUMMONS.get());
        activeSummons.removeIf(uuid -> {
            net.minecraft.world.entity.Entity e = serverLevel.getEntity(uuid);
            return e == null || !e.isAlive();
        });

        if (activeSummons.size() >= 3) {
            player.displayClientMessage(Component.literal("Maximum global summons reached! (3/3)").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }

        String sourceId = this.getDescriptionId();
        boolean hasThisBookSummon = false;
        for (java.util.UUID uuid : activeSummons) {
            net.minecraft.world.entity.Entity e = serverLevel.getEntity(uuid);
            if (e != null && sourceId.equals(e.getPersistentData().getString("OririSummonSource"))) {
                hasThisBookSummon = true;
                break;
            }
        }

        if (hasThisBookSummon) {
            player.displayClientMessage(Component.literal("You already have an active summon from this book!").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }

        if (!ModManaUtil.tryConsumeMana(player, manaCost, stack)) {
            return;
        }

        BlockPos spawnPos = hitResult.getBlockPos().relative(hitResult.getDirection());

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
        data.putString("OririSummonSource", sourceId);

        // Prevent the summoned mob from despawning naturally
        summoned.setPersistenceRequired();

        // Rebuild AI: clear all existing goals and target goals, then add ours
        rebuildAI(summoned);

        // Apply Upgrades based on Level
        this.profile.applyUpgrades(summoned, weaponLevel, player);

        // Glowing effect so the summoner can track the mob
        summoned.addEffect(new MobEffectInstance(MobEffects.GLOWING, summonDurationTicks, 0, false, false));

        serverLevel.addFreshEntity(summoned);
        
        // Track the new summon
        activeSummons.add(summoned.getUUID());

        int actualCooldown = cooldownTicks;
        if (castingLevel > 0) {
            actualCooldown = Math.max(1, (int)(cooldownTicks * (1.0f - (castingLevel * 0.15f))));
        }
        player.getCooldowns().addCooldown(this, actualCooldown);
        
        InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND) == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
    }

    private int getCastingLevel(ItemStack stack, Level level) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY)
                .getLevel(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getHolderOrThrow(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.CASTING));
    }

    // Upgrades handled by ISummonProfile

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

    @Override
    public int getEnchantmentValue() {
        return 18; // High enchantability for magic weapons
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
}
