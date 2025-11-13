// java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VestigeEffects {
    private VestigeEffects() {}

    // Flackerfreier, „NV‑aware“ Effektgeber
    public static VestigeEffect mobEffect(Holder<MobEffect> effect, int amplifier, int durationTicks) {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int level) {
                int duration = durationTicks;
                int refreshThreshold = durationTicks / 2;

                if (effect == MobEffects.NIGHT_VISION) {
                    // Mindestens 600 Ticks und vor der 200‑Tick‑Flackerzone erneuern
                    duration = Math.max(durationTicks, 600);
                    refreshThreshold = Math.max(220, duration - 120);
                }

                var existing = player.getEffect(effect);
                if (existing == null
                        || existing.getAmplifier() < amplifier
                        || existing.getDuration() <= refreshThreshold) {
                    player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
                }
            }
        };
    }

    public static VestigeEffect keepInventory() {
        return new VestigeEffect() {
            @Override
            public boolean keepInventoryOnDeath(ServerPlayer player, ItemStack stack, int level) {
                return true;
            }
        };
    }

    public static VestigeEffect stepHeight(float bonus) {
        return new VestigeEffect() {
            @Override
            public float stepHeightBonus(ServerPlayer player, ItemStack stack, int level) {
                return bonus;
            }
        };
    }

    // Erz-Partikel: gezielt an den Spieler senden, etwas dichter
    public static VestigeEffect oreSense(int radius) {
        return new VestigeEffect() {
            // Einfacher Cache pro Spieler (nur während Serverlaufzeit)
            private final Map<UUID, Long> lastScanTime = new HashMap<>();
            private final Map<UUID, BlockPos> lastCenter = new HashMap<>();

            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                ServerLevel level = player.serverLevel();
                long gt = level.getGameTime();
                int r = Math.max(1, radius);
                var center = player.blockPosition();

                // Intervall oder Positionswechsel (Block)
                boolean moved = !center.equals(lastCenter.get(player.getUUID()));
                long prev = lastScanTime.getOrDefault(player.getUUID(), -999L);
                int interval = 40; // alle 2 Sekunden

                if (!moved && (gt - prev) < interval) return;

                lastScanTime.put(player.getUUID(), gt);
                lastCenter.put(player.getUUID(), center);

                int matched = 0;

                // Vollscan (Cube) – kann später in Scheiben aufgeteilt werden
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            var pos = center.offset(dx, dy, dz);
                            var state = level.getBlockState(pos);
                            if (state.isAir() || !state.is(ModTags.Blocks.ORES)) continue;
                            matched++;
                            // Partikel über dem Erz (sichtbar), leichte Streuung
                            double px = pos.getX() + 0.5;
                            double py = pos.getY() + 1.1; // über Block
                            double pz = pos.getZ() + 0.5;
                            level.sendParticles(player, ParticleTypes.END_ROD, true,
                                    px, py, pz,
                                    4,
                                    0.25, 0.15, 0.25,
                                    0.002);
                        }
                    }
                }

                if (matched == 0) {
                    // Optional dezentes Debug (Logger bevorzugen)
                    // OririMod.LOGGER.debug("oreSense: keine Erze im Radius {} um {}", r, center);
                }
            }
        };
    }

    // MobSense: Effekt robust setzen (alle 10 Ticks, 200 Ticks Dauer)
    public static VestigeEffect mobSense(int radius) {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                if (player.tickCount % 10 != 0) return;
                ServerLevel level = player.serverLevel();
                int r = Math.max(1, radius);
                AABB box = player.getBoundingBox().inflate(r);
                for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, false));
                }
            }
        };
    }
}