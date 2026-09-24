package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.world.data.TeleporterSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TeleporterBlockEntity extends BlockEntity implements GeoBlockEntity {

    private String teleporterId = "";
    private boolean isLocked = false;
    private int baseColor = 0x00AAFF; // Default to glowing blue

    public enum TeleporterState {
        UNLINKED, SPAWNING, IDLE, DESPAWNING
    }
    private TeleporterState currentState = TeleporterState.UNLINKED;
    private long stateChangeTime = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEPORTER.get(), pos, state);
    }

    public String getTeleporterId() {
        return teleporterId;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void setTeleporterId(String id, ServerPlayer player) {
        if (this.level == null || this.level.isClientSide) return;
        
        TeleporterSavedData data = TeleporterSavedData.get((ServerLevel) this.level);
        
        // Remove old ID from registry if exists
        if (!this.teleporterId.isEmpty()) {
            data.removeTeleporter(this.teleporterId, this.getBlockPos());
        }

        if (id != null && !id.isEmpty()) {
            boolean success = data.addTeleporter(id, this.getBlockPos());
            if (!success && player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.oririmod.teleporter.frequency_full").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }
            // Going from unlinked to linked -> SPAWNING
            if (this.teleporterId.isEmpty()) {
                this.currentState = TeleporterState.SPAWNING;
                this.stateChangeTime = this.level.getGameTime();
            }
        } else {
            // Going from linked to unlinked -> DESPAWNING
            if (!this.teleporterId.isEmpty()) {
                this.currentState = TeleporterState.DESPAWNING;
                this.stateChangeTime = this.level.getGameTime();
            }
        }
        
        this.teleporterId = id == null ? "" : id;
        this.setChanged();
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.oririmod.teleporter.id_set", this.teleporterId).withStyle(net.minecraft.ChatFormatting.GREEN), true);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("TeleporterId", this.teleporterId);
        tag.putBoolean("IsLocked", this.isLocked);
        tag.putInt("BaseColor", this.baseColor);
        tag.putString("State", this.currentState.name());
        tag.putLong("StateChangeTime", this.stateChangeTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.teleporterId = tag.getString("TeleporterId");
        this.isLocked = tag.getBoolean("IsLocked");
        if (tag.contains("BaseColor")) {
            this.baseColor = tag.getInt("BaseColor");
        }
        if (tag.contains("State")) {
            try {
                this.currentState = TeleporterState.valueOf(tag.getString("State"));
            } catch (IllegalArgumentException e) {
                this.currentState = TeleporterState.UNLINKED;
            }
        }
        this.stateChangeTime = tag.getLong("StateChangeTime");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        super.onDataPacket(net, pkt, provider);
        handleUpdateTag(pkt.getTag(), provider);
    }

    public int getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(int baseColor) {
        this.baseColor = baseColor;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public TeleporterState getCurrentState() {
        return currentState;
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, TeleporterBlockEntity entity) {
        if (!level.isClientSide) {
            
            long timeSinceChange = level.getGameTime() - entity.stateChangeTime;
            if (entity.currentState == TeleporterState.SPAWNING && timeSinceChange >= 5) {
                entity.currentState = TeleporterState.IDLE;
                entity.stateChangeTime = level.getGameTime();
                level.sendBlockUpdated(pos, state, state, 3);
            } else if (entity.currentState == TeleporterState.DESPAWNING && timeSinceChange >= 5) {
                entity.currentState = TeleporterState.UNLINKED;
                entity.stateChangeTime = level.getGameTime();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        } else {
            // Client side particles
            if (entity.currentState == TeleporterState.IDLE) {
                if (level.random.nextInt(3) == 0) {
                    double cx = pos.getX() + 0.5;
                    double cy = pos.getY() + 1.5; // Core center
                    double cz = pos.getZ() + 0.5;
                    
                    // Spawn particle in a radius of 2.5 around the center
                    double dx = (level.random.nextDouble() - 0.5) * 5.0;
                    double dy = (level.random.nextDouble() - 0.5) * 5.0;
                    double dz = (level.random.nextDouble() - 0.5) * 5.0;
                    
                    level.addParticle(net.ganyusbathwater.oririmod.particle.ModParticles.TELEPORTER_VORTEX_PARTICLE.get(),
                        cx + dx, cy + dy, cz + dz,
                        cx, cy, cz); // Target position
                }
            }
        }
    }

    // --- GeckoLib ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<TeleporterBlockEntity> event) {
        switch (this.currentState) {
            case SPAWNING:
                event.getController().setAnimation(RawAnimation.begin().thenPlay("teleporter_activating"));
                return PlayState.CONTINUE;
            case DESPAWNING:
                event.getController().setAnimation(RawAnimation.begin().thenPlay("animationteleporter_deactivating"));
                return PlayState.CONTINUE;
            case IDLE:
                event.getController().setAnimation(RawAnimation.begin().thenLoop("teleporter_idle"));
                return PlayState.CONTINUE;
            case UNLINKED:
            default:
                event.getController().forceAnimationReset();
                return PlayState.STOP;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
