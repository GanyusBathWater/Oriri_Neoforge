package net.ganyusbathwater.oririmod.block.custom;

import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Scarlet Grass Block - A grass block variant for Scarlet biomes.
 * Allows plants to be placed on it, unlike moss blocks.
 */
public class ScarletGrassBlock extends GrassBlock {
    public ScarletGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
