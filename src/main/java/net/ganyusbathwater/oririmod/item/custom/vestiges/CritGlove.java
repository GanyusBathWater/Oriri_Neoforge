// java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CritGlove extends VestigeItem {

    // Gemeinsame ID für den Attribut‑Modifier (wie bei HeartOfTheTank)
    private static final ResourceLocation CRIT_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_crit_bonus");

    public CritGlove(Item.Properties props) {
        super(props, List.of(
                List.of(critBonusEffect()), // Level 1
                List.of(critBonusEffect()), // Level 2
                List.of(critBonusEffect())  // Level 3
        ));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        this.setUnlockedLevel(stack, this.getMaxLevel());
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide) return;
        if (this.getUnlockedLevel(stack) < this.getMaxLevel()) {
            this.setUnlockedLevel(stack, this.getMaxLevel());
        }
    }

    // Pro Level ein AttackSpeed‑Bonus, der "schneller/öfter Krits" simuliert:
    // Lvl 1: +25%, Lvl 2: +50%, Lvl 3: +75%
    private static VestigeEffect critBonusEffect() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                if (!(stack.getItem() instanceof CritGlove self)) return;
                int unlocked = self.getUnlockedLevel(stack);
                if (lvl != unlocked) return;

                var inst = player.getAttribute(Attributes.ATTACK_SPEED);
                if (inst == null) return;

                double bonus = switch (unlocked) {
                    case 1 -> 0.25D; // +25% AttackSpeed
                    case 2 -> 0.50D; // +50%
                    case 3 -> 0.75D; // +75%
                    default -> 0.0D;
                };

                // Bisherigen Modifier entfernen
                var existing = inst.getModifier(CRIT_BONUS_ID);
                if (existing != null) {
                    inst.removeModifier(CRIT_BONUS_ID);
                }

                if (bonus > 0.0D) {
                    inst.addTransientModifier(new AttributeModifier(
                            CRIT_BONUS_ID,
                            bonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                }
            }
        };
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.crit_glove";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }
}