package net.ganyusbathwater.oririmod;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class TestChestLogic {
    public static void main(String[] args) {
        BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
        System.out.println("Facing SOUTH, Type LEFT");
        System.out.println("getConnectedDirection: " + ChestBlock.getConnectedDirection(left));
        
        BlockState right = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
        System.out.println("Facing SOUTH, Type RIGHT");
        System.out.println("getConnectedDirection: " + ChestBlock.getConnectedDirection(right));
    }
}
