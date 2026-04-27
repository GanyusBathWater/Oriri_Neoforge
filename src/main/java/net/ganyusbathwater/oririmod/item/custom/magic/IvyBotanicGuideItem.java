package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.entity.custom.VenomousPlantEntity;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.util.MagicIndicatorClientState;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class IvyBotanicGuideItem extends Item implements ModRarityCarrier {

    private static final String OWNER_TAG = "OririSummonerUUID";
    private static final int MANA_COST = 30;
    private static final int COOLDOWN_TICKS = 20; // 1 second cooldown
    private static final int CHARGE_DURATION_TICKS = 20; // 1 second charge

    private static final float SUMMON_MID_RADIUS = 3.0f;
    private static final float SUMMON_INNER_RADIUS = 2.0f;

    public IvyBotanicGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public ModRarity getModRarity() {
        return ModRarity.UNCOMMON;
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int unlockedLevel = Math.max(1, getUnlockedLevel(stack));
        String descriptionId = this.getDescriptionId();

        tooltip.add(Component.translatable(descriptionId + ".level", unlockedLevel));
        tooltip.add(Component.translatable(descriptionId + ".level." + Math.min(3, unlockedLevel) + ".description"));

        // Element
        String elementKey = descriptionId + ".element";
        tooltip.add(Component.translatable("tooltip.oririmod.element", Component.translatable(elementKey)).withStyle(net.minecraft.ChatFormatting.GRAY));

        // Mana Cost
        tooltip.add(Component.translatable("tooltip.oririmod.mana_cost", MANA_COST).withStyle(net.minecraft.ChatFormatting.GRAY));

        // Damage
        tooltip.add(Component.translatable("tooltip.oririmod.damage", "5.0").withStyle(net.minecraft.ChatFormatting.GRAY));

        // Lore
        String loreKey = descriptionId + ".lore";
        tooltip.add(Component.translatable(loreKey).withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
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
        if (!level.isClientSide) return;

        BlockHitResult hit = raycastToDistance(level, living, 12.0);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            MagicIndicatorClientState.stopFor(living);
            return;
        }

        BlockPos ground = hit.getBlockPos().relative(hit.getDirection());
        double topY = ground.getY() + 0.05;

        Vec3 groundCenter = new Vec3(ground.getX() + 0.5, topY, ground.getZ() + 0.5);

        ResourceLocation TEX_OUTER = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_outer.png");
        ResourceLocation TEX_MID = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_mid.png");
        ResourceLocation TEX_INNER = ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_inner.png");

        MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                .duration(0)
                .distance(1.6f)
                .spin(4.0f)
                .worldAnchor(groundCenter);

        MagicIndicatorClientState.Indicator.Layer outer = new MagicIndicatorClientState.Indicator.Layer(
                TEX_OUTER, 1.0f, 10f, 0xFFFFFFFF, 0f, MagicIndicatorClientState.Anchor.PLAYER);
        MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                TEX_MID, SUMMON_MID_RADIUS, -6f, 0xFFFFFFFF, 0f, MagicIndicatorClientState.Anchor.WORLD);
        MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                TEX_INNER, SUMMON_INNER_RADIUS, 6f, 0xFFFFFFFF, 0f, MagicIndicatorClientState.Anchor.WORLD);

        MagicIndicatorClientState.startFor(living, b.addLayer(outer).addLayer(mid).addLayer(inner).build());
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide) {
            MagicIndicatorClientState.stopFor(living);
            return;
        }

        int usedTicks = getUseDuration(stack, living) - timeLeft;
        if (usedTicks < CHARGE_DURATION_TICKS) {
            return;
        }

        if (!(living instanceof Player player)) {
            return;
        }

        if (!ModManaUtil.tryConsumeMana(player, MANA_COST, stack)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        BlockHitResult hitResult = raycastToDistance(level, player, 12.0);
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        BlockPos spawnPos = hitResult.getBlockPos().relative(hitResult.getDirection());

        int weaponLevel = Math.max(1, getUnlockedLevel(stack));
        int maxSummons = Math.min(3, weaponLevel);

        // Find existing summons and enforce cap. Use an infinite range basically, or a very large one, to ensure we catch all of them.
        String playerUUID = player.getStringUUID();
        List<VenomousPlantEntity> existingPlants = serverLevel.getEntitiesOfClass(VenomousPlantEntity.class, player.getBoundingBox().inflate(128.0D), 
            entity -> entity.getPersistentData().getString(OWNER_TAG).equals(playerUUID));

        if (existingPlants.size() >= maxSummons) {
            // Sort by oldest first (highest tickCount)
            existingPlants.sort(Comparator.comparingInt((VenomousPlantEntity e) -> e.tickCount).reversed());
            
            // Remove the oldest ones to make room for the new one
            int toRemove = existingPlants.size() - maxSummons + 1;
            for (int i = 0; i < toRemove; i++) {
                existingPlants.get(i).discard();
            }
        }

        // Spawn new Venomous Plant
        VenomousPlantEntity newPlant = ModEntities.VENOMOUS_PLANT.get().create(serverLevel);
        if (newPlant != null) {
            newPlant.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYRot(), 0.0F);
            newPlant.getPersistentData().putString(OWNER_TAG, playerUUID);
            newPlant.getPersistentData().putBoolean("OririSummoned", true);
            newPlant.getPersistentData().putInt("OririSummonTicks", 600);
            
            // Allow the plant to persist in the world
            newPlant.setPersistenceRequired();

            // Clear its default target goal (which targets players) and give it the summon target goal
            new java.util.ArrayList<>(newPlant.targetSelector.getAvailableGoals())
                    .forEach(g -> newPlant.targetSelector.removeGoal(g.getGoal()));
            newPlant.targetSelector.addGoal(1, new net.ganyusbathwater.oririmod.entity.ai.SummonedMobGoal(newPlant));
            
            serverLevel.addFreshEntity(newPlant);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
    }

    private static BlockHitResult raycastToDistance(Level level, LivingEntity living, double range) {
        Vec3 eye = living.getEyePosition(1.0f);
        Vec3 look = living.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        return level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, living));
    }
}
