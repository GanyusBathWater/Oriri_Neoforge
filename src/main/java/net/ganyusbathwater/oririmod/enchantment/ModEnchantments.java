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
    public static final ResourceKey<Enchantment> SNIPER = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sniper"));

    public static final ResourceKey<Enchantment> INVINCIBLE = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "invincible"));

    public static final ResourceKey<Enchantment> TEACHER = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "teacher"));

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        var enchantments = context.lookup(Registries.ENCHANTMENT);
        var items = context.lookup(Registries.ITEM);

        registerEnchantments(context, SNIPER, Enchantment.enchantment(Enchantment.definition(
            //supported Items
            items.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
            //primary item
            items.getOrThrow(ItemTags.BOW_ENCHANTABLE),
            //weighting (how often it occurs)
            2,
            //max Level
            1,
            //Level costs
            Enchantment.dynamicCost(8,9),
            Enchantment.dynamicCost(25, 8),
            //Anvil costs
            5,
            //Self-explanatory
            EquipmentSlotGroup.MAINHAND))
                    .withEffect(
                            EnchantmentEffectComponents.PROJECTILE_SPREAD,
                            new SetValue(LevelBasedValue.constant(0.0F))
                    )
            /*
            ensures that the custom enchantment is incompatible with certain enchantments. In this case it would be incompatible with  Sharpness or Smite
            .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
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
                EquipmentSlotGroup.FEET
        );

        MultiplyValue noDamage = new MultiplyValue(LevelBasedValue.constant(0f));

        // Builder erstellen, Effekt anhängen und registrieren
        var invincibleBuilder = Enchantment.enchantment(invincibleDef)
                .withEffect(EnchantmentEffectComponents.ITEM_DAMAGE, noDamage);

        context.register(INVINCIBLE, invincibleBuilder.build(INVINCIBLE.location()));


        Enchantment.EnchantmentDefinition teacherDef = Enchantment.definition(
                items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE), // oder ein anderes Tag / ItemSet wenn nötig
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
                EquipmentSlotGroup.FEET
        );

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
    }

    private static void registerEnchantments(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {

        registry.register(key, builder.build(key.location()));
    }
}
