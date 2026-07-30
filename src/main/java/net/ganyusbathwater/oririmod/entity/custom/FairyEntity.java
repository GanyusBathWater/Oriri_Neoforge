package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

public class FairyEntity extends Monster implements GeoEntity, net.ganyusbathwater.oririmod.combat.IElementalEntity {
    public static final EntityDataAccessor<Integer> DATA_HAIR_COLOR = SynchedEntityData.defineId(FairyEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_WING_COLOR = SynchedEntityData.defineId(FairyEntity.class, EntityDataSerializers.INT);

    // Pale nature colors for hair
    private static final int[] HAIR_PALETTE = {
            0xA8E6CF, 0xFFF2CC, 0xE1D5E7, 0xF8CECC, 0xD4C19C, 0xD5E8D4
    };
    // Bright vibrant colors for wings
    private static final int[] WING_PALETTE = {
            0x00FFFF, 0xFF00FF, 0xFFD700, 0x50C878, 0xBF00FF, 0xFF007F
    };

    private static final TagKey<EntityType<?>> DRYADS_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dryads"));

    @Nullable
    private LivingEntity owner;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public FairyEntity(EntityType<? extends FairyEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.lookControl = new net.minecraft.world.entity.ai.control.LookControl(this);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
        this.setPathfindingMalus(PathType.FENCE, -1.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D) // 12 Hearts
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAIR_COLOR, HAIR_PALETTE[0]);
        builder.define(DATA_WING_COLOR, WING_PALETTE[0]);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.entityData.set(DATA_HAIR_COLOR, HAIR_PALETTE[this.random.nextInt(HAIR_PALETTE.length)]);
        this.entityData.set(DATA_WING_COLOR, WING_PALETTE[this.random.nextInt(WING_PALETTE.length)]);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        this.populateDefaultEquipmentEnchantments(level, level.getRandom(), difficulty);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal(this, 1.0D));
        
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new ProtectDryadGoal(this));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false; // Flying mobs take no fall damage
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        // Do nothing
    }

    // --- GETTERS & SETTERS ---
    public int getHairColor() { return this.entityData.get(DATA_HAIR_COLOR); }
    public int getWingColor() { return this.entityData.get(DATA_WING_COLOR); }
    
    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    // --- SAVE / LOAD ---
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HairColor", this.entityData.get(DATA_HAIR_COLOR));
        tag.putInt("WingColor", this.entityData.get(DATA_WING_COLOR));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HairColor")) {
            this.entityData.set(DATA_HAIR_COLOR, tag.getInt("HairColor"));
        }
        if (tag.contains("WingColor")) {
            this.entityData.set(DATA_WING_COLOR, tag.getInt("WingColor"));
        }
    }

    // --- GECKOLIB ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            if (this.getTarget() != null) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("fairy_charging"));
            } else if (state.isMoving()) {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("fairy_idle"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenLoop("fairy_idle"));
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    @Override
    public net.ganyusbathwater.oririmod.combat.Element getElement() {
        return net.ganyusbathwater.oririmod.combat.Element.NATURE;
    }



    // --- CUSTOM PROTECT DRYAD GOAL ---
    static class ProtectDryadGoal extends TargetGoal {
        private final FairyEntity fairy;
        private LivingEntity attacker;

        public ProtectDryadGoal(FairyEntity fairy) {
            super(fairy, false, true);
            this.fairy = fairy;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            AABB aabb = this.fairy.getBoundingBox().inflate(16.0D);
            List<LivingEntity> dryads = this.fairy.level().getEntitiesOfClass(LivingEntity.class, aabb, 
                entity -> entity.getType().is(DRYADS_TAG) && entity.getLastHurtByMob() != null);
            
            if (!dryads.isEmpty()) {
                for (LivingEntity dryad : dryads) {
                    LivingEntity threat = dryad.getLastHurtByMob();
                    if (threat != null && threat != this.fairy && this.canAttack(threat, TargetingConditions.DEFAULT)) {
                        this.attacker = threat;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void start() {
            this.fairy.setTarget(this.attacker);
            super.start();
        }
    }
}
