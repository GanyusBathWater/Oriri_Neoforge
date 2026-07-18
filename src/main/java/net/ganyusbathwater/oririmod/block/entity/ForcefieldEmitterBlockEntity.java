package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.block.custom.ForcefieldEmitterBlock;
import net.ganyusbathwater.oririmod.block.custom.ForcefieldVariant;
import net.ganyusbathwater.oririmod.entity.ai.FleeBlockGoal;
import net.ganyusbathwater.oririmod.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.effect.MobEffect;

import java.util.*;

public class ForcefieldEmitterBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private boolean isActive = true;
    
    // Static registries for quick event lookups per dimension
    private static final Map<Level, Set<BlockPos>> ATTRACTING_EMITTERS = new WeakHashMap<>();
    private static final Map<Level, Set<BlockPos>> PROTECTION_EMITTERS = new WeakHashMap<>();
    
    public ForcefieldEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORCEFIELD_EMITTER.get(), pos, state);
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        if (level != null && !level.isClientSide) {
            removeFromStaticLists();
            registerToStaticLists();
        }
        setChanged();
    }
    
    public ForcefieldVariant getVariant() {
        if (getBlockState().getBlock() instanceof ForcefieldEmitterBlock emitter) {
            return emitter.getVariant();
        }
        return ForcefieldVariant.REPELLENT;
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            registerToStaticLists();
        }
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            removeFromStaticLists();
        }
    }
    
    private void registerToStaticLists() {
        if (!isActive() && getVariant() != ForcefieldVariant.REPELLENT) return;
        
        if (getVariant() == ForcefieldVariant.ATTRACTING) {
            ATTRACTING_EMITTERS.computeIfAbsent(level, k -> new HashSet<>()).add(getBlockPos());
        } else if (getVariant() == ForcefieldVariant.PROTECTION) {
            PROTECTION_EMITTERS.computeIfAbsent(level, k -> new HashSet<>()).add(getBlockPos());
        }
    }
    
    private void removeFromStaticLists() {
        if (getVariant() == ForcefieldVariant.ATTRACTING) {
            Set<BlockPos> set = ATTRACTING_EMITTERS.get(level);
            if (set != null) set.remove(getBlockPos());
        } else if (getVariant() == ForcefieldVariant.PROTECTION) {
            Set<BlockPos> set = PROTECTION_EMITTERS.get(level);
            if (set != null) set.remove(getBlockPos());
        }
    }
    
    public static boolean isAttractingNearby(Level level, BlockPos targetPos) {
        Set<BlockPos> set = ATTRACTING_EMITTERS.get(level);
        if (set == null || set.isEmpty()) return false;
        for (BlockPos pos : set) {
            if (pos.distSqr(targetPos) <= 32 * 32) return true;
        }
        return false;
    }
    
    public static boolean isProtectionNearby(Level level, BlockPos targetPos) {
        return isProtectionNearby(level, targetPos, null);
    }
    
    public static boolean isProtectionNearby(Level level, BlockPos targetPos, BlockPos excludePos) {
        Set<BlockPos> set = PROTECTION_EMITTERS.get(level);
        if (set == null || set.isEmpty()) return false;
        for (BlockPos pos : set) {
            if (pos.equals(excludePos)) continue;
            if (pos.distSqr(targetPos) <= 32 * 32) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsActive", isActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isActive = tag.getBoolean("IsActive");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ForcefieldEmitterBlockEntity pEntity) {
        if (level.isClientSide) return;
        
        ForcefieldVariant variant = pEntity.getVariant();
        // Only Repellent works when inactive
        if (!pEntity.isActive() && variant != ForcefieldVariant.REPELLENT) return;
        
        // Every 20 ticks
        if (level.getGameTime() % 20 == 0) {
            if (variant == ForcefieldVariant.REPELLENT) {
                pEntity.tickRepellent();
            } else if (variant == ForcefieldVariant.MODIFIER) {
                pEntity.tickModifier();
            }
        }
    }
    
    private void tickRepellent() {
        AABB bounds = new AABB(worldPosition).inflate(24);
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class, bounds);
        for (Monster monster : monsters) {
            boolean hasFlee = false;
            for (net.minecraft.world.entity.ai.goal.WrappedGoal goal : monster.goalSelector.getAvailableGoals()) {
                if (goal.getGoal() instanceof FleeBlockGoal flee && flee.getTargetPos().equals(worldPosition)) {
                    hasFlee = true;
                    break;
                }
            }
            if (!hasFlee) {
                monster.goalSelector.addGoal(1, new FleeBlockGoal(monster, worldPosition, 24.0f, 1.0, 1.2));
            }
        }
    }
    
    private void tickModifier() {
        AABB bounds = new AABB(worldPosition).inflate(32);
        List<Player> players = level.getEntitiesOfClass(Player.class, bounds);
        if (players.isEmpty()) return;
        
        net.minecraft.world.Difficulty difficulty = level.getDifficulty();
        if (difficulty == net.minecraft.world.Difficulty.PEACEFUL) return;
        
        int numEffects = 0;
        int amplifier = 0;
        
        switch (difficulty) {
            case EASY -> { numEffects = 1 + level.random.nextInt(2); amplifier = 0; } // 1-2 effects, 10%
            case NORMAL -> { numEffects = 2 + level.random.nextInt(3); amplifier = 1; } // 2-4 effects, 15%
            case HARD -> { numEffects = 3 + level.random.nextInt(5); amplifier = 2; } // 3-7 effects, 20%
        }
        
        if (level.getLevelData().isHardcore()) {
            numEffects = 7;
            amplifier = 3; // 25%
        }
        
        List<Holder<MobEffect>> allEffects = List.of(
            ModEffects.FORCEFIELD_PENALTY_HEALTH,
            ModEffects.FORCEFIELD_PENALTY_ARMOR,
            ModEffects.FORCEFIELD_PENALTY_ARMOR_TOUGHNESS,
            ModEffects.FORCEFIELD_PENALTY_ATTACK_DAMAGE,
            ModEffects.FORCEFIELD_PENALTY_ATTACK_SPEED,
            ModEffects.FORCEFIELD_PENALTY_BURNING_TIME,
            ModEffects.FORCEFIELD_PENALTY_FALL_DAMAGE
        );
        
        for (Player player : players) {
            long seed = worldPosition.asLong() ^ player.getUUID().getMostSignificantBits();
            Random random = new Random(seed);
            
            List<Holder<MobEffect>> shuffled = new ArrayList<>(allEffects);
            Collections.shuffle(shuffled, random);
            
            int toApply = Math.min(numEffects, shuffled.size());
            for (int i = 0; i < toApply; i++) {
                // 200 ticks (10 seconds), false for ambient, false for visible, true for showIcon
                player.addEffect(new MobEffectInstance(shuffled.get(i), 200, amplifier, false, false, true));
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("forcefield_emitter_idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
