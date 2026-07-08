package net.ganyusbathwater.oririmod.event;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.ItemElementRegistry;
import net.ganyusbathwater.oririmod.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.Set;

@EventBusSubscriber(modid = OririMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class EnchantmentEventHandler {

    private static final Set<ResourceKey<Enchantment>> ELEMENT_ENCHANTS = Set.of(
            ModEnchantments.ELEMENT_FIRE,
            ModEnchantments.ELEMENT_WATER,
            ModEnchantments.ELEMENT_EARTH,
            ModEnchantments.ELEMENT_NATURE,
            ModEnchantments.ELEMENT_LIGHT,
            ModEnchantments.ELEMENT_DARKNESS
    );

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        // 1. Determine if the left item has an innate element
        // (Either registered in ItemElementRegistry or belongs to the Mana Weapons tag)
        boolean hasInnateElement = false;
        
        // ItemElementRegistry inherently checks the innate elements
        Element registeredInnate = ItemElementRegistry.getElement(left);
        // But wait! getElement now checks enchantments too!
        // We need to verify if the element is innate, not from enchantments.
        // Let's do a strict item-based check:
        // However, if getElement returns anything other than PHYSICAL, it has some element.
        // To be safe, let's check if the raw item is innate. We can bypass our new enchantment check by looking at the item directly, or just rely on the tag.
        if (left.is(net.ganyusbathwater.oririmod.util.ModTags.Items.MANA_WEAPONS)) {
            hasInnateElement = true;
        } else {
            // Check if the item is explicitly registered with an element
            // We temporarily strip enchantments to check the innate element (by passing a new stack or checking registry directly)
            ItemStack dummy = new ItemStack(left.getItem());
            if (ItemElementRegistry.getElement(dummy) != Element.PHYSICAL) {
                hasInnateElement = true;
            }
        }

        // Count element enchantments that would be on the resulting item
        // The output might not be populated yet, so we count elements on left AND right
        int elementEnchantsCount = 0;
        boolean rightAddsElement = false;

        ItemEnchantments leftEnchants = left.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (leftEnchants != null) {
            for (Holder<Enchantment> enchant : leftEnchants.keySet()) {
                if (enchant.unwrapKey().isPresent() && ELEMENT_ENCHANTS.contains(enchant.unwrapKey().get())) {
                    elementEnchantsCount++;
                }
            }
        }

        ItemEnchantments rightEnchants = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (rightEnchants != null) {
            for (Holder<Enchantment> enchant : rightEnchants.keySet()) {
                if (enchant.unwrapKey().isPresent() && ELEMENT_ENCHANTS.contains(enchant.unwrapKey().get())) {
                    elementEnchantsCount++;
                    rightAddsElement = true;
                }
            }
        }

        // 2. If the right item tries to add an element to a weapon that already has an innate element, block it!
        if (hasInnateElement && rightAddsElement) {
            event.setCanceled(true);
            return;
        }

        // 3. If combining them would result in more than 1 element enchantment:
        if (elementEnchantsCount > 1) {
            // If the item has an innate element, it shouldn't have ANY element enchantments (already blocked above)
            // But if it's a NORMAL weapon, we allow the NEW element to override the old one!
            if (!hasInnateElement && rightAddsElement) {
                ItemStack out = left.copy();
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                
                // Copy non-element enchantments from left
                if (leftEnchants != null) {
                    for (Holder<Enchantment> enchant : leftEnchants.keySet()) {
                        if (enchant.unwrapKey().isPresent() && ELEMENT_ENCHANTS.contains(enchant.unwrapKey().get())) {
                            continue; // Skip old element
                        }
                        mutable.set(enchant, leftEnchants.getLevel(enchant));
                    }
                }
                
                // Apply all enchantments from right (which includes the new element)
                int cost = 0;
                if (rightEnchants != null) {
                    for (Holder<Enchantment> enchant : rightEnchants.keySet()) {
                        int rightLevel = rightEnchants.getLevel(enchant);
                        int leftLevel = mutable.getLevel(enchant);
                        
                        if (leftLevel < rightLevel) {
                            mutable.set(enchant, rightLevel);
                            cost += rightLevel;
                        } else if (leftLevel == rightLevel && rightLevel < enchant.value().getMaxLevel()) {
                            mutable.set(enchant, rightLevel + 1);
                            cost += rightLevel + 1;
                        }
                    }
                }
                
                out.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mutable.toImmutable());
                
                int baseCost = left.getOrDefault(net.minecraft.core.component.DataComponents.REPAIR_COST, 0);
                out.set(net.minecraft.core.component.DataComponents.REPAIR_COST, baseCost * 2 + 1);
                
                if (event.getName() != null && !event.getName().isEmpty()) {
                    out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(event.getName()));
                    cost += 1;
                }
                
                event.setOutput(out);
                event.setCost(cost + 5 + baseCost); 
                event.setMaterialCost(1);
                return;
            } else {
                event.setCanceled(true);
                return;
            }
        }
    }
}
