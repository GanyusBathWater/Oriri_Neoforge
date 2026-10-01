package net.ganyusbathwater.oririmod.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CraterWorkerEntity extends Entity {
    private static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.defineId(CraterWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_TICKS = SynchedEntityData.defineId(CraterWorkerEntity.class, EntityDataSerializers.INT);

    public CraterWorkerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10); // Default 10
        builder.define(MAX_TICKS, 40); // Default 2 seconds
    }

    public void setCraterData(int radius, int maxTicks) {
        this.entityData.set(RADIUS, radius);
        this.entityData.set(MAX_TICKS, maxTicks);
    }

    public int getRadius() {
        return this.entityData.get(RADIUS);
    }

    public int getMaxTicks() {
        return this.entityData.get(MAX_TICKS);
    }

    @Override
    public void tick() {
        super.tick();

        int carveTicks = getMaxTicks();
        int lifeTime = carveTicks + 100; // Linger for 5 seconds after generation finishes

        if (this.tickCount > lifeTime) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }

        if (!this.level().isClientSide()) {
            if (this.tickCount <= carveTicks) {
                carveCraterLayer();
            }
        }
    }

    private void carveCraterLayer() {
        int r = getRadius();
        int max = getMaxTicks();
        
        // We carve radially inside-out. 
        float prevRadiusFloat = (this.tickCount - 1.0f) / max * r;
        float currRadiusFloat = (float) this.tickCount / max * r;

        // Bounding box for this tick's shell
        int boxMin = -(int) Math.ceil(currRadiusFloat);
        int boxMax = (int) Math.ceil(currRadiusFloat);

        BlockPos center = this.blockPosition();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Level level = this.level();

        for (int x = boxMin; x <= boxMax; x++) {
            for (int z = boxMin; z <= boxMax; z++) {
                double dist = Math.sqrt(x * x + z * z);
                
                // Only process the shell for this specific tick.
                // FIX: If dist == 0 and we are on tick 1, include it so we don't leave a center pillar!
                boolean isCenterOnFirstTick = (this.tickCount == 1 && dist == 0.0);
                if ((dist > prevRadiusFloat && dist <= currRadiusFloat) || isCenterOnFirstTick) {
                    
                    // Depth of the hemisphere at this X,Z
                    int depth = (int) Math.sqrt(Math.max(0, r * r - (x * x + z * z)));
                    
                    // We carve from R blocks ABOVE the impact (to destroy trees and carve cliffs) 
                    // down to the hemisphere depth.
                    int topY = center.getY() + r;
                    int bottomY = center.getY() - depth;

                    for (int y = topY; y >= bottomY; y--) {
                        // Calculate true 3D distance for the sphere, but treat everything above ground as a cylinder
                        double dist3D = (y > center.getY()) ? dist : Math.sqrt(x * x + Math.pow(center.getY() - y, 2) + z * z);
                        
                        // Apply 3D organic interference noise to the edges
                        if (dist3D > r * 0.6) {
                            // Middle-ground frequencies: Rocks are smaller than the first version, but not as chaotic as the second
                            double nx = (center.getX() + x) * 0.5;
                            double ny = y * 0.55;
                            double nz = (center.getZ() + z) * 0.5;
                            
                            // Base organic shapes
                            double noise = (Math.sin(nx) * Math.cos(ny) + Math.sin(nz) * Math.cos(nx) + Math.sin(ny) * Math.cos(nz)) * 2.2;
                            // Minor high-frequency details
                            noise += (Math.sin(nx * 1.5) + Math.sin(ny * 1.7) + Math.sin(nz * 1.5)) * 0.8;
                            
                            if (dist3D + noise > r) {
                                continue; // Leave this block intact to form the rough jagged wall
                            }
                        }

                        mutablePos.set(center.getX() + x, y, center.getZ() + z);
                        BlockState state = level.getBlockState(mutablePos);
                        
                        if (!state.isAir() && state.getDestroySpeed(level, mutablePos) >= 0 && state.getBlock().getExplosionResistance() < 1200) {
                            level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("CraterRadius")) {
            this.entityData.set(RADIUS, compound.getInt("CraterRadius"));
        }
        if (compound.contains("CraterMaxTicks")) {
            this.entityData.set(MAX_TICKS, compound.getInt("CraterMaxTicks"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("CraterRadius", this.getRadius());
        compound.putInt("CraterMaxTicks", this.getMaxTicks());
    }
}
