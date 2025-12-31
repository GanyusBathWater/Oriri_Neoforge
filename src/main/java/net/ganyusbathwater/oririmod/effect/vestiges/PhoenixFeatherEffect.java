// file: `java/net/ganyusbathwater/oririmod/effect/vestiges/PhoenixFeatherEffect.java`
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PhoenixFeatherEffect implements VestigeEffect {

    private static final String NBT_FATAL_GUARD_KEY = "Oriri_FatalDamageGuard";
    private static final String NBT_FATAL_GUARD_CHANCE = "Chance";
    private static final String NBT_FATAL_GUARD_COOLDOWN_UNTIL = "CooldownUntil";

    private static final int FATAL_GUARD_COOLDOWN_TICKS = 20 * 30; // 30s

    /**
     * Erwartet Wert als Anteil \(0.0 .. 1.0\), z.B. 0.33f = 33\%.
     */
    private final float chanceFraction;

    public PhoenixFeatherEffect(float chanceFraction) {
        this.chanceFraction = chanceFraction;
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        // Alle Level-Effekte werden getickt; der höchste erlaubte Wert ist der vom höchsten Level.
        // Setze nur, wenn dieses Effect-Objekt zum aktuell höchsten angewendeten Level gehört.
        int levelsToApply = Math.min(ctx.levelUnlocked(), 3); // 3 = aktuell definierte Levels beim PhoenixFeather
        if (levelsToApply <= 0) return;

        // Level-Indizes: 0..2. Höchster angewendeter Index = levelsToApply - 1.
        // Da wir hier keine direkte Index-Info haben, nehmen wir den konservativen Ansatz:
        // immer auf den maximalen Wert setzen, denn niedrigere Level werden später im selben Tick
        // nicht mehr überschreiben, wenn du sicherstellst, dass nur der höchste Level schreibt.
        // -> Dafür guarden wir: schreibe nur, wenn chanceFraction dem Maximalwert der freigeschalteten Levels entspricht.
        float desired = getDesiredChanceFractionForUnlockedLevel(levelsToApply);
        if (Float.compare(this.chanceFraction, desired) != 0) return;

        setNewNegatingChance(player, desired);
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        tick(ctx);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        // optional: Chance auf 0 setzen, damit ohne Item nichts passiert
        if (ctx == null || ctx.isClient()) return;
        Player player = ctx.player();
        if (player == null) return;
        setNewNegatingChance(player, 0.0f);
    }

    private static float getDesiredChanceFractionForUnlockedLevel(int levelsToApply) {
        // Muss zu `PhoenixFeather` passen: 0.1f, 0.2f, 0.33f
        return switch (levelsToApply) {
            case 1 -> 0.1f;
            case 2 -> 0.2f;
            default -> 0.33f;
        };
    }

    public static float getNegatingChancePercent(Player player) {
        if (player == null) return 0.0f;
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_FATAL_GUARD_KEY);
        return tag.getFloat(NBT_FATAL_GUARD_CHANCE);
    }

    /**
     * Erwartet Anteil \(0.0 .. 1.0\), speichert intern Prozent \(0..100\).
     */
    public static float setNewNegatingChance(Player player, float newChanceFraction) {
        if (player == null || player.level().isClientSide) return 0.0f;

        float clampedFraction = Math.max(0.0f, Math.min(1.0f, newChanceFraction));
        float percent = clampedFraction * 100.0f;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_FATAL_GUARD_KEY);
        tag.putFloat(NBT_FATAL_GUARD_CHANCE, percent);
        root.put(NBT_FATAL_GUARD_KEY, tag);

        return percent;
    }

    /**
     * chancePercent in \(0..100\).
     */
    public static boolean tryNegateFatalDamageLikeTotem(Player player, DamageSource source, float chancePercent) {
        if (player == null || player.level().isClientSide) return false;
        if (!player.isAlive()) return false;
        if (chancePercent <= 0.0f) return false;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_FATAL_GUARD_KEY);

        long now = player.level().getGameTime();
        long cooldownUntil = tag.getLong(NBT_FATAL_GUARD_COOLDOWN_UNTIL);
        if (now < cooldownUntil) return false;

        float clampedChance = Math.max(0.0f, Math.min(100.0f, chancePercent));
        float roll = player.getRandom().nextFloat() * 100.0f;
        if (roll >= clampedChance) return false;

        player.setHealth(1.0f);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));

        Level level = player.level();
        Vec3 pos = player.position();
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

        if (level instanceof ServerLevel serverLevel) {
            ItemStack particleStack = new ItemStack(ModItems.PHOENIX_FEATHER.get());
            ItemParticleOption option = new ItemParticleOption(ParticleTypes.ITEM, particleStack);
            serverLevel.sendParticles(
                    option,
                    pos.x, pos.y + 1.0d, pos.z,
                    48,
                    0.5d, 0.75d, 0.5d,
                    0.25d
            );
        }

        if (player instanceof ServerPlayer sp) {
            sp.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
            sp.containerMenu.broadcastChanges();
            sp.connection.send(new ClientboundEntityEventPacket(player, (byte) 35));
        }

        tag.putFloat(NBT_FATAL_GUARD_CHANCE, clampedChance);
        tag.putLong(NBT_FATAL_GUARD_COOLDOWN_UNTIL, now + FATAL_GUARD_COOLDOWN_TICKS);
        root.put(NBT_FATAL_GUARD_KEY, tag);

        return true;
    }
}