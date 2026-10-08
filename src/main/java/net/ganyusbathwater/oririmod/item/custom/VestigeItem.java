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
        net.ganyusbathwater.oririmod.util.TooltipHelper.addLevelInfo(tooltip, this.getDescriptionId(), unlockedLevel);

        // Lore (Key: item.oririmod.\<id\>.lore)
        net.ganyusbathwater.oririmod.util.TooltipHelper.addLore(tooltip, this.getDescriptionId() + ".lore");

        // Rarität am Schluss (kommt aus ModRarityCarrier)
        tooltip.addAll(buildModTooltip(stack, context, flag));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) {
            return true;
        }

        Item itemToEquip = stack.getItem();
        var curioInventoryOpt = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player);
        if (curioInventoryOpt.isPresent()) {
            var inv = curioInventoryOpt.get();
            for (var entry : inv.getCurios().entrySet()) {
                String identifier = entry.getKey();
                var handler = entry.getValue();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack curioStack = handler.getStacks().getStackInSlot(i);
                    if (!curioStack.isEmpty() && curioStack.getItem() == itemToEquip) {
                        if (identifier.equals(slotContext.identifier()) && i == slotContext.index()) {
                            continue;
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        if (ctx == null)
            return;

        Entity entity = ctx.entity();
        if (!(entity instanceof Player player))
            return;

        boolean client = player.level().isClientSide;
        int unlockedLevel = Math.max(0, this.getUnlockedLevel(stack));
        int maxDefinedLevels = effectsByLevel.size();
        int levelsToApply = Math.min(unlockedLevel, maxDefinedLevels);

        if (levelsToApply <= 0)
            return;

        VestigeContext vctx = new VestigeContext(
                player,
                player.level(),
                stack,
                unlockedLevel,
                client);

        for (int i = 0; i < levelsToApply; i++) {
            List<VestigeEffect> effects = effectsByLevel.get(i);
            if (effects == null || effects.isEmpty())
                continue;

            for (VestigeEffect effect : effects) {
                if (effect == null)
                    continue;
                effect.tick(vctx);
            }
        }
    }

    @Override
    public void onEquip(SlotContext ctx, ItemStack prevStack, ItemStack stack) {
        if (ctx == null)
            return;

        Entity entity = ctx.entity();
        if (!(entity instanceof Player player))
            return;

        boolean client = player.level().isClientSide;
        int unlockedLevel = Math.max(0, this.getUnlockedLevel(stack));
        int maxDefinedLevels = effectsByLevel.size();
        int levelsToApply = Math.min(unlockedLevel, maxDefinedLevels);
        if (levelsToApply <= 0)
            return;

        VestigeContext vctx = new VestigeContext(
                player,
                player.level(),
                stack,
                unlockedLevel,
                client);

        for (int i = 0; i < levelsToApply; i++) {
            List<VestigeEffect> effects = effectsByLevel.get(i);
            if (effects == null || effects.isEmpty())
                continue;

            for (VestigeEffect effect : effects) {
                if (effect == null)
                    continue;
                effect.onEquip(vctx);
            }
        }
    }

    @Override
    public void onUnequip(SlotContext ctx, ItemStack newStack, ItemStack stack) {
        if (ctx == null)
            return;

        Entity entity = ctx.entity();
        if (!(entity instanceof Player player))
            return;

        boolean client = player.level().isClientSide;
        int unlockedLevel = Math.max(0, this.getUnlockedLevel(stack));
        int maxDefinedLevels = effectsByLevel.size();
        int levelsToApply = Math.min(unlockedLevel, maxDefinedLevels);
        if (levelsToApply <= 0)
            return;

        VestigeContext vctx = new VestigeContext(
                player,
                player.level(),
                stack,
                unlockedLevel,
                client);

        for (int i = 0; i < levelsToApply; i++) {
            List<VestigeEffect> effects = effectsByLevel.get(i);
            if (effects == null || effects.isEmpty())
                continue;

            for (VestigeEffect effect : effects) {
                if (effect == null)
                    continue;
                effect.onUnequip(vctx);
            }
        }
    }

    public static int getUnlockedLevel(ItemStack stack) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            net.minecraft.nbt.CompoundTag customData = stack
                    .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (customData.contains("oriri_level")) {
                return customData.getInt("oriri_level");
            }
        }
        return 1;
    }
}