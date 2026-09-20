package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.registries.BuiltInRegistries;

public class PuzzleSolveStage extends AbstractDungeonStage {
    
    public PuzzleSolveStage(StageDefinition definition) {
        super(definition);
    }
    
    @Override
    protected void doStart(ServerLevel level, DungeonInstance instance) {
        // Nothing special to initialize
    }
    
    @Override
    protected void doTick(ServerLevel level, DungeonInstance instance) {
        // Only check every 10 ticks for performance
        if (level.getGameTime() % 10 != 0) return;
        
        boolean allMatch = true;
        
        for (StageDefinition.BlockMatchEntry entry : definition.getBlockMatches()) {
            BlockPos pos = entry.pos();
            BlockState actualState = level.getBlockState(pos);
            
            // Check block ID
            if (!BuiltInRegistries.BLOCK.getKey(actualState.getBlock()).equals(entry.blockId())) {
                allMatch = false;
                break;
            }
            
            // Check block state data if provided (e.g. "lit=true,facing=north")
            String requiredStateData = entry.stateData();
            if (requiredStateData != null && !requiredStateData.isBlank() && !requiredStateData.equals("0")) { // "0" is default empty from old countBox
                String[] statePairs = requiredStateData.split(",");
                for (String pair : statePairs) {
                    String[] parts = pair.split("=");
                    if (parts.length == 2) {
                        String propName = parts[0].trim();
                        String propValue = parts[1].trim();
                        
                        boolean matchFound = false;
                        for (Property<?> property : actualState.getProperties()) {
                            if (property.getName().equals(propName)) {
                                String actualValue = getPropertyValueString(actualState, property);
                                if (actualValue.equalsIgnoreCase(propValue)) {
                                    matchFound = true;
                                }
                                break;
                            }
                        }
                        
                        if (!matchFound) {
                            allMatch = false;
                            break;
                        }
                    }
                }
            }
            
            if (!allMatch) break;
        }
        
        if (allMatch && !definition.getBlockMatches().isEmpty()) {
            this.state = StageState.COMPLETE;
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> String getPropertyValueString(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
    
    @Override
    public void onComplete(ServerLevel level, DungeonInstance instance) {
        applyCompletionEffects(level, instance);
    }
}
