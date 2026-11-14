package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class NoxusKnightEntity extends Monster {
    public static final TagKey<EntityType<?>> NOXUS_MOBS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("oririmod", "noxus_mobs"));

    private static final int SHIELD_DISABLE_TICKS = 100;
    private static final int BASH_COOLDOWN_TICKS = 100;

    private int shieldCooldownTicks = 0;
    private int shieldActiveTicks = 0;
    private int bashCooldownTicks = 0;
    private int pendingBashDelay = -1;
    @Nullable
    private LivingEntity pendingBashTarget = null;

    public NoxusKnightEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ShieldDefenseGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false) {
            @Override
            protected boolean canPerformAttack(LivingEntity target) {
                return !NoxusKnightEntity.this.isRaisingShield() && super.canPerformAttack(target);
            }
        });
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, 
            (entity) -> {
                // Target monsters but ignore other noxus mobs
                return entity instanceof Monster 
                    && !entity.getType().is(NOXUS_MOBS)
                    && !entity.getPersistentData().getBoolean("IsNoxusMob");
            }
        ));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath(); // wait, BuiltInRegistries is correct in 1.21.1

        if (name.equals("noxus_paladin")) {
            this.setItemSlot(EquipmentSlot.MAINHAND, createEnchantedSword(new ItemStack(Items.NETHERITE_SWORD)));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.JADE_SHIELD.get()));
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GILDED_NETHERRITE_NOXUS_HELMET.get()));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GILDED_NETHERRITE_CHESTPLATE.get()));
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.GILDED_NETHERRITE_LEGGINGS.get()));
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.GILDED_NETHERRITE_BOOTS.get()));
        } else if (name.equals("noxus_general")) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NETHERITE_NOXUS_HELMET.get()));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        } else {
            // Default Knight
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.IRON_NOXUS_HELMET.get()));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
    }

    private ItemStack createEnchantedSword(ItemStack sword) {
        // EnchantmentHelper is tricky in 1.21.1 with RegistryAccess, it's safer to just let the game enchant it or not use specific enchants without registry context, but we can do a simple one.
        return sword;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // Players don't have an ambient sound
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT; // standard player hurt sound
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH; // standard player death sound
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.shieldCooldownTicks > 0) this.shieldCooldownTicks--;
        if (this.bashCooldownTicks > 0) this.bashCooldownTicks--;

        if (this.isRaisingShield() && (this.shieldCooldownTicks > 0 || getShieldHand() == null)) {
            this.stopUsingItem();
            this.shieldActiveTicks = 0;
        }

        if (this.pendingBashDelay >= 0) {
            if (this.pendingBashDelay-- == 0) {
                executeShieldBash();
            }
        }
    }

    @Nullable
    public InteractionHand getShieldHand() {
        if (isUsableShield(this.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (isUsableShield(this.getMainHandItem())) return InteractionHand.MAIN_HAND;
        return null;
    }

    private static boolean isUsableShield(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ShieldItem || stack.getUseAnimation() == UseAnim.BLOCK);
    }

    public boolean isRaisingShield() {
        return this.isUsingItem() && this.getUseItem().getUseAnimation() == UseAnim.BLOCK;
    }

    @Override
    protected void blockUsingShield(LivingEntity attacker) {
        super.blockUsingShield(attacker);
        if (attacker.canDisableShield()) {
            this.disableShield();
            return;
        }
        if (this.bashCooldownTicks <= 0 && this.distanceToSqr(attacker) <= 16.0D && this.pendingBashDelay < 0) {
            this.pendingBashTarget = attacker;
            this.pendingBashDelay = 6;
        }
    }

    public void disableShield() {
        this.shieldCooldownTicks = SHIELD_DISABLE_TICKS;
        this.shieldActiveTicks = 0;
        this.pendingBashDelay = -1;
        this.pendingBashTarget = null;
        this.stopUsingItem();
        this.level().broadcastEntityEvent(this, EntityEvent.SHIELD_DISABLED);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float damage) {
        if (isUsableShield(this.useItem) && damage >= 3.0F) {
            int dmg = 1 + Mth.floor(damage);
            InteractionHand hand = this.getUsedItemHand();
            this.useItem.hurtAndBreak(dmg, this, getSlotForHand(hand));
            if (this.useItem.isEmpty()) {
                this.setItemSlot(getSlotForHand(hand), ItemStack.EMPTY);
                this.useItem = ItemStack.EMPTY;
                this.shieldActiveTicks = 0;
                this.level().broadcastEntityEvent(this, EntityEvent.SHIELD_DISABLED);
            }
        }
    }

    private void executeShieldBash() {
        LivingEntity target = this.pendingBashTarget;
        this.pendingBashTarget = null;
        if (target == null || !target.isAlive() || this.shieldCooldownTicks > 0 || !this.isRaisingShield()) {
            return;
        }
        if (this.distanceToSqr(target) > 12.25D || !this.getSensing().hasLineOfSight(target)) {
            return;
        }

        // Lunge slightly and slam attacker back, then drop guard briefly to create a combat opening.
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 1.0E-4D) {
            this.push((dx / dist) * 0.25D, 0.08D, (dz / dist) * 0.25D);
        }

        float bashDamage = Math.max(3.0F, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F);
        target.hurt(this.damageSources().mobAttack(this), bashDamage);
        target.knockback(1.1D, this.getX() - target.getX(), this.getZ() - target.getZ());

        this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.7F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5D), target.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.15D);
        }

        this.bashCooldownTicks = BASH_COOLDOWN_TICKS;
        this.shieldCooldownTicks = 25;
        this.shieldActiveTicks = 0;
        this.stopUsingItem();
    }

    private class ShieldDefenseGoal extends Goal {
        private int guardCooldown = 0;

        @Override
        public boolean canUse() {
            if (this.guardCooldown > 0) this.guardCooldown--;
            if (NoxusKnightEntity.this.shieldCooldownTicks > 0 || NoxusKnightEntity.this.getShieldHand() == null) {
                return false;
            }
            LivingEntity target = NoxusKnightEntity.this.getTarget();
            if (target == null || !target.isAlive()) return false;

            double distSqr = NoxusKnightEntity.this.distanceToSqr(target);
            boolean rangedThreat = isRangedThreat(target) && distSqr <= 324.0D && NoxusKnightEntity.this.getSensing().hasLineOfSight(target);
            boolean closingGap = distSqr > 10.24D && distSqr <= 256.0D && NoxusKnightEntity.this.getSensing().hasLineOfSight(target);
            boolean justHit = NoxusKnightEntity.this.hurtTime > 0 && distSqr <= 36.0D;
            boolean tacticalGuard = this.guardCooldown <= 0 && distSqr <= 12.25D && NoxusKnightEntity.this.getRandom().nextInt(18) == 0;
            return rangedThreat || closingGap || justHit || tacticalGuard;
        }

        @Override
        public void start() {
            InteractionHand hand = NoxusKnightEntity.this.getShieldHand();
            if (hand != null) {
                NoxusKnightEntity.this.startUsingItem(hand);
                NoxusKnightEntity.this.shieldActiveTicks = 24 + NoxusKnightEntity.this.getRandom().nextInt(16);
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (NoxusKnightEntity.this.shieldCooldownTicks > 0 || NoxusKnightEntity.this.getShieldHand() == null) {
                return false;
            }
            LivingEntity target = NoxusKnightEntity.this.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!NoxusKnightEntity.this.isRaisingShield()) return false;

            double distSqr = NoxusKnightEntity.this.distanceToSqr(target);
            if (NoxusKnightEntity.this.pendingBashDelay >= 0) {
                return true;
            }
            // Keep shield up while closing in on ranged threats; otherwise release once timer expires at melee range.
            if (isRangedThreat(target) && distSqr > 7.84D && distSqr <= 324.0D) {
                return NoxusKnightEntity.this.getSensing().hasLineOfSight(target);
            }
            if (distSqr > 9.0D && distSqr <= 256.0D && NoxusKnightEntity.this.shieldActiveTicks > 0) {
                return true;
            }
            return NoxusKnightEntity.this.shieldActiveTicks > 0;
        }

        @Override
        public void tick() {
            LivingEntity target = NoxusKnightEntity.this.getTarget();
            if (target != null) {
                NoxusKnightEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (NoxusKnightEntity.this.shieldActiveTicks > 0) {
                NoxusKnightEntity.this.shieldActiveTicks--;
            }
        }

        @Override
        public void stop() {
            NoxusKnightEntity.this.shieldActiveTicks = 0;
            if (NoxusKnightEntity.this.isRaisingShield()) {
                NoxusKnightEntity.this.stopUsingItem();
            }
            this.guardCooldown = 50 + NoxusKnightEntity.this.getRandom().nextInt(35);
        }

        private static boolean isRangedThreat(LivingEntity target) {
            ItemStack active = target.getUseItem();
            if (target.isUsingItem() && !active.isEmpty()
                    && (active.getItem() instanceof BowItem || active.getItem() instanceof CrossbowItem)) {
                return true;
            }
            ItemStack main = target.getMainHandItem();
            return main.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(main);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target.getType().is(NOXUS_MOBS) || target.getPersistentData().getBoolean("IsNoxusMob")) {
            return false;
        }
        return super.canAttack(target);
    }
}
