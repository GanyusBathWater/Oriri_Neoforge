package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.combat.ElementalDamageHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DuellantCortextEffect implements VestigeEffect {

    private static final int COOLDOWN_TICKS = 20;
    private static final double RADIUS = 12.0;

    private static final String NBT_KEY = "Oriri_DuellantCortex_Combat";
    private static final String NBT_APPLIED = "Applied";
    private static final String NBT_LAST_INCOMING = "LastIncoming";
    private static final String NBT_LAST_OUTGOING = "LastOutgoing";

    // Default-Fabrik (falls anderweitig genutzt)
    public static VestigeEffect duellantCortexCombat() {
        return new DuellantCortextEffect(0.05f, 0.00f);
    }

    private final float defensePerMonsterPct;
    private final float damagePerMonsterPct;

    public DuellantCortextEffect(float defensePerMonsterPct, float damagePerMonsterPct) {
        this.defensePerMonsterPct = Math.max(0.0f, defensePerMonsterPct);
        this.damagePerMonsterPct = Math.max(0.0f, damagePerMonsterPct);
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        if (player.tickCount % COOLDOWN_TICKS != 0) return;

        int monsters = countMonsters(player);

        // Nur Werte dieses Effekts anwenden (höchstes Level steuert, weil nur dessen Effekt aktiv sein soll)
        applyOrUpdate(player, ctx.levelUnlocked(), monsters, defensePerMonsterPct, damagePerMonsterPct);
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        int monsters = countMonsters(player);
        applyOrUpdate(player, ctx.levelUnlocked(), monsters, defensePerMonsterPct, damagePerMonsterPct);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.isClient()) return;

        Player player = ctx.player();
        if (player == null) return;

        ElementalDamageHandler.setPlayerIncomingMultiplier(player, 1.0f);
        ElementalDamageHandler.setPlayerOutgoingMultiplier(player, 1.0f);

        CompoundTag root = player.getPersistentData();
        root.remove(NBT_KEY);
    }

    /**
     * @param defensePerMonsterPct Prozent pro Monster als 0..1 (z.B. 0.05f = 5%)
     * @param damagePerMonsterPct  Prozent pro Monster als 0..1 (z.B. 0.025f = 2,5%)
     */
    private static void applyOrUpdate(Player player, int levelUnlocked, int monsters, float defensePerMonsterPct, float damagePerMonsterPct) {
        if (levelUnlocked <= 0) return;

        // incoming *= max(0, 1 - defensePct * monsters)
        float incoming = 1.0f;
        if (defensePerMonsterPct > 0.0f) {
            float reduced = 1.0f - (defensePerMonsterPct * monsters);
            if (reduced < 0.0f) reduced = 0.0f;
            incoming = reduced;
        }

        // outgoing *= (1 + damagePct * monsters)
        float outgoing = 1.0f;
        if (damagePerMonsterPct > 0.0f) {
            outgoing = 1.0f + (damagePerMonsterPct * monsters);
        }

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);

        boolean applied = tag.getBoolean(NBT_APPLIED);
        float lastIncoming = tag.contains(NBT_LAST_INCOMING) ? tag.getFloat(NBT_LAST_INCOMING) : 1.0f;
        float lastOutgoing = tag.contains(NBT_LAST_OUTGOING) ? tag.getFloat(NBT_LAST_OUTGOING) : 1.0f;

        if (applied && lastIncoming == incoming && lastOutgoing == outgoing) return;

        ElementalDamageHandler.setPlayerIncomingMultiplier(player, incoming);
        ElementalDamageHandler.setPlayerOutgoingMultiplier(player, outgoing);

        tag.putBoolean(NBT_APPLIED, true);
        tag.putFloat(NBT_LAST_INCOMING, incoming);
        tag.putFloat(NBT_LAST_OUTGOING, outgoing);
        root.put(NBT_KEY, tag);
    }

    public static int countMonsters(Player player) {
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, box, DuellantCortextEffect::isHostileMonster);
        return mobs == null ? 0 : mobs.size();
    }

    private static boolean isHostileMonster(Entity e) {
        if (!(e instanceof Mob mob)) return false;
        return !mob.getType().getCategory().isFriendly();
    }
}