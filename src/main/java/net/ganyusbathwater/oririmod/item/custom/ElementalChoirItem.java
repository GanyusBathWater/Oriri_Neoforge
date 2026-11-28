package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.client.physics.ElementalChoirAnimationController;
import net.minecraft.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.WeakHashMap;

public class ElementalChoirItem extends SwordItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final Map<Player, Boolean> CLIENT_IS_NEXT_RIGHT = new WeakHashMap<>();
    private static final Map<Player, Long> CLIENT_LAST_SWING_MS = new WeakHashMap<>();
    public static final Map<Player, Long> SERVER_LAST_SWING_TICK = new WeakHashMap<>();

    public ElementalChoirItem(Properties properties) {
        super(Tiers.DIAMOND, properties);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && entity instanceof Player player && player.level().isClientSide) {
            // Monotonic wall-clock cooldown (500ms) so swings stay responsive even if game ticks stall
            long nowMs = Util.getMillis();
            long lastSwingMs = CLIENT_LAST_SWING_MS.getOrDefault(player, 0L);

            if (nowMs - lastSwingMs >= ElementalChoirAnimationController.SWING_DURATION_MS) {
                CLIENT_LAST_SWING_MS.put(player, nowMs);

                boolean isNextRight = CLIENT_IS_NEXT_RIGHT.getOrDefault(player, true);
                CLIENT_IS_NEXT_RIGHT.put(player, !isNextRight);

                ElementalChoirAnimationController.triggerSwing(player, isNextRight);
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new net.ganyusbathwater.oririmod.network.packet.ElementalChoirSweepPayload());
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            int ticksUsed = this.getUseDuration(stack, livingEntity) - count;

            // Trigger every 10 ticks (0.5s) starting on the 10th tick
            if (ticksUsed > 0 && ticksUsed % 10 == 0) {
                // --- RIGHT CLICK BUZZSAW SPHERICAL AOE ---
                net.minecraft.world.phys.AABB bounds = player.getBoundingBox().inflate(3.0D);
                java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bounds);

                boolean hitAny = false;
                for (LivingEntity target : targets) {
                    if (target != player && player.distanceTo(target) <= 3.0D) {
                        dealElementalDamage(target, player, level, 2.0f);
                        hitAny = true;
                    }
                }
                if (hitAny) {
                    stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                }
            }
        }
        super.onUseTick(level, livingEntity, stack, count);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Driven 100% procedurally by ElementalChoirAnimationController in ElementalChoirItemModel#setCustomAnimations.
    }

    public void executeServerSwing(Player player, ItemStack stack) {
        long currentTick = player.level().getGameTime();
        long lastSwing = SERVER_LAST_SWING_TICK.getOrDefault(player, Long.MIN_VALUE / 2);
        // Handle dimension/world time resets cleanly as well as normal 10-tick cooldown
        if (currentTick < lastSwing || currentTick - lastSwing >= 10) {
            SERVER_LAST_SWING_TICK.put(player, currentTick);
            dealConeDamage(player);
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }

    public void dealConeDamage(Player player) {
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 look2d = new net.minecraft.world.phys.Vec3(look.x, 0, look.z).normalize();

        net.minecraft.world.phys.AABB bounds = player.getBoundingBox().inflate(4.0D);
        java.util.List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, bounds);

        for (LivingEntity target : targets) {
            if (target == player) continue;

            if (player.distanceTo(target) <= 4.0D) {
                net.minecraft.world.phys.Vec3 toTarget = target.position().subtract(player.position());
                net.minecraft.world.phys.Vec3 toTarget2d = new net.minecraft.world.phys.Vec3(toTarget.x, 0, toTarget.z).normalize();

                if (toTarget.lengthSqr() < 1.0D || look2d.dot(toTarget2d) >= 0.5D) {
                    dealElementalDamage(target, player, player.level(), 4.0f);
                }
            }
        }
    }

    private void dealElementalDamage(LivingEntity target, Player player, Level level, float damagePerElement) {
        target.hurt(net.ganyusbathwater.oririmod.damage.ModDamageTypes.getElementalDamage(level, player, net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_FIRE), damagePerElement);
        target.invulnerableTime = 0;

        target.hurt(net.ganyusbathwater.oririmod.damage.ModDamageTypes.getElementalDamage(level, player, net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_WATER), damagePerElement);
        target.invulnerableTime = 0;

        target.hurt(net.ganyusbathwater.oririmod.damage.ModDamageTypes.getElementalDamage(level, player, net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_NATURE), damagePerElement);
        target.invulnerableTime = 0;

        target.hurt(net.ganyusbathwater.oririmod.damage.ModDamageTypes.getElementalDamage(level, player, net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_EARTH), damagePerElement);
        target.invulnerableTime = 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("item.oririmod.elemental_choir.element"));
        net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.elemental_choir.ability");
        net.ganyusbathwater.oririmod.util.TooltipHelper.addLore(tooltipComponents, "tooltip.oririmod.elemental_choir.lore");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
