package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.HashSet;
import java.util.Set;

public class AbyssSpikeFeature extends Feature<AbyssSpikeFeature.AbyssSpikeConfig> {

    private static final ResourceKey<net.minecraft.world.level.biome.Biome> ELYSIAN_ABYSS_KEY =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "elysian_abyss"));

    public AbyssSpikeFeature(Codec<AbyssSpikeConfig> codec) {
        super(codec);
    }

    private boolean isReplaceableBySpike(BlockState state) {
        // Allow replacing stone/deepslate so the root of the spike can anchor into the ceiling!
        return !state.is(Blocks.BEDROCK) && !state.is(net.minecraft.tags.BlockTags.DIRT)
                && !state.is(ModBlocks.HARDENED_MANASHROOM.get())
                && !state.is(ModBlocks.AETHER_MAGMA_BLOCK.get())
                && !state.is(net.ganyusbathwater.oririmod.fluid.ModFluids.AETHER_BLOCK.get());
    }

    @Override
    public boolean place(FeaturePlaceContext<AbyssSpikeConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rng = context.random();
        AbyssSpikeConfig config = context.config();

        if (!level.getBiome(origin).is(ELYSIAN_ABYSS_KEY)) {
            return false;
        }

        // Search for floor/ceiling to anchor to
        boolean foundAnchor = false;
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        
        int startY = origin.getY();
        // Scan up to 16 blocks to find the true surface
        for (int y = startY; Math.abs(y - startY) < 16; y += (config.isCeiling() ? 1 : -1)) {
            mPos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(mPos);
            BlockState next = level.getBlockState(mPos.relative(config.isCeiling() ? Direction.UP : Direction.DOWN));

            if ((state.isAir() || state.is(Blocks.WATER)) && !next.isAir() && !next.is(Blocks.WATER) && !next.is(Blocks.BEDROCK)
                && !next.is(ModBlocks.HARDENED_MANASHROOM.get()) && !next.is(ModBlocks.AETHER_MAGMA_BLOCK.get())) {
                origin = mPos.immutable();
                foundAnchor = true;
                break;
            }
        }

        if (!foundAnchor) return false;

        // Prevent overlapping: check if there's already a spike or obstacle immediately in front of the anchor
        for (int i = 1; i <= 3; i++) {
            BlockPos checkPos = origin.relative(config.isCeiling() ? Direction.DOWN : Direction.UP, i);
            BlockState checkState = level.getBlockState(checkPos);
            if (!checkState.isAir() && !checkState.is(Blocks.WATER) && !checkState.canBeReplaced()) {
                return false; // Area is occupied, abort to prevent overlapping spikes
            }
        }

        // Dimensions
        int height = 4 + rng.nextInt(13); // 4 to 16 blocks tall
        
        // Scale base radius by height (taller spikes = thicker bases)
        double maxRadius = (height * 0.15) + 1.5; // 4 height -> 2.1; 16 height -> 3.9
        double baseRadius = maxRadius * (0.8 + rng.nextDouble() * 0.4);

        // Keep tilt small relative to the base radius to prevent diagonal stair-stepping disconnects
        double tiltScale = baseRadius * 0.5; 
        double tiltX = (rng.nextDouble() - 0.5) * 2.0 * tiltScale;
        double tiltZ = (rng.nextDouble() - 0.5) * 2.0 * tiltScale;
        
        // Rare chance for the spike tip to be made of ores!
        boolean isOreSpike = rng.nextDouble() < 0.08; // 8% chance
        
        BlockState oreDeepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState oreStone = Blocks.STONE.defaultBlockState();
        
        if (isOreSpike) {
            int oreType = rng.nextInt(10);
            switch (oreType) {
                case 0 -> { oreDeepslate = ModBlocks.DEEPSLATE_JADE_ORE.get().defaultBlockState(); oreStone = ModBlocks.JADE_ORE.get().defaultBlockState(); }
                case 1 -> { oreDeepslate = ModBlocks.DEEPSLATE_DRAGON_IRON_ORE.get().defaultBlockState(); oreStone = ModBlocks.DRAGON_IRON_ORE.get().defaultBlockState(); }
                case 2 -> { oreDeepslate = Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(); oreStone = Blocks.IRON_ORE.defaultBlockState(); }
                case 3 -> { oreDeepslate = Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(); oreStone = Blocks.GOLD_ORE.defaultBlockState(); }
                case 4 -> { oreDeepslate = Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(); oreStone = Blocks.DIAMOND_ORE.defaultBlockState(); }
                case 5 -> { oreDeepslate = Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(); oreStone = Blocks.REDSTONE_ORE.defaultBlockState(); }
                case 6 -> { oreDeepslate = Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState(); oreStone = Blocks.LAPIS_ORE.defaultBlockState(); }
                case 7 -> { oreDeepslate = Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(); oreStone = Blocks.COAL_ORE.defaultBlockState(); }
                case 8 -> { oreDeepslate = Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState(); oreStone = Blocks.COPPER_ORE.defaultBlockState(); }
                case 9 -> { oreDeepslate = Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState(); oreStone = Blocks.EMERALD_ORE.defaultBlockState(); }
            }
        }

        BlockState deepslateBlock = Blocks.DEEPSLATE.defaultBlockState();
        BlockState stoneBlock = Blocks.STONE.defaultBlockState();

        int directionModifier = config.isCeiling() ? -1 : 1;
        int boundingRadius = (int) Math.ceil(baseRadius) + 2;
        int ridges = 3 + rng.nextInt(3); // 3 to 5 ridges per spike for organic fluting

        // 3. Generate the spike (including a 5-block root in the opposite direction)
        for (int i = -5; i < height; i++) {
            int y = origin.getY() + (i * directionModifier);

            double progress;
            double currentRadius;
            
            if (i < 0) {
                // Taper the root over 5 blocks
                progress = 0.0;
                double baseR = baseRadius;
                double taper = 1.0 - (Math.abs(i) / 5.0); // tapers to 0 at i = -5
                currentRadius = baseR * Math.max(0.0, taper);
            } else {
                progress = (double) i / height;
                currentRadius = baseRadius * (1.0 - Math.pow(progress, 1.5));
            }

            // Continuous center coordinates for smooth translation
            double cx = tiltX * progress;
            double cz = tiltZ * progress;

            for (int x = -boundingRadius; x <= boundingRadius; x++) {
                for (int z = -boundingRadius; z <= boundingRadius; z++) {
                    double dx = x - cx;
                    double dz = z - cz;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    
                    // Elegant angle-based ridging (2D noise) instead of chaotic 3D noise
                    double angle = Math.atan2(dz, dx);
                    double noise = Math.sin(angle * ridges) * 0.2 * currentRadius;

                    if (dist + noise <= currentRadius) {
                        int worldX = origin.getX() + x;
                        int worldZ = origin.getZ() + z;
                        BlockPos targetPos = new BlockPos(worldX, y, worldZ);
                        
                        BlockState existing = level.getBlockState(targetPos);
                        if (isReplaceableBySpike(existing)) {
                            // If it's an ore spike, the tip (progress >= 0.6) is made of ore
                            boolean useOre = isOreSpike && progress >= 0.6;
                            BlockState baseDeepslate = useOre ? oreDeepslate : deepslateBlock;
                            BlockState baseStone = useOre ? oreStone : stoneBlock;
                            
                            // Deepslate under Y=0, Stone above Y=0
                            BlockState toPlace = (y < 0) ? baseDeepslate : baseStone;
                            
                            // Blend zone between Y=-3 and Y=3
                            if (y >= -3 && y <= 3) {
                                double deepslateChance = (3.0 - y) / 6.0; // 1.0 at -3, 0.0 at 3
                                if (rng.nextDouble() > deepslateChance) {
                                    toPlace = baseStone;
                                } else {
                                    toPlace = baseDeepslate;
                                }
                            }
                            
                            
                            level.setBlock(targetPos, toPlace, 3);
                        }
                    }
                }
            }
        }

        return true;
    }

    public record AbyssSpikeConfig(boolean isCeiling) implements FeatureConfiguration {
        public static final Codec<AbyssSpikeConfig> CODEC = RecordCodecBuilder.create((builder) -> {
            return builder.group(Codec.BOOL.fieldOf("is_ceiling").forGetter(AbyssSpikeConfig::isCeiling)).apply(builder, AbyssSpikeConfig::new);
        });
    }
}
