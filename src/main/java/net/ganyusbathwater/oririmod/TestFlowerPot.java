package net.ganyusbathwater.oririmod;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;

public class TestFlowerPot {
    public static void test() {
        ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "oak_sapling"), () -> Blocks.POTTED_OAK_SAPLING);
    }
}
