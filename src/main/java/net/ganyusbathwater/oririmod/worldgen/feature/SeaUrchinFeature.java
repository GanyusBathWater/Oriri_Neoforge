package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SeaUrchinFeature extends Feature<NoneFeatureConfiguration> {

    public SeaUrchinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static class Spike {
        Vector3f dir;
        float length;
        float baseWidth;

        Spike(Vector3f dir, float length, float baseWidth) {
            this.dir = dir;
            this.length = length;
            this.baseWidth = baseWidth;
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource rng = context.random();

        // 50/50 Chance between Amethyst and Mana Crystals
        boolean isMana = rng.nextBoolean();
        BlockState crystalBlock = isMana ? ModBlocks.MANA_CRYSTAL_BLOCK.get().defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState clusterBlock = isMana ? ModBlocks.MANA_CRYSTAL_CLUSTER.get().defaultBlockState() : Blocks.AMETHYST_CLUSTER.defaultBlockState();

        // Ensure we are placing it inside solid rock
        if (!level.getBlockState(origin).isSolid()) {
            boolean foundSolid = false;
            for (int i = 0; i < 5; i++) {
                origin = origin.below();
                if (level.getBlockState(origin).isSolid()) {
                    foundSolid = true;
                    break;
                }
            }
            if (!foundSolid) return false;
        }

        int coreRadius = rng.nextInt(3) + 4; // 4 to 6 blocks core
        int numSpikes = 15 + rng.nextInt(20); // 15 to 35 spikes
        
        List<Spike> spikes = new ArrayList<>();
        for (int i = 0; i < numSpikes; i++) {
            float theta = rng.nextFloat() * 2.0f * (float) Math.PI;
            float phi = (float) Math.acos(2.0f * rng.nextFloat() - 1.0f);
            
            float dx = (float) (Math.sin(phi) * Math.cos(theta));
            float dy = (float) (Math.cos(phi));
            float dz = (float) (Math.sin(phi) * Math.sin(theta));
            
            Vector3f direction = new Vector3f(dx, dy, dz).normalize();
            
            float length = 10.0f + rng.nextFloat() * 12.0f; // 10 to 22 blocks long
            float width = 3.0f + rng.nextFloat() * 2.5f; // 3 to 5.5 blocks wide base
            
            spikes.add(new Spike(direction, length, width));
        }

        int maxBounding = 24; 
        float noiseScale = 0.15f;
        
        // Phase 1: Carve volume and place solid crystals
        for (int x = -maxBounding; x <= maxBounding; x++) {
            for (int y = -maxBounding; y <= maxBounding; y++) {
                for (int z = -maxBounding; z <= maxBounding; z++) {
                    double distSq = x*x + y*y + z*z;
                    double dist = Math.sqrt(distSq);
                    
                    if (dist > maxBounding) continue;
                    
                    BlockPos pos = origin.offset(x, y, z);
                    
                    // 3D Noise for organic distortion
                    float nx = (origin.getX() + x) * noiseScale;
                    float ny = (origin.getY() + y) * noiseScale;
                    float nz = (origin.getZ() + z) * noiseScale;
                    
                    double noise = net.ganyusbathwater.oririmod.util.FastNoise.fbm3D(nx, ny, nz, 2);
                    
                    boolean isInside = false;
                    
                    // Check core
                    double distortedCoreRadius = coreRadius + (noise * 2.0); // Bumpy core
                    if (dist < distortedCoreRadius) {
                        isInside = true;
                    } else {
                        // Check spikes
                        for (Spike s : spikes) {
                            double dot = (x * s.dir.x() + y * s.dir.y() + z * s.dir.z());
                            if (dot > 0 && dot < s.length) {
                                double distToAxis = Math.sqrt(distSq - dot*dot);
                                
                                // Linear taper from base to tip
                                double taper = 1.0 - (dot / s.length);
                                double currentThickness = s.baseWidth * taper;
                                
                                // Noise distorts the surface of the spikes
                                double distortedThickness = currentThickness + (noise * 1.8 * taper);
                                
                                if (distToAxis < distortedThickness) {
                                    isInside = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (isInside) {
                        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
                        if (pos.getY() > surfaceY - 5) {
                            continue;
                        }
                        
                        BlockState currentState = level.getBlockState(pos);
                        if (currentState.isAir() || currentState.is(Blocks.WATER) || currentState.is(Blocks.LAVA) || !currentState.isSolid()) {
                            continue;
                        }

                        // Geode Crust
                        if (dist > distortedCoreRadius - 1.5 && dist < distortedCoreRadius + 0.5) {
                            if (rng.nextFloat() < 0.2f) {
                                level.setBlock(pos, Blocks.CALCITE.defaultBlockState(), 2);
                            } else if (rng.nextFloat() < 0.1f) {
                                level.setBlock(pos, Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
                            } else {
                                level.setBlock(pos, crystalBlock, 2);
                            }
                        } else {
                            level.setBlock(pos, crystalBlock, 2);
                        }
                    }
                }
            }
        }
        
        // Phase 2: Place clusters on the surface for texture
        for (int x = -maxBounding; x <= maxBounding; x++) {
            for (int y = -maxBounding; y <= maxBounding; y++) {
                for (int z = -maxBounding; z <= maxBounding; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    
                    if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
                        Direction attachedFace = null;
                        for (Direction dir : Direction.values()) {
                            BlockPos neighbor = pos.relative(dir);
                            BlockState neighborState = level.getBlockState(neighbor);
                            if (neighborState.is(crystalBlock.getBlock())) {
                                attachedFace = dir;
                                break;
                            }
                        }
                        
                        if (attachedFace != null && rng.nextFloat() < 0.04f) {
                            level.setBlock(pos, clusterBlock.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, attachedFace.getOpposite()), 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
