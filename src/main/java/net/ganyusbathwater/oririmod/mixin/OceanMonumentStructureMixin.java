package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(OceanMonumentStructure.class)
public abstract class OceanMonumentStructureMixin {

    @Shadow
    private static StructurePiece createTopPiece(ChunkPos chunkPos, WorldgenRandom random) {
        throw new AssertionError();
    }

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void oririmod$abortUnsafeMonumentPlacement(Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        if (cir.getReturnValue().isPresent()
                && context.chunkGenerator() instanceof ElderwoodsChunkGenerator
                && OceanMonumentPlacementHelper.evaluateSafeMinY(context, context.chunkPos()).isEmpty()) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Redirect(
            method = "generatePieces",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/structures/OceanMonumentStructure;createTopPiece(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/WorldgenRandom;)Lnet/minecraft/world/level/levelgen/structure/StructurePiece;"
            )
    )
    private static StructurePiece oririmod$adaptMonumentElevation(ChunkPos chunkPos, WorldgenRandom random,
            StructurePiecesBuilder builder, Structure.GenerationContext context) {
        StructurePiece piece = createTopPiece(chunkPos, random);
        if (context.chunkGenerator() instanceof ElderwoodsChunkGenerator
                && piece instanceof OceanMonumentPlacementHelper.ShiftableMonument shiftable) {
            int targetMinY = OceanMonumentPlacementHelper.computeAdaptiveMinY(context, piece.getBoundingBox());
            int dy = targetMinY - piece.getBoundingBox().minY();
            shiftable.oririmod$shiftVertical(dy);
        }
        return piece;
    }

    @Inject(method = "regeneratePiecesAfterLoad", at = @At("RETURN"))
    private static void oririmod$preserveShiftedElevationOnReload(ChunkPos chunkPos, long seed,
            PiecesContainer piecesContainer, CallbackInfoReturnable<PiecesContainer> cir) {
        PiecesContainer rebuilt = cir.getReturnValue();
        if (piecesContainer.isEmpty() || rebuilt == null || rebuilt.isEmpty()) {
            return;
        }
        int savedMinY = piecesContainer.pieces().get(0).getBoundingBox().minY();
        StructurePiece rebuiltTop = rebuilt.pieces().get(0);
        int dy = savedMinY - rebuiltTop.getBoundingBox().minY();
        if (dy != 0 && rebuiltTop instanceof OceanMonumentPlacementHelper.ShiftableMonument shiftable) {
            shiftable.oririmod$shiftVertical(dy);
        }
    }
}

