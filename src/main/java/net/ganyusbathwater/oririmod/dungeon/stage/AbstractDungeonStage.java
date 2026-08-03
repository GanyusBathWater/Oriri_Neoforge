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
    protected boolean complete = false;

    protected AbstractDungeonStage(StageDefinition definition) {
        this.definition = definition;
    }

    @Override
    public StageDefinition getDefinition() {
        return definition;
    }

    @Override
    public boolean isComplete() {
        return complete;
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
                // Future: fill area with a specific block
            }
        }
    }

    private boolean matchesFilter(BlockState state, ResourceLocation filter) {
        return state.getBlock().builtInRegistryHolder().key().location().equals(filter);
    }
}
