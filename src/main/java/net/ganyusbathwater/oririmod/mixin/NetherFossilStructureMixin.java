package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.worldgen.ElderwoodsChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilPieces;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(NetherFossilStructure.class)
public abstract class NetherFossilStructureMixin {

    @Inject(method = "findGenerationPoint", at = @At("HEAD"), cancellable = true)
    private void oririmod$allowBloodPondFossils(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        if (!(context.chunkGenerator() instanceof ElderwoodsChunkGenerator ego)) {
            return;
        }

        WorldgenRandom random = context.random();
        int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
        int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);

        int surfaceY = ego.getBaseHeight(
                x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        if (surfaceY < ElderwoodsChunkGenerator.BASE_HEIGHT - 8) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        // Anchor submerged Blood Water pond fossils near the shallow pond bed so rib cages emerge above water
        int placeY = (surfaceY < ElderwoodsChunkGenerator.BASE_HEIGHT - 1)
                ? Math.max(surfaceY, ElderwoodsChunkGenerator.BASE_HEIGHT - 3)
                : surfaceY;

        BlockPos pos = new BlockPos(x, placeY, z);
        cir.setReturnValue(Optional.of(new Structure.GenerationStub(
                pos,
                builder -> NetherFossilPieces.addPieces(context.structureTemplateManager(), builder, random, pos))));
    }
}
