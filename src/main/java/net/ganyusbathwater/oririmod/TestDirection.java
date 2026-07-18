package net.ganyusbathwater.oririmod;

import net.minecraft.core.Direction;

public class TestDirection {
    public static void main(String[] args) {
        System.out.println("NORTH clockwise: " + Direction.NORTH.getClockWise());
        System.out.println("SOUTH clockwise: " + Direction.SOUTH.getClockWise());
        System.out.println("EAST clockwise: " + Direction.EAST.getClockWise());
        System.out.println("WEST clockwise: " + Direction.WEST.getClockWise());
    }
}
