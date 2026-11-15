package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class WarGraveFeature extends Feature<NoneFeatureConfiguration> {

    public WarGraveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        int radius = 32;
        int numSwords = 6 + random.nextInt(10); // 6 to 15 swords scattered in the area

        int minWritableX = Integer.MIN_VALUE;
        int maxWritableX = Integer.MAX_VALUE;
        int minWritableZ = Integer.MIN_VALUE;
        int maxWritableZ = Integer.MAX_VALUE;
        if (level instanceof WorldGenRegion region) {
            ChunkPos center = region.getCenter();
            minWritableX = (center.x - 1) << 4;
            maxWritableX = ((center.x + 1) << 4) + 15;
            minWritableZ = (center.z - 1) << 4;
            maxWritableZ = ((center.z + 1) << 4) + 15;
        }

        boolean placedAny = false;
        BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < numSwords; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;
            int worldX = origin.getX() + x;
            int worldZ = origin.getZ() + z;
            if (worldX < minWritableX || worldX > maxWritableX || worldZ < minWritableZ || worldZ > maxWritableZ) {
                continue;
            }
            int actualY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, worldX, worldZ);

            placePos.set(worldX, actualY, worldZ);
            belowPos.set(worldX, actualY - 1, worldZ);

            // Must be on sol sand
            if (level.getBlockState(belowPos).is(ModBlocks.SOL_SAND.get()) && 
                (level.isEmptyBlock(placePos) || level.getBlockState(placePos).canBeReplaced())) {
                
                Block swordBlock = random.nextBoolean() ? ModBlocks.BROKEN_SWORD_BLOCK.get() : ModBlocks.TILTED_BROKEN_SWORD_BLOCK.get();
                Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                
                BlockState state = swordBlock.defaultBlockState();
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
                }
                
                level.setBlock(placePos, state, 2);
                placedAny = true;
            }
        }
        
        return placedAny;
    }
}
