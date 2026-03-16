package net.ganyusbathwater.oririmod.enchantment;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.MultiplyValue;
import net.minecraft.world.item.enchantment.effects.SetValue;

import java.util.List;

public class ModEnchantments {
        public static final ResourceKey<Enchantment> SNIPER = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sniper"));

        public static final ResourceKey<Enchantment> INVINCIBLE = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "invincible"));

        public static final ResourceKey<Enchantment> TEACHER = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "teacher"));

        public static final ResourceKey<Enchantment> MANA_REGENERATION = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "mana_regeneration"));
        public static final ResourceKey<Enchantment> MANA_CAPACITY = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "mana_capacity"));
        public static final ResourceKey<Enchantment> MANA_SAVINGS = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "mana_savings"));

        public static final ResourceKey<Enchantment> ELEMENT_FIRE = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_fire"));
        public static final ResourceKey<Enchantment> ELEMENT_WATER = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_water"));
        public static final ResourceKey<Enchantment> ELEMENT_NATURE = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_nature"));
        public static final ResourceKey<Enchantment> ELEMENT_EARTH = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_earth"));
        public static final ResourceKey<Enchantment> ELEMENT_LIGHT = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_light"));
        public static final ResourceKey<Enchantment> ELEMENT_DARKNESS = ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_darkness"));

        public static void bootstrap(BootstrapContext<Enchantment> context) {
                var enchantments = context.lookup(Registries.ENCHANTMENT);
                var items = context.lookup(Registries.ITEM);

                registerEnchantments(context, SNIPER, Enchantment.enchantment(Enchantment.definition(
                                // supported Items
                                items.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                                // primary item
                                items.getOrThrow(ItemTags.BOW_ENCHANTABLE),
                                // weighting (how often it occurs)
                                2,
                                // max Level
                                1,
                                // Level costs
                                Enchantment.dynamicCost(8, 9),
                                Enchantment.dynamicCost(25, 8),
                                // Anvil costs
                                5,
                                // Self-explanatory
                                EquipmentSlotGroup.MAINHAND))
                                .withEffect(
                                                EnchantmentEffectComponents.PROJECTILE_SPREAD,
                                                new SetValue(LevelBasedValue.constant(0.0F)))
                /*
                 * ensures that the custom enchantment is incompatible with certain
                 * enchantments. In this case it would be incompatible with Sharpness or Smite
                 * .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                 */
                );

                Enchantment.EnchantmentDefinition invincibleDef = Enchantment.definition(
                                items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE), // was verzauberbar ist
                                1, // minLevel
                                1, // maxLevel
                                Enchantment.dynamicCost(15, 20), // min cost
                                Enchantment.dynamicCost(40, 15), // max cost
                                20, // weight
                                EquipmentSlotGroup.MAINHAND,
                                EquipmentSlotGroup.OFFHAND,
                                EquipmentSlotGroup.HEAD,
                                EquipmentSlotGroup.CHEST,
                                EquipmentSlotGroup.LEGS,
                                EquipmentSlotGroup.FEET);

                MultiplyValue noDamage = new MultiplyValue(LevelBasedValue.constant(0f));

                // Builder erstellen, Effekt anhängen und registrieren
                var invincibleBuilder = Enchantment.enchantment(invincibleDef)
                                .withEffect(EnchantmentEffectComponents.ITEM_DAMAGE, noDamage);

                context.register(INVINCIBLE, invincibleBuilder.build(INVINCIBLE.location()));

                Enchantment.EnchantmentDefinition teacherDef = Enchantment.definition(
                                items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE), // oder ein anderes Tag / ItemSet
                                                                                   // wenn nötig
                                1, // min level
                                3, // max level = 3
                                Enchantment.dynamicCost(10, 5), // min cost (anpassen)
                                Enchantment.dynamicCost(40, 10), // max cost (anpassen)
                                10, // weight/rarity (anpassen)
                                EquipmentSlotGroup.MAINHAND, // auf welchen Slots erscheinen soll
                                EquipmentSlotGroup.OFFHAND,
                                EquipmentSlotGroup.HEAD,
                                EquipmentSlotGroup.CHEST,
                                EquipmentSlotGroup.LEGS,
                                EquipmentSlotGroup.FEET);

                // LevelMapping: Level 1 -> 1.5, Level 2 -> 2.0, Level 3 -> 3.0
                LevelBasedValue lookupFactor = LevelBasedValue.lookup(
                                List.of(2F, 3F, 4F),
                                LevelBasedValue.constant(4F) // fallback falls Level > 3; optional
                );

                // Multiply effect mit diesem factor
                MultiplyValue xpMultiplierEffect = new MultiplyValue(lookupFactor);

                // Builder erzeugen und the effects hinzufügen:
                var techerBuilder = Enchantment.enchantment(teacherDef)
                                .withEffect(EnchantmentEffectComponents.MOB_EXPERIENCE, xpMultiplierEffect)
                                .withEffect(EnchantmentEffectComponents.BLOCK_EXPERIENCE, xpMultiplierEffect);

                // registrieren (wichtig: build(location) verwenden)
                context.register(TEACHER, techerBuilder.build(TEACHER.location()));

                // --- MANA ENCHANTMENTS ---
                registerEnchantments(context, MANA_REGENERATION, Enchantment.enchantment(Enchantment.definition(
                                items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                                1, 1,
                                Enchantment.dynamicCost(15, 0), Enchantment.dynamicCost(65, 0), 2,
                                EquipmentSlotGroup.ARMOR)));

                registerEnchantments(context, MANA_CAPACITY, Enchantment.enchantment(Enchantment.definition(
                                items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                                1, 1,
                                Enchantment.dynamicCost(15, 0), Enchantment.dynamicCost(65, 0), 2,
                                EquipmentSlotGroup.ARMOR)));

                registerEnchantments(context, MANA_SAVINGS, Enchantment.enchantment(Enchantment.definition(
                                items.getOrThrow(net.ganyusbathwater.oririmod.util.ModTags.Items.MANA_WEAPONS),
                                1, 5,
                                Enchantment.dynamicCost(10, 10), Enchantment.dynamicCost(60, 10), 5,
                                EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.OFFHAND)));

                // --- ELEMENT ENCHANTMENTS ---
                var exclusiveElements = enchantments
                                .getOrThrow(net.ganyusbathwater.oririmod.util.ModTags.Enchantments.ELEMENTAL);

                Enchantment.EnchantmentDefinition elementDef = Enchantment.definition(
                                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                                1, 1,
                                Enchantment.dynamicCost(20, 0), Enchantment.dynamicCost(70, 0), 2,
                                EquipmentSlotGroup.MAINHAND);

                registerEnchantments(context, ELEMENT_FIRE,
                                Enchantment.enchantment(elementDef).exclusiveWith(exclusiveElements));
                registerEnchantments(context, ELEMENT_WATER,
                                Enchantment.enchantment(elementDef).exclusiveWith(exclusiveElements));
                registerEnchantments(context, ELEMENT_NATURE,
                                Enchantment.enchantment(elementDef).exclusiveWith(exclusiveElements));
                registerEnchantments(context, ELEMENT_EARTH,
                                Enchantment.enchantment(elementDef).exclusiveWith(exclusiveElements));
                registerEnchantments(context, ELEMENT_LIGHT,
                                Enchantment.enchantment(elementDef).exclusiveWith(exclusiveElements));
                registerEnchantments(context, ELEMENT_DARKNESS,
                                Enchantment.enchantment(elementDef).exclusiveWith(exclusiveElements));
        }

        private static void registerEnchantments(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                        Enchantment.Builder builder) {

                registry.register(key, builder.build(key.location()));
        }
}
