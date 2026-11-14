package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.ganyusbathwater.oririmod.worldgen.structure.OceanMonumentPlacementHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces$OceanMonumentPiece")
public abstract class OceanMonumentPieceMixin extends StructurePiece {

    @Shadow
    @Final
    protected static BlockState FILL_BLOCK;

    @Shadow
    @Final
    protected static Set<Block> FILL_KEEP;

    protected OceanMonumentPieceMixin(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
    }

    @Inject(method = "generateWaterBox", at = @At("HEAD"), cancellable = true)
    private void oririmod$handleElderwoodsWaterBox(WorldGenLevel level, BoundingBox boundingBox,
            int x1, int y1, int z1, int x2, int y2, int z2, CallbackInfo ci) {
        if (!OceanMonumentPlacementHelper.ELDERWOODS_ACTIVE.get()) {
            return;
        }

        if ((Object) this instanceof OceanMonumentPieces.MonumentBuilding) {
            // Skip the massive 58x58 bounding box clear AND the 5-step outer crater moat rings (-1..-5 / 58..62)
            if (x1 < 0 || z1 < 0 || x2 >= 58 || z2 >= 58 || (x1 == 0 && z1 == 0 && x2 == 58 && z2 == 58)) {
                ci.cancel();
                return;
            }
        }

        // For interior monument calls, use Elderwoods BASE_HEIGHT (72) as sea level instead of Level.getSeaLevel() (63)
        for (int y = y1; y <= y2; y++) {
            boolean aboveSea = this.getWorldY(y) >= ElderwoodsChunkGenerator.BASE_HEIGHT;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    BlockState blockstate = this.getBlock(level, x, y, z, boundingBox);
                    if (!FILL_KEEP.contains(blockstate.getBlock())) {
                        if (aboveSea && blockstate != FILL_BLOCK) {
                            this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, boundingBox);
                        } else {
                            this.placeBlock(level, FILL_BLOCK, x, y, z, boundingBox);
                        }
                    }
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "generateBoxOnFillOnly", at = @At("HEAD"), cancellable = true)
    private void oririmod$handleElderwoodsBoxOnFillOnly(WorldGenLevel level, BoundingBox box,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, CallbackInfo ci) {
        if (!OceanMonumentPlacementHelper.ELDERWOODS_ACTIVE.get()) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            boolean aboveSea = this.getWorldY(y) >= ElderwoodsChunkGenerator.BASE_HEIGHT;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState existing = this.getBlock(level, x, y, z, box);
                    if (existing == FILL_BLOCK || (aboveSea && existing.isAir())) {
                        this.placeBlock(level, state, x, y, z, box);
                    }
                }
            }
        }
        ci.cancel();
    }
}

