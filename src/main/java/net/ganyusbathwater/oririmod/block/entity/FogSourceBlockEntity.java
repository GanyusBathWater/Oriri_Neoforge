package net.ganyusbathwater.oririmod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lightweight non-ticking marker BlockEntity for {@link net.ganyusbathwater.oririmod.block.custom.FogSourceBlock}.
 * Enables O(1) per-chunk discovery of placed fog sources inside structures and dungeons
 * without expensive 3D block scanning on the client. All configuration lives in BlockState
 * so structure NBT templates serialize cleanly.
 */
public class FogSourceBlockEntity extends BlockEntity {

    public FogSourceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FOG_SOURCE_BE.get(), pos, blockState);
    }
}
