package net.ganyusbathwater.oririmod.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.WeakHashMap;

public class ElementalChoirItem extends SwordItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private static final Map<Player, Boolean> CLIENT_IS_NEXT_RIGHT = new WeakHashMap<>();
    private static final Map<Player, Boolean> SERVER_IS_NEXT_RIGHT = new WeakHashMap<>();
    public static final Map<Player, Long> CLIENT_LAST_SWING_TICK = new WeakHashMap<>();
    public static final Map<Player, Long> SERVER_LAST_SWING_TICK = new WeakHashMap<>();

    public ElementalChoirItem(Properties properties) {
        super(Tiers.DIAMOND, properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            // LEFT CLICK (Combo): Use absolute game ticks because vanilla resets attack strength BEFORE onEntitySwing fires
            long currentTick = player.level().getGameTime();
            Map<Player, Long> tickMap = player.level().isClientSide ? CLIENT_LAST_SWING_TICK : SERVER_LAST_SWING_TICK;
            Map<Player, Boolean> rightMap = player.level().isClientSide ? CLIENT_IS_NEXT_RIGHT : SERVER_IS_NEXT_RIGHT;
            long lastSwing = tickMap.getOrDefault(player, 0L);
            
            if (currentTick - lastSwing >= 10) { // 10 ticks = 0.5s cooldown
                tickMap.put(player, currentTick);
                
                boolean isNextRight = rightMap.getOrDefault(player, true);
                String anim = isNextRight ? "attack_sweep_right" : "attack_sweep_left";
                
                if (player.level().isClientSide) {
                    long id = GeoItem.getId(stack);
                    if (id != -1) {
                        triggerAnim(player, id, "AttackController", anim);
                    }
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.ganyusbathwater.oririmod.network.packet.ElementalChoirSweepPayload());
                }
                
                rightMap.put(player, !isNextRight);
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
                
                for (LivingEntity target : targets) {
                    if (target != player && player.distanceTo(target) <= 3.0D) {
                        dealElementalDamage(target, player, level, 2.0f);
                    }
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
        // IdleController: Speeds up dynamically when special attack is active, and PAUSES during normal swings
        controllers.add(new AnimationController<>(this, "IdleController", 5, state -> {
            Player player = net.ganyusbathwater.oririmod.client.render.item.ElementalChoirItemRenderer.currentRenderEntity;
            if (player != null) {
                if (player.getUseItem().getItem() == this) {
                    state.getController().setAnimationSpeed(3.0);
                } else {
                    long currentTick = player.level().getGameTime();
                    long lastSwing = CLIENT_LAST_SWING_TICK.getOrDefault(player, 0L);
                    if (currentTick - lastSwing < 10) {
                        state.getController().setAnimationSpeed(0.0); // Freeze base rotation during swings
                    } else {
                        state.getController().setAnimationSpeed(1.0);
                    }
                }
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("elemental_swords_idle"));
        }));
        
        // SpecialController: Flawlessly handles the hold-on-last-frame state logic independently
        controllers.add(new AnimationController<>(this, "SpecialController", 5, state -> {
            Player player = net.ganyusbathwater.oririmod.client.render.item.ElementalChoirItemRenderer.currentRenderEntity;
            if (player != null && player.getUseItem().getItem() == this) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("elemental_swords_special_attack"));
            }
            return PlayState.STOP;
        }));
        
        // AttackController: Dedicated strictly to Left-Click swing triggers without dampening
        controllers.add(new AnimationController<>(this, "AttackController", 1, state -> PlayState.CONTINUE)
        .triggerableAnim("attack_sweep_left", RawAnimation.begin().thenPlay("elemental_swords_normal_attack_left"))
        .triggerableAnim("attack_sweep_right", RawAnimation.begin().thenPlay("elemental_swords_normal_attack_right")));
    }

    public void executeServerSwing(Player player, ItemStack stack) {
        long currentTick = player.level().getGameTime();
        long lastSwing = SERVER_LAST_SWING_TICK.getOrDefault(player, 0L);
        if (currentTick - lastSwing >= 10) {
            SERVER_LAST_SWING_TICK.put(player, currentTick);
            
            boolean isNextRight = SERVER_IS_NEXT_RIGHT.getOrDefault(player, true);
            String anim = isNextRight ? "attack_sweep_right" : "attack_sweep_left";
            
            long id = GeoItem.getOrAssignId(stack, (ServerLevel) player.level());
            triggerAnim(player, id, "AttackController", anim);
            
            SERVER_IS_NEXT_RIGHT.put(player, !isNextRight);
            dealConeDamage(player);
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
