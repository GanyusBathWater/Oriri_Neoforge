package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.world.level.block.LeavesBlock;

public class ElderLeavesBlock extends LeavesBlock {
    public ElderLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PERSISTENT, true)
                .setValue(DISTANCE, 7)
                .setValue(WATERLOGGED, false));
    }
}
