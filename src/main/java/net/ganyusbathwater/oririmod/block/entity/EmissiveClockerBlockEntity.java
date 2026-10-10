package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.block.custom.EmissiveClockerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.PriorityQueue;

public class EmissiveClockerBlockEntity extends BlockEntity {
    public enum Mode {
        REPEATER, PULSE, CLOCK
    }

    private static class StateChange implements Comparable<StateChange> {
        long triggerTime;
        boolean lit;

        StateChange(long triggerTime, boolean lit) {
            this.triggerTime = triggerTime;
            this.lit = lit;
        }

        @Override
        public int compareTo(StateChange o) {
            return Long.compare(this.triggerTime, o.triggerTime);
        }
    }

    private Mode mode = Mode.REPEATER;
    private int delay = 20;
    private int timeOn = 20;
    private int timeOff = 20;
    private boolean useSeconds = false;

    private boolean lastInputState = false;
    private final PriorityQueue<StateChange> queue = new PriorityQueue<>();

    public EmissiveClockerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.EMISSIVE_CLOCKER_BE.get(), pos, blockState);
    }

    public Mode getMode() { return mode; }
    public int getDelay() { return delay; }
    public int getTimeOn() { return timeOn; }
    public int getTimeOff() { return timeOff; }
    public boolean isUseSeconds() { return useSeconds; }

    public void setConfig(Mode mode, int delay, int timeOn, int timeOff, boolean useSeconds, boolean emitLight) {
        this.mode = mode;
        this.delay = Math.max(1, delay);
        this.timeOn = Math.max(1, timeOn);
        this.timeOff = Math.max(1, timeOff);
        this.useSeconds = useSeconds;
        resetState();
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState currentState = getBlockState();
            if (currentState.getValue(EmissiveClockerBlock.EMIT_LIGHT) != emitLight) {
                level.setBlock(getBlockPos(), currentState.setValue(EmissiveClockerBlock.EMIT_LIGHT, emitLight), 3);
            } else {
                level.sendBlockUpdated(getBlockPos(), currentState, currentState, 3);
            }
        }
    }

    private void resetState() {
        queue.clear();
        if (level != null && !level.isClientSide) {
            level.setBlock(getBlockPos(), getBlockState().setValue(EmissiveClockerBlock.LIT, false), 3);
            lastInputState = false;
        }
    }

    public void onNeighborUpdate(boolean isPowered) {
        if (isPowered == lastInputState) return;
        lastInputState = isPowered;

        if (level == null || level.isClientSide) return;
        long currentTime = level.getGameTime();

        if (mode == Mode.REPEATER) {
            int delayTicks = useSeconds ? delay * 20 : delay;
            queue.add(new StateChange(currentTime + delayTicks, isPowered));
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), delayTicks);
        } else if (mode == Mode.PULSE) {
            if (isPowered) {
                if (queue.isEmpty() && !getBlockState().getValue(EmissiveClockerBlock.LIT)) {
                    int delayTicks = useSeconds ? delay * 20 : delay;
                    int onTicks = useSeconds ? timeOn * 20 : timeOn;
                    queue.add(new StateChange(currentTime + delayTicks, true));
                    queue.add(new StateChange(currentTime + delayTicks + onTicks, false));
                    level.scheduleTick(getBlockPos(), getBlockState().getBlock(), delayTicks);
                }
            }
        } else if (mode == Mode.CLOCK) {
            if (isPowered) {
                if (queue.isEmpty() && !getBlockState().getValue(EmissiveClockerBlock.LIT)) {
                    int delayTicks = useSeconds ? delay * 20 : delay;
                    queue.add(new StateChange(currentTime + delayTicks, true));
                    level.scheduleTick(getBlockPos(), getBlockState().getBlock(), delayTicks);
                }
            } else {
                resetState();
            }
        }
        setChanged();
    }

    public void onScheduledTick() {
        if (level == null || level.isClientSide) return;

        long currentTime = level.getGameTime();
        boolean stateChanged = false;
        Boolean finalState = null;

        while (!queue.isEmpty() && queue.peek().triggerTime <= currentTime) {
            StateChange sc = queue.poll();
            finalState = sc.lit;
            stateChanged = true;
        }

        if (stateChanged && finalState != null) {
            setLitState(finalState);
            
            if (mode == Mode.CLOCK) {
                if (finalState) {
                    int onTicks = useSeconds ? timeOn * 20 : timeOn;
                    queue.add(new StateChange(currentTime + onTicks, false));
                    level.scheduleTick(getBlockPos(), getBlockState().getBlock(), onTicks);
                } else {
                    if (lastInputState) {
                        int offTicks = useSeconds ? timeOff * 20 : timeOff;
                        queue.add(new StateChange(currentTime + offTicks, true));
                        level.scheduleTick(getBlockPos(), getBlockState().getBlock(), offTicks);
                    }
                }
            }
            setChanged();
        }

        if (!queue.isEmpty()) {
            long nextDelay = queue.peek().triggerTime - currentTime;
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), (int) Math.max(1, nextDelay));
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
        tag.putBoolean("LastInput", lastInputState);
        
        ListTag queueList = new ListTag();
        for (StateChange sc : queue) {
            CompoundTag c = new CompoundTag();
            c.putLong("Time", sc.triggerTime);
            c.putBoolean("Lit", sc.lit);
            queueList.add(c);
        }
        tag.put("Queue", queueList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Mode")) mode = Mode.values()[tag.getInt("Mode")];
        if (tag.contains("Delay")) delay = tag.getInt("Delay");
        if (tag.contains("TimeOn")) timeOn = tag.getInt("TimeOn");
        if (tag.contains("TimeOff")) timeOff = tag.getInt("TimeOff");
        if (tag.contains("UseSeconds")) useSeconds = tag.getBoolean("UseSeconds");
        if (tag.contains("LastInput")) lastInputState = tag.getBoolean("LastInput");
        
        queue.clear();
        if (tag.contains("Queue")) {
            ListTag queueList = (ListTag) tag.get("Queue");
            if (queueList != null) {
                for (int i = 0; i < queueList.size(); i++) {
                    CompoundTag c = queueList.getCompound(i);
                    queue.add(new StateChange(c.getLong("Time"), c.getBoolean("Lit")));
                }
            }
        }
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
