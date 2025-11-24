package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
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
    public static final Map<UUID, Integer> RELIC_COOLDOWN = new HashMap<>();

    // Eindeutiger Key für Relic-of-the-Past-Cooldown (für HUD / Manager)
    public static final ResourceLocation KEY_RELIC =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "relic_of_the_past");

    private VestigeEffects() {}

    public static VestigeEffect relicOfThePastReactive() {
        return new VestigeEffect() {
            private static final int BUFF_DURATION = 20 * 5;
            private static final int COOLDOWN      = 20 * 10; // in Ticks

            @Override
            public void onDamaged(ServerPlayer player, ItemStack stack, int level, float amount) {
                if (amount <= 0.0F) return;

                int maxLevel = 1;
                if (stack.getItem() instanceof VestigeItem vestigeItem) {
                    maxLevel = vestigeItem.getUnlockedLevel(stack);
                }

                // zentralen Manager verwenden
                boolean consumed = VestigeCooldownManager.consume(player, KEY_RELIC, maxLevel, COOLDOWN, false);
                if (!consumed) return;

                if (maxLevel >= 1) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.ABSORPTION, BUFF_DURATION, 0, true, true, true
                    ));
                }
                if (maxLevel >= 2) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_BOOST, BUFF_DURATION, 0, true, true, true
                    ));
                }
                if (maxLevel >= 3) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION, BUFF_DURATION, 0, true, true, true
                    ));
                }
            }
        };
    }

    public static VestigeEffect mobEffect(Holder<MobEffect> effect, int amplifier, int durationTicks) {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int level) {
                int duration = durationTicks;
                int refreshThreshold = durationTicks / 2;

                if (effect == MobEffects.NIGHT_VISION) {
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

    public static VestigeEffect Luck(float bonus) {
        return new VestigeEffect() {
            @Override
            public float LuckBonus(ServerPlayer player, ItemStack stack, int level) {
                return bonus;
            }
        };
    }

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