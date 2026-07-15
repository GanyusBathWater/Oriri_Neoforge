package net.ganyusbathwater.oririmod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class MagicIndicatorClientState {
    public static final MagicIndicatorClientState INSTANCE = new MagicIndicatorClientState();

    public static final ResourceLocation DEFAULT_TEX =
            ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_outer.png");

    private final Map<Integer, Indicator> active = new HashMap<>();

    private MagicIndicatorClientState() {}

    public static void startFor(LivingEntity entity, Indicator params) {
        if (entity == null || entity.level().isClientSide() == false) return;
        // Wichtig: vorhandene startGameTime beibehalten, damit die Animation nicht pro Tick neu startet
        Indicator prev = INSTANCE.active.get(entity.getId());
        long start = prev != null ? prev.startGameTime() : entity.level().getGameTime();
        INSTANCE.active.put(entity.getId(), params.withStartTime(start));
    }

    public static void stopFor(LivingEntity entity) {
        if (entity == null) return;
        INSTANCE.active.remove(entity.getId());
    }

    public static void spawnChargingParticles(net.minecraft.world.level.Level level, LivingEntity living) {
        if (!level.isClientSide) return;
        
        long time = level.getGameTime();
        Vec3 center = living.position().add(0, living.getBbHeight() * 0.5, 0);
        
        net.minecraft.core.particles.ParticleOptions particleType = net.minecraft.core.particles.ParticleTypes.WITCH;
        net.minecraft.world.item.ItemStack useItem = living.getUseItem();
        if (useItem != null && !useItem.isEmpty()) {
            net.ganyusbathwater.oririmod.combat.Element element = net.ganyusbathwater.oririmod.combat.ItemElementRegistry.getElement(useItem);
            particleType = switch (element) {
                case FIRE -> net.minecraft.core.particles.ParticleTypes.FLAME;
                case WATER -> net.minecraft.core.particles.ParticleTypes.SPLASH;
                case NATURE -> net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER;
                case LIGHT -> net.minecraft.core.particles.ParticleTypes.END_ROD;
                case DARKNESS -> net.minecraft.core.particles.ParticleTypes.SMOKE;
                case EARTH -> net.minecraft.core.particles.ParticleTypes.ASH;
                case PHYSICAL -> net.minecraft.core.particles.ParticleTypes.CRIT;
                default -> net.minecraft.core.particles.ParticleTypes.WITCH;
            };
        }
        
        // Orbital swirl
        float angle = (time % 40) * (360f / 40f);
        float rad = (float) Math.toRadians(angle);
        double radius = 1.0;
        double ox = Math.cos(rad) * radius;
        double oz = Math.sin(rad) * radius;
        double oy = Math.sin((time % 40) / 40.0 * Math.PI * 2) * 0.5;
        
        level.addParticle(particleType,
                center.x + ox, center.y + oy, center.z + oz,
                0, 0.05, 0);

        // Converging energy (spawn around and move inwards)
        for (int i = 0; i < 2; i++) {
            double cx = center.x + (level.random.nextDouble() - 0.5) * 4.0;
            double cy = center.y + (level.random.nextDouble() - 0.5) * 4.0;
            double cz = center.z + (level.random.nextDouble() - 0.5) * 4.0;
            Vec3 dir = center.subtract(cx, cy, cz).normalize().scale(0.15);
            level.addParticle(particleType,
                    cx, cy, cz, dir.x, dir.y, dir.z);
        }
    }

    public Map<Integer, Indicator> getActive() {
        return active;
    }

    public enum Anchor { PLAYER, WORLD }

    public record Indicator(
            ResourceLocation texture,
            float radius,
            float distanceForward,
            int argbColor,
            int durationTicks,
            float spinDegPerTick,
            boolean faceWithLook,
            long startGameTime,
            List<Layer> layers,
            Vec3 worldAnchor,
            boolean persistentUntilMeteorImpact
    ) {
        public Indicator withStartTime(long gameTime) {
            return new Indicator(
                    texture, radius, distanceForward, argbColor, durationTicks, spinDegPerTick, faceWithLook,
                    gameTime,
                    layers == null ? List.of() : List.copyOf(layers),
                    worldAnchor,
                    persistentUntilMeteorImpact
            );
        }

        public float progress(long gameTime, float partialTick) {
            if (durationTicks <= 0) return 0f;
            float age = (float)((gameTime - startGameTime)) + partialTick;
            return Math.min(1f, Math.max(0f, age / (float) durationTicks));
        }

        public record Layer(
                ResourceLocation texture,
                float radius,
                float spinDegPerTick,
                int argbColor,
                float extraDistanceForward,
                Anchor anchor
        ) {}

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private ResourceLocation texture = DEFAULT_TEX;
            private float radius = 1.0f;
            private float distanceForward = 1.5f;
            private int argbColor = 0xFFFFFFFF;
            private int durationTicks = 20;
            private float spinDegPerTick = 6f;
            private boolean faceWithLook = true;

            private final List<Layer> layers = new ArrayList<>();
            private Vec3 worldAnchor = null;
            private boolean persistentUntilMeteorImpact = false;

            public Builder texture(ResourceLocation tex) { this.texture = tex; return this; }
            public Builder radius(float r) { this.radius = r; return this; }
            public Builder distance(float d) { this.distanceForward = d; return this; }
            public Builder color(int argb) { this.argbColor = argb; return this; }
            public Builder duration(int ticks) { this.durationTicks = ticks; return this; }
            public Builder spin(float degPerTick) { this.spinDegPerTick = degPerTick; return this; }
            public Builder faceWithLook(boolean v) { this.faceWithLook = v; return this; }

            public Builder worldAnchor(Vec3 pos) { this.worldAnchor = pos; return this; }
            public Builder persistentUntilMeteorImpact(boolean v) { this.persistentUntilMeteorImpact = v; return this; }

            public Builder addLayer(Layer layer) { if (layer != null) this.layers.add(layer); return this; }
            public Builder clearLayers() { this.layers.clear(); return this; }
            public Builder layers(Collection<Layer> ls) { this.layers.clear(); if (ls != null) this.layers.addAll(ls); return this; }

            public Indicator build() {
                return new Indicator(
                        texture, radius, distanceForward, argbColor, durationTicks,
                        spinDegPerTick, faceWithLook, 0L,
                        List.copyOf(layers),
                        worldAnchor,
                        persistentUntilMeteorImpact
                );
            }
        }
    }
}