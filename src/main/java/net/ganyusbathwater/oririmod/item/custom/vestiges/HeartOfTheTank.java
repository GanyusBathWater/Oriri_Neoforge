package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class HeartOfTheTank extends VestigeItem {
    private static final ResourceLocation HEALTH_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_health_bonus");

    public HeartOfTheTank(Properties props) {
        super(props, List.of(
                List.of(healthBonusHearts(4)),  // Stufe 1: +4 Herzen (8 HP)
                List.of(healthBonusHearts(6)),  // Stufe 2: +6 Herzen (12 HP)
                List.of(healthBonusHearts(10))  // Stufe 3: +10 Herzen (20 HP)
        ));
    }

    // Wie bei StrangeEnderEye: neue Stacks sind voll freigeschaltet
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        this.setUnlockedLevel(stack, this.getMaxLevel());
        return stack;
    }

    // Bestehende Stacks im Server-Tick nachziehen
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide) return;
        if (this.getUnlockedLevel(stack) < this.getMaxLevel()) {
            this.setUnlockedLevel(stack, this.getMaxLevel());
        }
    }

    private static VestigeEffect healthBonusHearts(int hearts) {
        final double extraHp = hearts * 2.0; // 1 Herz = 2 HP
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                var inst = player.getAttribute(Attributes.MAX_HEALTH);
                if (inst == null) return;

                // Manager entfernt den Modifier vorab – hier nur hinzufügen
                if (extraHp > 0) {
                    inst.addTransientModifier(new AttributeModifier(
                            HEALTH_BONUS_ID, extraHp, AttributeModifier.Operation.ADD_VALUE));
                }

                // Gesundheit auf neues Maximum klemmen
                float max = player.getMaxHealth();
                if (player.getHealth() > max) {
                    player.setHealth(max);
                }
            }
        };
    }
}