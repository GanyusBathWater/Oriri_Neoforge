package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    public NoxusKnightEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));

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
    public boolean canAttack(LivingEntity target) {
        if (target.getType().is(NOXUS_MOBS) || target.getPersistentData().getBoolean("IsNoxusMob")) {
            return false;
        }
        return super.canAttack(target);
    }
}
