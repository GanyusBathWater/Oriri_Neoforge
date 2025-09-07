package net.ganyusbathwater.oririmod.item.custom;


import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class ModArmorMaterials {
    public static final Holder<ArmorMaterial> CRYSTAL_ARMOR_MATERIAL = registerArmorMaterial("crystal_armor",
            Util.make(new EnumMap<>(ArmorItem.Type.class), attribute -> {
                //the int is the defense value
                attribute.put(ArmorItem.Type.BOOTS, 2);
                attribute.put(ArmorItem.Type.LEGGINGS, 4);
                attribute.put(ArmorItem.Type.CHESTPLATE, 6);
                attribute.put(ArmorItem.Type.HELMET, 2);
                attribute.put(ArmorItem.Type.BODY, 4);
            }),20, 2f, 0.1f, () -> Items.AMETHYST_SHARD);

    public static final Holder<ArmorMaterial> ANCIENT_ARMOR_MATERIAL = registerArmorMaterial("ancient_armor",
            Util.make(new EnumMap<>(ArmorItem.Type.class), attribute -> {
                attribute.put(ArmorItem.Type.BOOTS, 2);
                attribute.put(ArmorItem.Type.LEGGINGS, 4);
                attribute.put(ArmorItem.Type.CHESTPLATE, 6);
                attribute.put(ArmorItem.Type.HELMET, 2);
                attribute.put(ArmorItem.Type.BODY, 4);
            }),20, 2f, 0.1f, () -> Items.ECHO_SHARD);

    public static final Holder<ArmorMaterial> BLUE_ICE_ARMOR_MATERIAL = registerArmorMaterial("blue_ice_armor",
            Util.make(new EnumMap<>(ArmorItem.Type.class), attribute -> {
                attribute.put(ArmorItem.Type.BOOTS, 2);
                attribute.put(ArmorItem.Type.LEGGINGS, 4);
                attribute.put(ArmorItem.Type.CHESTPLATE, 6);
                attribute.put(ArmorItem.Type.HELMET, 2);
                attribute.put(ArmorItem.Type.BODY, 4);
            }),20, 2f, 0.1f, () -> Items.BLUE_ICE);

    public static final Holder<ArmorMaterial> GILDED_NETHERRITE_ARMOR_MATERIAL = registerArmorMaterial("gilded_netherite_armor",
            Util.make(new EnumMap<>(ArmorItem.Type.class), attribute -> {
                attribute.put(ArmorItem.Type.BOOTS, 2);
                attribute.put(ArmorItem.Type.LEGGINGS, 4);
                attribute.put(ArmorItem.Type.CHESTPLATE, 6);
                attribute.put(ArmorItem.Type.HELMET, 2);
                attribute.put(ArmorItem.Type.BODY, 4);
            }),20, 2f, 0.1f, () -> Items.GILDED_BLACKSTONE);

    public static final Holder<ArmorMaterial> MOLTEN_ARMOR_MATERIAL = registerArmorMaterial("molten_armor",
            Util.make(new EnumMap<>(ArmorItem.Type.class), attribute -> {
                attribute.put(ArmorItem.Type.BOOTS, 2);
                attribute.put(ArmorItem.Type.LEGGINGS, 4);
                attribute.put(ArmorItem.Type.CHESTPLATE, 6);
                attribute.put(ArmorItem.Type.HELMET, 2);
                attribute.put(ArmorItem.Type.BODY, 4);
            }),20, 2f, 0.1f, () -> Items.IRON_BARS);

    public static final Holder<ArmorMaterial> PRISMARINE_ARMOR_MATERIAL = registerArmorMaterial("prismarine_armor",
            Util.make(new EnumMap<>(ArmorItem.Type.class), attribute -> {
                attribute.put(ArmorItem.Type.BOOTS, 2);
                attribute.put(ArmorItem.Type.LEGGINGS, 4);
                attribute.put(ArmorItem.Type.CHESTPLATE, 6);
                attribute.put(ArmorItem.Type.HELMET, 2);
                attribute.put(ArmorItem.Type.BODY, 4);
            }),20, 2f, 0.1f, () -> Items.PRISMARINE_SHARD);

    private static Holder<ArmorMaterial> registerArmorMaterial(String name, EnumMap<ArmorItem.Type, Integer> typeProtection, int enchantability, float toughness, float knockbackResistance, Supplier<Item> ingredientItem) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, name);
        Holder<SoundEvent> equipSound = SoundEvents.ARMOR_EQUIP_NETHERITE;
        Supplier<Ingredient> ingredient = () -> Ingredient.of(ingredientItem.get());
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(location));

        EnumMap<ArmorItem.Type, Integer> typeMap = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            typeMap.put(type, typeProtection.get(type));
        }

        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, location, new ArmorMaterial(typeProtection, enchantability, equipSound, ingredient, layers, toughness, knockbackResistance));
    }
}