package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.block.custom.EmissiveClockerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EmissiveClockerBlockEntity extends BlockEntity {
    public enum Mode {
        REPEATER, PULSE, CLOCK
    }

    private enum Phase {
        IDLE, WAITING_ON, ON, WAITING_OFF, OFF
    }

    private Mode mode = Mode.REPEATER;
    private int delay = 20;
    private int timeOn = 20;
    private int timeOff = 20;
    private boolean useSeconds = false;

    private Phase currentPhase = Phase.IDLE;
    private boolean lastInputState = false;

    public EmissiveClockerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.EMISSIVE_CLOCKER_BE.get(), pos, blockState);
    }

    public Mode getMode() { return mode; }
    public int getDelay() { return delay; }
    public int getTimeOn() { return timeOn; }
    public int getTimeOff() { return timeOff; }
    public boolean isUseSeconds() { return useSeconds; }

    public void setConfig(Mode mode, int delay, int timeOn, int timeOff, boolean useSeconds) {
        this.mode = mode;
        this.delay = Math.max(1, delay);
        this.timeOn = Math.max(1, timeOn);
        this.timeOff = Math.max(1, timeOff);
        this.useSeconds = useSeconds;
        resetState();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void resetState() {
        currentPhase = Phase.IDLE;
        if (level != null && !level.isClientSide) {
            level.setBlock(getBlockPos(), getBlockState().setValue(EmissiveClockerBlock.LIT, false), 3);
            // We should ideally read the actual input state here
            lastInputState = false;
        }
    }

    public void onNeighborUpdate(boolean isPowered) {
        if (isPowered == lastInputState) return;
        lastInputState = isPowered;

        if (mode == Mode.REPEATER) {
            if (isPowered && currentPhase != Phase.WAITING_ON && currentPhase != Phase.ON) {
                currentPhase = Phase.WAITING_ON;
                scheduleNextTick(delay);
            } else if (!isPowered && currentPhase != Phase.WAITING_OFF && currentPhase != Phase.OFF && currentPhase != Phase.IDLE) {
                currentPhase = Phase.WAITING_OFF;
                scheduleNextTick(delay);
            }
        } else if (mode == Mode.PULSE) {
            if (isPowered && currentPhase == Phase.IDLE) {
                currentPhase = Phase.WAITING_ON;
                scheduleNextTick(delay);
            }
        } else if (mode == Mode.CLOCK) {
            if (isPowered && currentPhase == Phase.IDLE) {
                currentPhase = Phase.WAITING_ON;
                scheduleNextTick(delay);
            } else if (!isPowered) {
                // When input stops, clock stops immediately or finishes phase?
                // Plan: finish current phase, then stop. Or stop immediately.
                // For simplicity, reset to IDLE and turn off.
                resetState();
            }
        }
    }

    public void onScheduledTick() {
        if (level == null || level.isClientSide) return;

        switch (currentPhase) {
            case WAITING_ON -> {
                setLitState(true);
                if (mode == Mode.PULSE) {
                    currentPhase = Phase.ON;
                    scheduleNextTick(timeOn);
                } else if (mode == Mode.CLOCK) {
                    currentPhase = Phase.ON;
                    scheduleNextTick(timeOn);
                } else {
                    currentPhase = Phase.ON; // Repeater stays ON until input goes off
                }
            }
            case ON -> {
                setLitState(false);
                if (mode == Mode.PULSE) {
                    currentPhase = Phase.IDLE; // Done pulsing
                } else if (mode == Mode.CLOCK) {
                    currentPhase = Phase.OFF;
                    scheduleNextTick(timeOff);
                }
            }
            case WAITING_OFF -> {
                setLitState(false);
                currentPhase = Phase.IDLE;
            }
            case OFF -> {
                if (mode == Mode.CLOCK && lastInputState) {
                    setLitState(true);
                    currentPhase = Phase.ON;
                    scheduleNextTick(timeOn);
                } else {
                    currentPhase = Phase.IDLE;
                }
            }
            case IDLE -> {}
        }
    }

    private void scheduleNextTick(int delayTicks) {
        if (level != null) {
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), delayTicks);
        }
    }

    private void setLitState(boolean lit) {
        if (level != null) {
            BlockState currentState = getBlockState();
            if (currentState.getValue(EmissiveClockerBlock.LIT) != lit) {
                level.setBlock(getBlockPos(), currentState.setValue(EmissiveClockerBlock.LIT, lit), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", mode.ordinal());
        tag.putInt("Delay", delay);
        tag.putInt("TimeOn", timeOn);
        tag.putInt("TimeOff", timeOff);
        tag.putBoolean("UseSeconds", useSeconds);
        tag.putInt("Phase", currentPhase.ordinal());
        tag.putBoolean("LastInput", lastInputState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Mode")) mode = Mode.values()[tag.getInt("Mode")];
        if (tag.contains("Delay")) delay = tag.getInt("Delay");
        if (tag.contains("TimeOn")) timeOn = tag.getInt("TimeOn");
        if (tag.contains("TimeOff")) timeOff = tag.getInt("TimeOff");
        if (tag.contains("UseSeconds")) useSeconds = tag.getBoolean("UseSeconds");
        if (tag.contains("Phase")) currentPhase = Phase.values()[tag.getInt("Phase")];
        if (tag.contains("LastInput")) lastInputState = tag.getBoolean("LastInput");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
