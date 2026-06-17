package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class AbyssPillarFeature extends Feature<NoneFeatureConfiguration> {

    private static final ResourceKey<net.minecraft.world.level.biome.Biome> ELYSIAN_ABYSS_KEY =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));

    private static final int MAX_FLOOR_Y = -60;

    public AbyssPillarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private boolean isReplaceableByPillar(BlockState state) {
        return !state.is(Blocks.BEDROCK) && !state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) && !state.is(net.minecraft.tags.BlockTags.DIRT);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rng = context.random();

        if (!level.getBiome(origin).is(ELYSIAN_ABYSS_KEY)) {
            return false;
        }

        // 1. Find the floor
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        boolean foundFloor = false;
        int startY = origin.getY() + 32;
        int endY = origin.getY() - 64;

        for (int y = startY; y >= endY; y--) {
            mPos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(mPos);
            BlockState below = level.getBlockState(mPos.below());

            if (isReplaceableByPillar(state) && !isReplaceableByPillar(below) && !below.is(Blocks.BEDROCK)) {
                origin = mPos.immutable();
                foundFloor = true;
                break;
            }
        }

        if (!foundFloor || origin.getY() > MAX_FLOOR_Y) return false;

        int ceilingY = -1;
        int freeAirRise = 0;
        int maxScanY = Math.min(300, origin.getY() + 250);
        for (int y = origin.getY() + 1; y <= maxScanY; y++) {
            mPos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(mPos);
            if (isReplaceableByPillar(state)) {
                if (ceilingY == -1) freeAirRise++;
            } else {
                if (ceilingY == -1) ceilingY = y;
                break;
            }
        }

        if (ceilingY == -1) {
            return false; // No ceiling found
        }
        if (freeAirRise < 30) {
            return false; // Too short to be a massive pillar (might be a ravine crack)
        }

        int floorY = origin.getY();
        int totalHeight = ceilingY - floorY;

        // 3. Generate the pillar
        int baseRadius = 3 + rng.nextInt(3); // Base radius 3-5 (diameter 7-11)
        int minRadius = Math.max(2, baseRadius - 2);
        
        // Random offsets to make it tilted or twisted
        double tiltX = (rng.nextDouble() - 0.5) * 10.0;
        double tiltZ = (rng.nextDouble() - 0.5) * 10.0;
        double twistStrength = (rng.nextDouble() - 0.5) * 3.0;

        // Generate the core pillar (with 5-block tapered roots)
        for (int y = floorY - 5; y <= ceilingY + 5; y++) {
            double normalizedY = (double) (y - floorY) / totalHeight; // 0.0 at floor, 1.0 at ceiling
            
            double currentRadiusFloat;
            if (y < floorY) {
                // Taper the root below the floor
                double baseR = minRadius + (baseRadius - minRadius) * 1.0; // Math.pow(1.0, 1.5) = 1.0
                double taper = 1.0 - ((floorY - y) / 5.0);
                if (taper <= 0) continue;
                currentRadiusFloat = baseR * taper;
            } else if (y > ceilingY) {
                // Taper the root above the ceiling
                double baseR = minRadius + (baseRadius - minRadius) * 1.0;
                double taper = 1.0 - ((y - ceilingY) / 5.0);
                if (taper <= 0) continue;
                currentRadiusFloat = baseR * taper;
            } else {
                // Hourglass formula (thick at ends, thin in middle)
                double shapeFactor = Math.abs(normalizedY - 0.5) * 2.0; // 1 at ends, 0 at middle
                currentRadiusFloat = minRadius + (baseRadius - minRadius) * Math.pow(shapeFactor, 1.5);
            }

            // Calculate center offset for this layer
            double offsetX = normalizedY * tiltX + Math.sin(normalizedY * Math.PI * 2.0) * twistStrength;
            double offsetZ = normalizedY * tiltZ + Math.cos(normalizedY * Math.PI * 2.0) * twistStrength;

            int centerX = origin.getX() + (int) Math.round(offsetX);
            int centerZ = origin.getZ() + (int) Math.round(offsetZ);

            BlockState material = y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();

            // We iterate a bit wider to accommodate the noise
            int scanRadius = (int) Math.ceil(currentRadiusFloat + 2);

            for (int dx = -scanRadius; dx <= scanRadius; dx++) {
                for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                    int worldX = centerX + dx;
                    int worldZ = centerZ + dz;
                    
                    double distSq = dx * dx + dz * dz;
                    
                    // Smooth organic noise for ridges and bumps
                    double noise = net.ganyusbathwater.oririmod.util.FastNoise.noise3D(
                        (float) worldX * 0.1f, (float) y * 0.1f, (float) worldZ * 0.1f
                    );
                    
                    double radiusWithNoise = currentRadiusFloat + noise * 1.5;
                    
                    if (distSq <= radiusWithNoise * radiusWithNoise) {
                        mPos.set(worldX, y, worldZ);
                        BlockState existing = level.getBlockState(mPos);
                        
                        if (isReplaceableByPillar(existing)) {
                            level.setBlock(mPos, material, 3);
                        }
                    }
                }
            }
        }

        return true;
    }
}
