package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffects;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class MinersLantern extends VestigeItem {

    private static final ResourceLocation LUCK_BONUS_ID = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "vestige_miners_lantern_luck");

    public MinersLantern(Item.Properties props) {
        super(props, List.of(
                List.of(hasteEffect()),          // Level 1: Haste
                List.of(blindnessImmunity()),    // Level 2: Blindness‑Immunität
                List.of(luckBonusEffect())       // Level 3: +5 Luck
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

    private static VestigeEffect hasteEffect() {
        // z\.B. Haste I, 200 Ticks, flackerfrei wie bei anderen mobEffect‑Hilfen
        return VestigeEffects.mobEffect(MobEffects.DIG_SPEED, 0, 200);
    }

    private static VestigeEffect blindnessImmunity() {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                // Blindness, wenn vorhanden, sofort entfernen
                if (player.hasEffect(MobEffects.BLINDNESS)) {
                    player.removeEffect(MobEffects.BLINDNESS);
                }
            }
        };
    }

    private static VestigeEffect luckBonusEffect() {
        return VestigeEffects.Luck(5);
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.miners_lantern";
    }
}