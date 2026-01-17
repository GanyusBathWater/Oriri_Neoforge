package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.item.custom.vestiges.SnowBoots;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SnowBootsEffect implements VestigeEffect {

    private static final ResourceLocation KNOCKBACK_RESIST_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "snow_boots_knockback_resist");

    private static final String NBT_KEY = "Oriri_SnowBoots";
    private static final String NBT_POWDER_SNOW_WALKABLE = "PowderSnowWalkable";

    private final int level;

    public SnowBootsEffect(int level) {
        this.level = Math.max(1, Math.min(3, level));
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        ItemStack stack = ctx.stack();
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SnowBoots)) return;

        int unlocked = Math.max(0, VestigeItem.getUnlockedLevel(stack));
        boolean enabled = unlocked > 0;

        if (!enabled) {
            removeKnockbackModifier(player);
            removeSpeedIfPresent(player);
            setPowderSnowWalkable(player, false);
            return;
        }

        boolean inCold = isInColdBiome(player.level(), player.blockPosition());

        // Level 1: Speed I in kalten Biomen, beim Verlassen entfernen
        if (unlocked >= 1) {
            if (inCold) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, true, false));
            } else {
                removeSpeedIfPresent(player);
            }
        }

        // Level 2: nicht in Powdered Snow versinken (Status serverseitig am Spieler speichern)
        // Hinweis: der eigentliche "nicht versinken"\-Mechanismus muss an der Stelle greifen,
        // wo Powdered Snow Verhalten geprüft wird. Hier wird nur der Zustand gepflegt.
        if (unlocked >= 2) {
            setPowderSnowWalkable(player, true);
        } else {
            setPowderSnowWalkable(player, false);
        }

        // Level 3: 100% Knockback Resistance in kalten Biomen, beim Verlassen entfernen
        if (unlocked >= 3 && inCold) {
            applyOrUpdateKnockbackModifier(player);
        } else {
            removeKnockbackModifier(player);
        }
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        tick(ctx);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player != null) {
            removeKnockbackModifier(player);
            removeSpeedIfPresent(player);
            setPowderSnowWalkable(player, false);
            clearSnowBootsTagIfEmpty(player);
        }
    }

    private static void removeSpeedIfPresent(Player player) {
        if (player == null) return;
        if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
    }

    private static boolean isInColdBiome(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        return level.getBiome(pos).value().coldEnoughToSnow(pos);
    }

    private static void applyOrUpdateKnockbackModifier(Player player) {
        if (player == null) return;

        var inst = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (inst == null) return;

        inst.removeModifier(KNOCKBACK_RESIST_ID);
        inst.addTransientModifier(new AttributeModifier(
                KNOCKBACK_RESIST_ID,
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static void removeKnockbackModifier(Player player) {
        if (player == null) return;

        var inst = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (inst == null) return;

        inst.removeModifier(KNOCKBACK_RESIST_ID);
    }

    private static void setPowderSnowWalkable(Player player, boolean walkable) {
        if (player == null || player.level().isClientSide) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);

        boolean before = tag.getBoolean(NBT_POWDER_SNOW_WALKABLE);
        if (before == walkable) return;

        tag.putBoolean(NBT_POWDER_SNOW_WALKABLE, walkable);
        root.put(NBT_KEY, tag);
    }

    private static void clearSnowBootsTagIfEmpty(Player player) {
        if (player == null || player.level().isClientSide) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);

        // Wenn nur unser Flag existiert und es false ist, Tag entfernen
        if (!tag.getBoolean(NBT_POWDER_SNOW_WALKABLE)) {
            tag.remove(NBT_POWDER_SNOW_WALKABLE);
        }

        if (tag.isEmpty()) {
            root.remove(NBT_KEY);
        } else {
            root.put(NBT_KEY, tag);
        }
    }
}