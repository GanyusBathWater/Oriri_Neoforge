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

    public static final TreeGrower ABYSS_CROWN_TREE = new TreeGrower(OririMod.MOD_ID + ":abyss_crown_tree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.ABYSS_CROWN_TREE_KEY), Optional.empty());

    public static final TreeGrower EPOCH_TREE = new TreeGrower(OririMod.MOD_ID + ":epoch_tree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.EPOCH_TREE_KEY), Optional.empty());
            
    public static final TreeGrower HUGE_RED_MUSHROOM = new TreeGrower(OririMod.MOD_ID + ":huge_red_mushroom",
            Optional.empty(), Optional.of(net.minecraft.data.worldgen.features.TreeFeatures.HUGE_RED_MUSHROOM), Optional.empty());
    public static final TreeGrower HUGE_BROWN_MUSHROOM = new TreeGrower(OririMod.MOD_ID + ":huge_brown_mushroom",
            Optional.empty(), Optional.of(net.minecraft.data.worldgen.features.TreeFeatures.HUGE_BROWN_MUSHROOM), Optional.empty());
    public static final TreeGrower HUGE_CRIMSON_FUNGUS = new TreeGrower(OririMod.MOD_ID + ":huge_crimson_fungus",
            Optional.empty(), Optional.of(net.minecraft.data.worldgen.features.TreeFeatures.CRIMSON_FUNGUS), Optional.empty());
    public static final TreeGrower HUGE_WARPED_FUNGUS = new TreeGrower(OririMod.MOD_ID + ":huge_warped_fungus",
            Optional.empty(), Optional.of(net.minecraft.data.worldgen.features.TreeFeatures.WARPED_FUNGUS), Optional.empty());
}
