package net.ganyusbathwater.oririmod.worldgen.tree;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.ModConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class ModTreeGrowers {
    public static final TreeGrower ELDER_TREE = new TreeGrower(OririMod.MOD_ID + ":elder_tree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.ELDER_TREE_KEY), Optional.empty());

    public static final TreeGrower SCARLET_TREE = new TreeGrower(OririMod.MOD_ID + ":scarlet_tree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.SCARLET_TREE_KEY), Optional.empty());

}
