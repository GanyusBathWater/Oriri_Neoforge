package net.ganyusbathwater.oririmod.worldgen.processor;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.worldgen.ModStructureProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public class LootTableChestProcessor extends StructureProcessor {
    public static final LootTableChestProcessor INSTANCE = new LootTableChestProcessor();
    public static final MapCodec<LootTableChestProcessor> CODEC = MapCodec.unit(INSTANCE);

    private LootTableChestProcessor() {}

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo blockInfoLocal, StructureTemplate.StructureBlockInfo blockInfoGlobal, StructurePlaceSettings settings) {
        BlockState state = blockInfoGlobal.state();
        ResourceLocation lootTable = null;

        if (state.is(Blocks.MAGENTA_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost/storage");
        } else if (state.is(Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost/training");
        } else if (state.is(Blocks.YELLOW_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost/kitchen");
        } else if (state.is(Blocks.LIME_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost/library");
        } else if (state.is(Blocks.PINK_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost_ruins/storage");
        } else if (state.is(Blocks.GRAY_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost_ruins/training");
        } else if (state.is(Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost_ruins/kitchen");
        } else if (state.is(Blocks.CYAN_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost_ruins/library");
        } else if (state.is(Blocks.PURPLE_GLAZED_TERRACOTTA)) {
            lootTable = ResourceLocation.parse("oririmod:chests/outpost_ruins/scarlet_storage");
        }

        if (lootTable != null) {
            Direction facing = Direction.NORTH;
            if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                facing = state.getValue(HorizontalDirectionalBlock.FACING);
            }
            
            BlockState newChestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
            
            CompoundTag nbt = new CompoundTag();
            nbt.putString("id", "minecraft:chest");
            nbt.putString("LootTable", lootTable.toString());

            return new StructureTemplate.StructureBlockInfo(blockInfoGlobal.pos(), newChestState, nbt);
        }

        return blockInfoGlobal;
    }

    @Override
    public java.util.List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            net.minecraft.world.level.ServerLevelAccessor level, 
            BlockPos offset, 
            BlockPos pos, 
            java.util.List<StructureTemplate.StructureBlockInfo> localInfos, 
            java.util.List<StructureTemplate.StructureBlockInfo> globalInfos, 
            StructurePlaceSettings settings) {
        
        java.util.List<StructureTemplate.StructureBlockInfo> modified = new java.util.ArrayList<>();
        
        for (StructureTemplate.StructureBlockInfo info : globalInfos) {
            if (info.state().is(Blocks.CHEST) && info.state().getValue(ChestBlock.TYPE) == net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                Direction facing = info.state().getValue(ChestBlock.FACING);
                
                StructureTemplate.StructureBlockInfo adjacentChest = null;
                Direction adjacentDir = null;
                
                for (StructureTemplate.StructureBlockInfo other : globalInfos) {
                    if (other != info && other.state().is(Blocks.CHEST) && other.state().getValue(ChestBlock.FACING) == facing) {
                        int dx = other.pos().getX() - info.pos().getX();
                        int dz = other.pos().getZ() - info.pos().getZ();
                        if (Math.abs(dx) + Math.abs(dz) == 1 && other.pos().getY() == info.pos().getY()) {
                            adjacentChest = other;
                            if (dx == 1) adjacentDir = Direction.EAST;
                            else if (dx == -1) adjacentDir = Direction.WEST;
                            else if (dz == 1) adjacentDir = Direction.SOUTH;
                            else if (dz == -1) adjacentDir = Direction.NORTH;
                            break;
                        }
                    }
                }
                
                if (adjacentDir != null) {
                    Direction leftDir = facing.getClockWise();
                    net.minecraft.world.level.block.state.properties.ChestType type = 
                        (adjacentDir == leftDir) ? net.minecraft.world.level.block.state.properties.ChestType.LEFT : net.minecraft.world.level.block.state.properties.ChestType.RIGHT;
                    
                    BlockState newState = info.state().setValue(ChestBlock.TYPE, type);
                    modified.add(new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt()));
                    continue;
                }
            }
            modified.add(info);
        }
        
        return modified;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessors.LOOT_CHEST_PROCESSOR.get();
    }
}
