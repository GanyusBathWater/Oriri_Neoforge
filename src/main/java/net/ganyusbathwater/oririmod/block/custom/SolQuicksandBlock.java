package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.util.Mth;
import java.util.Optional;

public class SolQuicksandBlock extends Block implements BucketPickup {
    private static final float FALL_DAMAGE_MULTIPLIER = 0.0F;
    private static final VoxelShape FALLING_COLLISION_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.9D, 1.0D);

    public SolQuicksandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction direction) {
        return adjacentBlockState.is(this) ? true : super.skipRendering(state, adjacentBlockState, direction);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity != null) {
                if (entity.fallDistance > 2.5F) {
                    return FALLING_COLLISION_SHAPE;
                }
            }
        }
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity) || level.getBlockState(entity.blockPosition()).is(this)) {
            entity.makeStuckInBlock(state, new Vec3(0.5D, 0.1D, 0.5D));
            if (level.isClientSide) {
                // Potential client side effects
            }
        }
        
        if (entity instanceof LivingEntity livingEntity) {
            // Check if head is inside the block for suffocation
            if (this.isEntityHeadInside(livingEntity, state, pos)) {
                if (!livingEntity.canBreatheUnderwater() && !livingEntity.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING)) {
                    if (livingEntity.getAirSupply() > -20) {
                        livingEntity.setAirSupply(livingEntity.getAirSupply() - 1);
                        if (livingEntity.getAirSupply() == -20) {
                            livingEntity.setAirSupply(0);
                            livingEntity.hurt(level.damageSources().inWall(), 2.0F);
                        }
                    }
                }
            }
        }
    }
    
    private boolean isEntityHeadInside(LivingEntity entity, BlockState state, BlockPos pos) {
        double eyeY = entity.getEyeY();
        return eyeY < (double)pos.getY() + 1.0D && eyeY > (double)pos.getY();
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!((double)fallDistance < 4.0D) && entity instanceof LivingEntity livingEntity) {
            LivingEntity.Fallsounds fallsounds = livingEntity.getFallSounds();
            SoundEvent soundevent = (double)fallDistance < 7.0D ? fallsounds.small() : fallsounds.big();
            entity.playSound(soundevent, 1.0F, 1.0F);
        }
    }

    @Override
    public ItemStack pickupBlock(@javax.annotation.Nullable net.minecraft.world.entity.player.Player player, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
        if (!level.isClientSide()) {
            level.levelEvent(2001, pos, Block.getId(state));
        }
        return new ItemStack(net.ganyusbathwater.oririmod.item.ModItems.SOL_QUICKSAND_BUCKET.get());
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_POWDER_SNOW);
    }
    
    public Optional<SoundEvent> getPickupSound(BlockState state) {
        return Optional.of(SoundEvents.BUCKET_FILL_POWDER_SNOW);
    }

    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
    
    public boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
