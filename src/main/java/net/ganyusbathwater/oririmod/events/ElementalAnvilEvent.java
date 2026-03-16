package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class ElementalAnvilEvent {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty())
            return;

        // Ensure right is an enchanted book
        if (!right.is(Items.ENCHANTED_BOOK))
            return;

        ItemEnchantments leftEnchants = left.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments rightEnchants = right.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

        Holder<Enchantment> leftElement = getElement(leftEnchants);
        Holder<Enchantment> rightElement = getElement(rightEnchants);

        // We only intervene if we're swapping elements
        if (leftElement != null && rightElement != null && !leftElement.equals(rightElement)) {

            ItemStack output = left.copy();
            ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

            int baseCost = 0;

            // Reapply left enchantments, skipping the old element
            for (Holder<Enchantment> ench : leftEnchants.keySet()) {
                if (!ench.equals(leftElement)) {
                    mut.set(ench, leftEnchants.getLevel(ench));
                }
            }

            // Build final enchantments from the book
            for (Holder<Enchantment> ench : rightEnchants.keySet()) {
                int rightLvl = rightEnchants.getLevel(ench);
                int leftLvl = mut.getLevel(ench);

                // Set the new element
                if (ench.equals(rightElement)) {
                    mut.set(ench, rightLvl);
                    baseCost += 5; // Flat cost for applying new overriding element
                } else if (!ench.equals(leftElement)) {
                    // Just take the higher level if we have it, or set it
                    int finalLvl = Math.max(leftLvl, rightLvl);
                    if (leftLvl == rightLvl)
                        finalLvl = Math.min(ench.value().getMaxLevel(), leftLvl + 1);
                    mut.set(ench, finalLvl);
                    baseCost += finalLvl;
                }
            }

            output.set(DataComponents.ENCHANTMENTS, mut.toImmutable());

            // Handle Custom Naming in Anvil
            String name = event.getName();
            if (name != null && !name.isEmpty() && !name.equals(output.getHoverName().getString())) {
                output.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
                baseCost += 1;
            } else if ((name == null || name.isEmpty()) && output.has(DataComponents.CUSTOM_NAME)) {
                output.remove(DataComponents.CUSTOM_NAME);
                baseCost += 1;
            }

            event.setOutput(output);

            // Fallback cost to 1 if it somehow reached 0
            if (baseCost <= 0)
                baseCost = 1;

            event.setCost(baseCost);
            event.setMaterialCost(1);
        }
    }

    private static Holder<Enchantment> getElement(ItemEnchantments enchants) {
        for (Holder<Enchantment> ench : enchants.keySet()) {
            if (ench.is(ModTags.Enchantments.ELEMENTAL)) {
                return ench;
            }
        }
        return null;
    }
}
