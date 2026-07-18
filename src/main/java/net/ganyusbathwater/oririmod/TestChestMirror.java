package net.ganyusbathwater.oririmod;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class TestChestMirror {
    public static void main(String[] args) {
        BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockState mirrored = left.mirror(Mirror.LEFT_RIGHT);
        System.out.println("Original: " + left.getValue(ChestBlock.TYPE));
        System.out.println("Mirrored: " + mirrored.getValue(ChestBlock.TYPE));
    }
}
