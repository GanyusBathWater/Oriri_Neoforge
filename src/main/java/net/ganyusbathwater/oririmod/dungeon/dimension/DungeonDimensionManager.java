package net.ganyusbathwater.oririmod.dungeon.dimension;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.dungeon.DungeonDefinition;
import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Handles physical instantiation of dungeons (placing structures) and
 * teleporting players into them.
 */
public class DungeonDimensionManager {

    /**
     * Loads a StructureTemplate directly from the mod's classpath resources.
     * This bypasses the StructureTemplateManager API entirely, which in 1.21.1
     * only searches the world's generated/ folder and not mod jar resources
     * until the template has been previously loaded/cached.
     */
    private static Optional<StructureTemplate> loadTemplateFromClasspath(ServerLevel level, ResourceLocation id) {
        String path = "/data/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt";
        try (InputStream stream = DungeonDimensionManager.class.getResourceAsStream(path)) {
            if (stream == null) {
                return Optional.empty();
            }
            CompoundTag nbt = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            StructureTemplate template = new StructureTemplate();
            template.load(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt);
            OririMod.LOGGER.info("[DungeonDimensionManager] Loaded structure template: {}", id);
            return Optional.of(template);
        } catch (IOException e) {
            OririMod.LOGGER.error("[DungeonDimensionManager] Failed to load structure template {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Loads the structure NBT defined in the DungeonDefinition and places it
     * in the target dimension at the instance's allocated origin.
     */
    public static void placeDungeonStructure(ServerLevel dimensionLevel, DungeonDefinition definition, DungeonInstance instance) {
        BlockPos origin = instance.getOrigin();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                // We must NOT ignore entities, because we need our DungeonMarkerEntity instances to spawn!
                .setIgnoreEntities(false)
                .setKnownShape(true);

        // Try single file first
        Optional<StructureTemplate> single = loadTemplateFromClasspath(dimensionLevel, definition.structureId());
        if (single.isPresent()) {
            StructureTemplate part = single.get();
            forceLoadChunks(dimensionLevel, origin, part);
            part.placeInWorld(dimensionLevel, origin, origin, settings, dimensionLevel.getRandom(), 2);
            instance.setStructureBounds(part.getBoundingBox(settings, origin));
            return;
        }

        // Try multi-part grid structure (e.g., name_0_0, name_0_48)
        int gridSize = 48;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean foundAny = false;

        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                int offsetX = x * gridSize;
                int offsetZ = z * gridSize;

                ResourceLocation partId = ResourceLocation.fromNamespaceAndPath(
                    definition.structureId().getNamespace(),
                    definition.structureId().getPath() + "_" + offsetX + "_" + offsetZ
                );

                Optional<StructureTemplate> partOpt = loadTemplateFromClasspath(dimensionLevel, partId);
                if (partOpt.isPresent()) {
                    foundAny = true;
                    StructureTemplate part = partOpt.get();
                    BlockPos partOrigin = origin.offset(offsetX, 0, offsetZ);

                    forceLoadChunks(dimensionLevel, partOrigin, part);
                    part.placeInWorld(dimensionLevel, partOrigin, partOrigin, settings, dimensionLevel.getRandom(), 2);

                    var bounds = part.getBoundingBox(settings, partOrigin);
                    minX = Math.min(minX, bounds.minX());
                    minY = Math.min(minY, bounds.minY());
                    minZ = Math.min(minZ, bounds.minZ());
                    maxX = Math.max(maxX, bounds.maxX());
                    maxY = Math.max(maxY, bounds.maxY());
                    maxZ = Math.max(maxZ, bounds.maxZ());
                }
            }
        }

        if (foundAny) {
            instance.setStructureBounds(new net.minecraft.world.level.levelgen.structure.BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
            OririMod.LOGGER.info("[DungeonDimensionManager] Placed multi-part structure for {} at origin {}", definition.structureId(), origin);
        } else {
            OririMod.LOGGER.error("[DungeonDimensionManager] CRITICAL: Could not find any structure files for: {}", definition.structureId());
            for (java.util.UUID pid : instance.getPlayers()) {
                ServerPlayer sp = dimensionLevel.getServer().getPlayerList().getPlayer(pid);
                if (sp != null) {
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "ERROR: Dungeon structure files not found for " + definition.structureId() + "! Please check your datapack or structure files.")
                            .withStyle(net.minecraft.ChatFormatting.RED), false);
                }
            }
        }
    }

    /**
     * Force-loads all chunks covered by the given structure template before placement.
     * Prevents placeInWorld() from silently failing on unloaded chunks.
     */
    private static void forceLoadChunks(ServerLevel level, BlockPos origin, StructureTemplate template) {
        net.minecraft.core.Vec3i size = template.getSize();
        net.minecraft.world.level.ChunkPos start = new net.minecraft.world.level.ChunkPos(origin);
        net.minecraft.world.level.ChunkPos end = new net.minecraft.world.level.ChunkPos(origin.offset(size.getX(), 0, size.getZ()));
        for (int cx = start.x; cx <= end.x; cx++) {
            for (int cz = start.z; cz <= end.z; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    /**
     * Teleports a player into the dungeon dimension relative to the instance origin.
     */
    public static void teleportPlayerToDungeon(ServerPlayer player, ServerLevel dimensionLevel, DungeonInstance instance) {
        BlockPos origin = instance.getOrigin();

        BlockPos spawnPos = instance.getPlayerSpawnPos();
        if (spawnPos == null) {
            spawnPos = origin.offset(5, 1, 5); // Fallback
        }

        player.teleportTo(
                dimensionLevel,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                0, 0
        );
    }
}
