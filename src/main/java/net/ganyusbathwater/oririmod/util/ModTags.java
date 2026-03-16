package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Blocks {
        public static final TagKey<Block> ELDERWOODS_PROTECTED_STRUCTURE_BLOCKS = createTag(
                "elderwoods_protected_structure_blocks");
        public static final TagKey<Block> ORES = createTag("ores");
        public static final TagKey<Block> SNOW_BOOTS_VALID_BLOCKS = createTag("snow_boots_valid_blocks");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }
    }

    public static class Enchantments {
        public static final TagKey<net.minecraft.world.item.enchantment.Enchantment> ELEMENTAL = createTag("elemental");

        private static TagKey<net.minecraft.world.item.enchantment.Enchantment> createTag(String name) {
            return TagKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> MANA_WEAPONS = createTag("mana_weapons");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name));
        }
    }
}
