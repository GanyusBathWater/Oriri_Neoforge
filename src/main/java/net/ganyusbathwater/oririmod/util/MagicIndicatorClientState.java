package net.ganyusbathwater.oririmod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class MagicIndicatorClientState {
    public static final MagicIndicatorClientState INSTANCE = new MagicIndicatorClientState();

    public static final ResourceLocation DEFAULT_TEX =
            ResourceLocation.fromNamespaceAndPath("oririmod", "textures/effect/magic_circles/arcane_outer.png");

    private final Map<Integer, Indicator> active = new HashMap<>();

    private MagicIndicatorClientState() {}

    public static void startFor(LivingEntity entity, Indicator params) {
        if (entity == null || entity.level().isClientSide() == false) return;
        INSTANCE.active.put(entity.getId(), params.withStartTime(entity.level().getGameTime()));
    }

    public static void stopFor(LivingEntity entity) {
        if (entity == null) return;
        INSTANCE.active.remove(entity.getId());
    }

    public Map<Integer, Indicator> getActive() {
        return active;
    }

    public record Indicator(
            ResourceLocation texture,    // Fallback für Einzel‑Kreis
            float radius,                // Fallback
            float distanceForward,       // Basis‑Distanz (für alle Layer)
            int argbColor,               // Fallback
            int durationTicks,           // <= 0 => unendlich
            float spinDegPerTick,        // Fallback
            boolean faceWithLook,        // aktuell nicht umgeschaltet; ausrichtend zur Blickrichtung
            long startGameTime,          // intern
            List<Layer> layers           // Mehrere konzentrische Schichten
    ) {
        public Indicator withStartTime(long gameTime) {
            return new Indicator(texture, radius, distanceForward, argbColor, durationTicks, spinDegPerTick, faceWithLook, gameTime,
                    layers == null ? List.of() : List.copyOf(layers));
        }

        public float progress(long gameTime, float partialTick) {
            if (durationTicks <= 0) return 0f;
            float age = (float)((gameTime - startGameTime)) + partialTick;
            return Math.min(1f, Math.max(0f, age / (float) durationTicks));
        }

        // Eine Schicht/Kreis
        public record Layer(
                ResourceLocation texture,
                float radius,
                float spinDegPerTick,
                int argbColor,
                float extraDistanceForward
        ) {}

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            // Fallback (Einzel‑Kreis)
            private ResourceLocation texture = DEFAULT_TEX;
            private float radius = 1.0f;
            private float distanceForward = 1.5f;
            private int argbColor = 0xFFFFFFFF;
            private int durationTicks = 20;
            private float spinDegPerTick = 6f;
            private boolean faceWithLook = true;

            // Multi‑Layer
            private final List<Layer> layers = new ArrayList<>();

            public Builder texture(ResourceLocation tex) { this.texture = tex; return this; }
            public Builder radius(float r) { this.radius = r; return this; }
            public Builder distance(float d) { this.distanceForward = d; return this; }
            public Builder color(int argb) { this.argbColor = argb; return this; }
            public Builder duration(int ticks) { this.durationTicks = ticks; return this; }
            public Builder spin(float degPerTick) { this.spinDegPerTick = degPerTick; return this; }
            public Builder faceWithLook(boolean v) { this.faceWithLook = v; return this; }

            // Layer‑API
            public Builder addLayer(Layer layer) { if (layer != null) this.layers.add(layer); return this; }
            public Builder clearLayers() { this.layers.clear(); return this; }
            public Builder layers(Collection<Layer> ls) { this.layers.clear(); if (ls != null) this.layers.addAll(ls); return this; }

            public Indicator build() {
                return new Indicator(
                        texture, radius, distanceForward, argbColor, durationTicks,
                        spinDegPerTick, faceWithLook, 0L,
                        List.copyOf(layers)
                );
            }
        }
    }
}