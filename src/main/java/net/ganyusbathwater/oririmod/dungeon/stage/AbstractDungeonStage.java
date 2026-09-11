package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for all stage implementations.
 * Handles the common door-opening and area-modifier logic so each stage
 * impl only needs to worry about its own win condition.
 */
public abstract class AbstractDungeonStage implements DungeonStage {

    protected final StageDefinition definition;
    protected StageState state = StageState.PENDING;

    protected AbstractDungeonStage(StageDefinition definition) {
        this.definition = definition;
    }

    @Override
    public StageDefinition getDefinition() {
        return definition;
    }

    @Override
    public StageState getState() {
        return state;
    }

    @Override
    public void onStart(ServerLevel level, DungeonInstance instance) {
        if (definition.getTriggers().isEmpty()) {
            this.state = StageState.ACTIVE;
            applyStartEffects(level, instance);
            doStart(level, instance);
        } else {
            this.state = StageState.PENDING;
        }
    }

    /** Subclasses must implement this to do their specific start logic (spawning mobs, etc) */
    protected abstract void doStart(ServerLevel level, DungeonInstance instance);

    @Override
    public void tick(ServerLevel level, DungeonInstance instance) {
        if (state == StageState.PENDING) {
            // Check every 10 ticks for performance
            if (level.getGameTime() % 10 != 0) return;

            for (StageDefinition.TriggerEntry trigger : definition.getTriggers()) {
                BlockPos pos = trigger.pos();
                double r = trigger.radius();
                net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                    pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                    pos.getX() + r, pos.getY() + r, pos.getZ() + r
                );
                
                boolean playerInside = level.players().stream().anyMatch(p -> {
                    return instance.hasPlayer(p.getUUID()) && box.contains(p.position());
                });

                if (playerInside) {
                    this.state = StageState.ACTIVE;
                    applyStartEffects(level, instance);
                    doStart(level, instance);
                    break;
                }
            }
        } else if (state == StageState.ACTIVE) {
            doTick(level, instance);
        }
    }

    /** Subclasses must implement this to do their specific tick logic */
    protected abstract void doTick(ServerLevel level, DungeonInstance instance);

    /**
     * Executes all door lock effects by placing MAGIC_BARRIER_BLOCK
     */
    protected void applyStartEffects(ServerLevel level, DungeonInstance instance) {
        net.minecraft.world.level.block.Block magicBarrier = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("oririmod:magic_barrier_block"));
        if (magicBarrier == net.minecraft.world.level.block.Blocks.AIR) return; // Fallback if block not found
        
        for (StageDefinition.DoorEntry door : definition.getDoors()) {
            BlockPos pos = door.pos();
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) {
                level.setBlock(pos, magicBarrier.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Executes all door and area-modifier entries in the definition.
     * Called by subclasses once they determine their win condition is met.
     */
    protected void applyCompletionEffects(ServerLevel level, DungeonInstance instance) {
        // Open doors / remove barriers
        for (StageDefinition.DoorEntry door : definition.getDoors()) {
            BlockPos pos = door.pos();
            BlockState state = level.getBlockState(pos);
            // If it's any non-air block acting as a barrier, set it to air.
            // In Phase 5+ we will check for MagicBarrier specifically.
            if (!state.isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // Apply area modifiers (e.g. destroy floor)
        for (StageDefinition.AreaModifierEntry modifier : definition.getAreaModifiers()) {
            applyAreaModifier(level, modifier);
        }

        // Play a triumphant sound at the instance origin
        level.playSound(null, instance.getOrigin(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.AMBIENT, 1.0f, 1.0f);
    }

    private void applyAreaModifier(ServerLevel level, StageDefinition.AreaModifierEntry modifier) {
        BlockPos center = modifier.pos();
        int radius = modifier.radius();
        ResourceLocation filter = modifier.blockFilter();

        switch (modifier.action().toLowerCase()) {
            case "destroy" -> {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos target = center.offset(x, y, z);
                            BlockState state = level.getBlockState(target);
                            if (filter == null || matchesFilter(state, filter)) {
                                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
            case "fill" -> {
                if (filter != null) {
                    net.minecraft.world.level.block.Block fillBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(filter);
                    if (fillBlock != Blocks.AIR) {
                        for (int x = -radius; x <= radius; x++) {
                            for (int y = -radius; y <= radius; y++) {
                                for (int z = -radius; z <= radius; z++) {
                                    BlockPos target = center.offset(x, y, z);
                                    level.setBlock(target, fillBlock.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean matchesFilter(BlockState state, ResourceLocation filter) {
        return state.getBlock().builtInRegistryHolder().key().location().equals(filter);
    }
}
