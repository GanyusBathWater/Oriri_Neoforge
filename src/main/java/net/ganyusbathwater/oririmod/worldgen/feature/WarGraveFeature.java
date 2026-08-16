package net.ganyusbathwater.oririmod.worldgen.feature;

import com.mojang.serialization.Codec;
import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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

        boolean placedAny = false;

        for (int i = 0; i < numSwords; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;
            
            BlockPos targetPos = origin.offset(x, 0, z);
            
            // Find surface at target x,z
            int actualY = targetPos.getY();
            // Go down to find the highest solid block
            while (level.isEmptyBlock(new BlockPos(targetPos.getX(), actualY - 1, targetPos.getZ())) && actualY > context.chunkGenerator().getMinY()) {
                actualY--;
            }
            // Go up if we are inside solid ground
            while (!level.isEmptyBlock(new BlockPos(targetPos.getX(), actualY, targetPos.getZ())) && actualY < context.chunkGenerator().getGenDepth()) {
                actualY++;
            }
            
            BlockPos placePos = new BlockPos(targetPos.getX(), actualY, targetPos.getZ());
            
            // Must be on sol sand
            if (level.getBlockState(placePos.below()).is(ModBlocks.SOL_SAND.get()) && 
                (level.isEmptyBlock(placePos) || level.getBlockState(placePos).canBeReplaced())) {
                
                Block swordBlock = random.nextBoolean() ? ModBlocks.BROKEN_SWORD_BLOCK.get() : ModBlocks.TILTED_BROKEN_SWORD_BLOCK.get();
                Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                
                BlockState state = swordBlock.defaultBlockState();
                if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
                }
                
                setBlock(level, placePos, state);
                placedAny = true;
            }
        }
        
        return placedAny;
    }
}
