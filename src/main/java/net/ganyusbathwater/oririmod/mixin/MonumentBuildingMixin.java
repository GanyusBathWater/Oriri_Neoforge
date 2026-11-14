package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(OceanMonumentPieces.MonumentBuilding.class)
public abstract class MonumentBuildingMixin implements OceanMonumentPlacementHelper.ShiftableMonument {

    @Shadow
    @Final
    private List<? extends StructurePiece> childPieces;

    @Unique
    private Boolean oririmod$prevActiveState;

    @Override
    public void oririmod$shiftVertical(int dy) {
        if (dy == 0) {
            return;
        }
        ((OceanMonumentPieces.MonumentBuilding) (Object) this).move(0, dy, 0);
        for (StructurePiece child : this.childPieces) {
            child.move(0, dy, 0);
        }
    }

    @Inject(method = "postProcess", at = @At("HEAD"))
    private void oririmod$beforePostProcess(WorldGenLevel level, StructureManager structureManager,
            ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos,
            CallbackInfo ci) {
        oririmod$prevActiveState = OceanMonumentPlacementHelper.ELDERWOODS_ACTIVE.get();
        boolean isElderwoods = generator instanceof ElderwoodsChunkGenerator;
        OceanMonumentPlacementHelper.ELDERWOODS_ACTIVE.set(isElderwoods);

        if (isElderwoods) {
            // Pre-clear only the enclosed interior room chambers instead of carving a 68x68 seabed crater
            for (StructurePiece child : this.childPieces) {
                if (!OceanMonumentPlacementHelper.isExcludedFromPreClear(child)) {
                    OceanMonumentPlacementHelper.clearRoomInterior(level, box, child.getBoundingBox());
                }
            }
        }
    }

    @Inject(method = "postProcess", at = @At("RETURN"))
    private void oririmod$afterPostProcess(WorldGenLevel level, StructureManager structureManager,
            ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos,
            CallbackInfo ci) {
        if (generator instanceof ElderwoodsChunkGenerator) {
            BoundingBox monumentBox = ((OceanMonumentPieces.MonumentBuilding) (Object) this).getBoundingBox();
            OceanMonumentPlacementHelper.cleanupOrphanedAquaticPlants(level, box, monumentBox);
        }
        OceanMonumentPlacementHelper.ELDERWOODS_ACTIVE.set(
                oririmod$prevActiveState != null ? oririmod$prevActiveState : Boolean.FALSE);
        oririmod$prevActiveState = null;
    }
}
