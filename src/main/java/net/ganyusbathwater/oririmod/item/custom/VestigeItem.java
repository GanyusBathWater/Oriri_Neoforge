package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.effect.vestiges.VestigeContext;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Collections;
import java.util.List;

public abstract class VestigeItem extends Item implements ICurioItem, ModRarityCarrier {

    private final List<List<VestigeEffect>> effectsByLevel;
    private final ModRarity modRarity;

    protected VestigeItem(Properties props, List<List<VestigeEffect>> effectsByLevel, ModRarity rarity) {
        super(props);
        this.effectsByLevel = effectsByLevel == null ? Collections.emptyList() : effectsByLevel;
        this.modRarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return modRarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int unlockedLevel = Math.max(0, getUnlockedLevel(stack));

        // Aktuelles Level (Key: item.oririmod.\<id\>.level -> "Current Level: %s")
        tooltip.add(Component.translatable(this.getDescriptionId() + ".level", unlockedLevel));

        // Level\-Beschreibungen bis zum aktuellen Level
        for (int i = 1; i <= unlockedLevel; i++) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".level." + i + ".description"));
        }

        // Lore (Key: item.oririmod.\<id\>.lore)
        tooltip.add(Component.translatable(this.getDescriptionId() + ".lore"));

        // Rarität am Schluss (kommt aus ModRarityCarrier)
        tooltip.addAll(buildModTooltip(stack, context, flag));
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        if (ctx == null) return;

        Entity entity = ctx.entity();
        if (!(entity instanceof Player player)) return;

        boolean client = player.level().isClientSide;
        int unlockedLevel = Math.max(0, this.getUnlockedLevel(stack));
        int maxDefinedLevels = effectsByLevel.size();
        int levelsToApply = Math.min(unlockedLevel, maxDefinedLevels);

        if (levelsToApply <= 0) return;

        VestigeContext vctx = new VestigeContext(
                player,
                player.level(),
                stack,
                unlockedLevel,
                client
        );

        for (int i = 0; i < levelsToApply; i++) {
            List<VestigeEffect> effects = effectsByLevel.get(i);
            if (effects == null || effects.isEmpty()) continue;

            for (VestigeEffect effect : effects) {
                if (effect == null) continue;
                effect.tick(vctx);
            }
        }
    }

    @Override
    public void onEquip(SlotContext ctx, ItemStack prevStack, ItemStack stack) {
        if (ctx == null) return;

        Entity entity = ctx.entity();
        if (!(entity instanceof Player player)) return;

        boolean client = player.level().isClientSide;
        int unlockedLevel = Math.max(0, this.getUnlockedLevel(stack));
        int maxDefinedLevels = effectsByLevel.size();
        int levelsToApply = Math.min(unlockedLevel, maxDefinedLevels);
        if (levelsToApply <= 0) return;

        VestigeContext vctx = new VestigeContext(
                player,
                player.level(),
                stack,
                unlockedLevel,
                client
        );

        for (int i = 0; i < levelsToApply; i++) {
            List<VestigeEffect> effects = effectsByLevel.get(i);
            if (effects == null || effects.isEmpty()) continue;

            for (VestigeEffect effect : effects) {
                if (effect == null) continue;
                effect.onEquip(vctx);
            }
        }
    }

    @Override
    public void onUnequip(SlotContext ctx, ItemStack newStack, ItemStack stack) {
        if (ctx == null) return;

        Entity entity = ctx.entity();
        if (!(entity instanceof Player player)) return;

        boolean client = player.level().isClientSide;
        int unlockedLevel = Math.max(0, this.getUnlockedLevel(stack));
        int maxDefinedLevels = effectsByLevel.size();
        int levelsToApply = Math.min(unlockedLevel, maxDefinedLevels);
        if (levelsToApply <= 0) return;

        VestigeContext vctx = new VestigeContext(
                player,
                player.level(),
                stack,
                unlockedLevel,
                client
        );

        for (int i = 0; i < levelsToApply; i++) {
            List<VestigeEffect> effects = effectsByLevel.get(i);
            if (effects == null || effects.isEmpty()) continue;

            for (VestigeEffect effect : effects) {
                if (effect == null) continue;
                effect.onUnequip(vctx);
            }
        }
    }

    public static int getUnlockedLevel(ItemStack stack) {
        return 3;
    }
}