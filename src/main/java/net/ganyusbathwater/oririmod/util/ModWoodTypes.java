package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class ModWoodTypes {
    public static final BlockSetType ELDER_BLOCK_SET_TYPE = BlockSetType.register(new BlockSetType(OririMod.MOD_ID + ":elder"));
    public static final WoodType ELDER_WOOD_TYPE = WoodType.register(new WoodType(OririMod.MOD_ID + ":elder", ELDER_BLOCK_SET_TYPE));

    public static final BlockSetType SCARLET_BLOCK_SET_TYPE = BlockSetType.register(new BlockSetType(OririMod.MOD_ID + ":scarlet"));
    public static final WoodType SCARLET_WOOD_TYPE = WoodType.register(new WoodType(OririMod.MOD_ID + ":scarlet", SCARLET_BLOCK_SET_TYPE));

    public static final BlockSetType ABYSS_CROWN_BLOCK_SET_TYPE = BlockSetType.register(new BlockSetType(OririMod.MOD_ID + ":abyss_crown"));
    public static final WoodType ABYSS_CROWN_WOOD_TYPE = WoodType.register(new WoodType(OririMod.MOD_ID + ":abyss_crown", ABYSS_CROWN_BLOCK_SET_TYPE));
}
