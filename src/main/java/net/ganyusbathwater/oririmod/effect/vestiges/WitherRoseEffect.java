package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.item.custom.vestiges.WitherRose;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WitherRoseEffect implements VestigeEffect {

    private static final String NBT_KEY = "Oriri_WitherRose";
    private static final String NBT_ENABLED = "Enabled";
    private static final String NBT_LEVEL = "Level";

    private final int level;

    public WitherRoseEffect(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        ItemStack stack = ctx.stack();
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof WitherRose)) return;

        int lvl = clampLevel(VestigeItem.getUnlockedLevel(stack));
        boolean enabled = lvl > 0;

        setEnabled(player, enabled);
        setActiveLevel(player, enabled ? lvl : 0);
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;
        tick(ctx);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        setEnabled(player, false);

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);
        tag.remove(NBT_LEVEL);
        root.put(NBT_KEY, tag);
    }

    public static float modifyIncomingDamage(Player player, DamageSource source, float amount) {
        if (player == null || source == null) return amount;

        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();

        int lvl = getActiveWitherRoseLevel(player);

        String srcType = source.type().msgId();
        boolean attackerIsWither = isWitherOrWitherSkeleton(attacker);
        boolean directIsWither = isWitherOrWitherSkeleton(direct);
        boolean sourceIsWitherType = "wither".equals(srcType);

        if (!(attackerIsWither || directIsWither || sourceIsWitherType)) return amount;
        if (lvl <= 0) return amount;

        float multiplier;
        switch (lvl) {
            case 1 -> multiplier = 0.75f;
            case 2, 3 -> multiplier = 0.50f;
            default -> multiplier = 1.0f;
        }

        return amount * multiplier;
    }

    public static float modifyOutgoingDamage(Player player, LivingEntity target, float amount) {
        if (player == null || target == null) return amount;
        if (!isWitherOrWitherSkeleton(target)) return amount;

        int lvl = getActiveWitherRoseLevel(player);
        if (lvl != 3) return amount;

        return amount * 1.50f;
    }

    private static boolean isWitherOrWitherSkeleton(Entity e) {
        return e instanceof WitherBoss || e instanceof WitherSkeleton;
    }

    private static int clampLevel(int level) {
        if (level < 0) return 0;
        if (level > 3) return 3;
        return level;
    }

    public static boolean isEnabled(Player player) {
        if (player == null) return false;
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);
        return tag.getBoolean(NBT_ENABLED);
    }

    private static void setEnabled(Player player, boolean enabled) {
        if (player == null || player.level().isClientSide) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);

        boolean before = tag.getBoolean(NBT_ENABLED);
        if (before == enabled) return;

        tag.putBoolean(NBT_ENABLED, enabled);
        root.put(NBT_KEY, tag);
    }

    private static int getActiveWitherRoseLevel(Player player) {
        if (player == null) return 0;
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);
        if (!tag.getBoolean(NBT_ENABLED)) return 0;
        return clampLevel(tag.getInt(NBT_LEVEL));
    }

    private static void setActiveLevel(Player player, int level) {
        if (player == null || player.level().isClientSide) return;

        int clamped = clampLevel(level);

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);

        int before = tag.getInt(NBT_LEVEL);
        if (before == clamped) return;

        tag.putInt(NBT_LEVEL, clamped);
        root.put(NBT_KEY, tag);
    }
}