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
            Direction globalFacing = Direction.NORTH;
            if (blockInfoGlobal.state().hasProperty(HorizontalDirectionalBlock.FACING)) {
                globalFacing = blockInfoGlobal.state().getValue(HorizontalDirectionalBlock.FACING);
            }
            
            BlockState newChestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, globalFacing);
            
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
        java.util.Set<BlockPos> processed = new java.util.HashSet<>();
        
        for (StructureTemplate.StructureBlockInfo info : globalInfos) {
            if (processed.contains(info.pos())) continue;
            
            if (info.state().is(Blocks.CHEST) && info.state().getValue(ChestBlock.TYPE) == net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                Direction facing = info.state().getValue(ChestBlock.FACING);
                
                StructureTemplate.StructureBlockInfo partner = null;
                
                for (StructureTemplate.StructureBlockInfo other : globalInfos) {
                    if (other != info && !processed.contains(other.pos()) && other.state().is(Blocks.CHEST) && other.state().getValue(ChestBlock.FACING) == facing) {
                        int dx = Math.abs(other.pos().getX() - info.pos().getX());
                        int dz = Math.abs(other.pos().getZ() - info.pos().getZ());
                        if (dx + dz == 1 && other.pos().getY() == info.pos().getY()) {
                            partner = other;
                            break;
                        }
                    }
                }
                
                if (partner != null) {
                    Direction adjacentDir = null;
                    if (info.pos().getX() < partner.pos().getX()) adjacentDir = Direction.EAST;
                    else if (info.pos().getX() > partner.pos().getX()) adjacentDir = Direction.WEST;
                    else if (info.pos().getZ() < partner.pos().getZ()) adjacentDir = Direction.SOUTH;
                    else if (info.pos().getZ() > partner.pos().getZ()) adjacentDir = Direction.NORTH;
                    
                    Direction correctedFacing = facing;
                    // Vanilla double chests CANNOT be front-to-back. The facing must be perpendicular to the axis they are placed on.
                    if (adjacentDir != null && adjacentDir.getAxis() == facing.getAxis()) {
                        correctedFacing = facing.getClockWise();
                        System.out.println("[ChestProcessor] Auto-correcting impossible front-to-back chest from " + facing + " to " + correctedFacing);
                    }

                    net.minecraft.world.level.block.state.properties.ChestType infoType = net.minecraft.world.level.block.state.properties.ChestType.SINGLE;
                    net.minecraft.world.level.block.state.properties.ChestType partnerType = net.minecraft.world.level.block.state.properties.ChestType.SINGLE;
                    
                    if (adjacentDir != null) {
                        if (adjacentDir == correctedFacing.getClockWise()) {
                            infoType = net.minecraft.world.level.block.state.properties.ChestType.LEFT;
                            partnerType = net.minecraft.world.level.block.state.properties.ChestType.RIGHT;
                        } else {
                            infoType = net.minecraft.world.level.block.state.properties.ChestType.RIGHT;
                            partnerType = net.minecraft.world.level.block.state.properties.ChestType.LEFT;
                        }
                    }
                    
                    // Log out exactly what is calculated so we can cross-reference with world geometry
                    System.out.println("[ChestProcessor] Pair: " + info.pos() + " & " + partner.pos() + " | Final Facing: " + correctedFacing + " | AdjDir: " + adjacentDir + " | Types: " + infoType + ", " + partnerType);
                    
                    BlockState infoState = info.state().setValue(ChestBlock.TYPE, infoType).setValue(ChestBlock.FACING, correctedFacing);
                    BlockState partnerState = partner.state().setValue(ChestBlock.TYPE, partnerType).setValue(ChestBlock.FACING, correctedFacing);
                    
                    if (infoType == net.minecraft.world.level.block.state.properties.ChestType.LEFT) {
                        modified.add(new StructureTemplate.StructureBlockInfo(info.pos(), infoState, info.nbt()));
                        modified.add(new StructureTemplate.StructureBlockInfo(partner.pos(), partnerState, partner.nbt()));
                    } else {
                        modified.add(new StructureTemplate.StructureBlockInfo(partner.pos(), partnerState, partner.nbt()));
                        modified.add(new StructureTemplate.StructureBlockInfo(info.pos(), infoState, info.nbt()));
                    }
                    
                    processed.add(info.pos());
                    processed.add(partner.pos());
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
