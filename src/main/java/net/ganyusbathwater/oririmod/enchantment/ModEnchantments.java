package net.ganyusbathwater.oririmod.enchantment;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> SNIPER = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sniper"));

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
                /*
                ensures that the custom enchantment is incompatible with certain enchantments. In this case it would be incompatible with  Sharpness or Smite
                .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                 */
        );

    }

    private static void registerEnchantments(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }
}
