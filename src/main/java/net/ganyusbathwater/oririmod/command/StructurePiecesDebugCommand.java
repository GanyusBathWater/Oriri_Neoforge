package net.ganyusbathwater.oririmod.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Debug command that lays out all individual pieces of a structure (or NBT template prefix)
 * in a neatly packed cluster in-game with floating TextDisplay labels and intact Jigsaw blocks.
 *
 * <ul>
 *   <li>{@code /oriri debugpieces structure <structure_id> [spacing] [limit]}</li>
 *   <li>{@code /oriri debugpieces template <prefix> [spacing] [limit]}</li>
 *   <li>{@code /oriri debugpieces clearlabels [radius]}</li>
 * </ul>
 */
public final class StructurePiecesDebugCommand {
    public static final String LABEL_TAG = "oriri_debug_label";
    private static final int DEFAULT_SPACING = 4;
    private static final int DEFAULT_LIMIT = 80;
    private static final int DEFAULT_CLEAR_RADIUS = 256;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final SuggestionProvider<CommandSourceStack> TEMPLATE_PREFIX_SUGGESTIONS = (ctx, builder) -> {
        StructureTemplateManager manager = ctx.getSource().getServer().getStructureManager();
        Set<ResourceLocation> prefixes = new LinkedHashSet<>();
        manager.listTemplates().forEach(id -> {
            String path = id.getPath();
            int firstSlash = path.indexOf('/');
            if (firstSlash > 0) {
                prefixes.add(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, firstSlash)));
                int secondSlash = path.indexOf('/', firstSlash + 1);
                if (secondSlash > firstSlash) {
                    prefixes.add(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, secondSlash)));
                }
            } else {
                prefixes.add(id);
            }
        });
        return SharedSuggestionProvider.suggestResource(prefixes, builder);
    };

    private StructurePiecesDebugCommand() {}

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("debugpieces")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("structure")
                        .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                                .executes(ctx -> executeStructure(ctx, DEFAULT_SPACING, DEFAULT_LIMIT))
                                .then(Commands.argument("spacing", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> executeStructure(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "spacing"),
                                                DEFAULT_LIMIT))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 500))
                                                .executes(ctx -> executeStructure(
                                                        ctx,
                                                        IntegerArgumentType.getInteger(ctx, "spacing"),
                                                        IntegerArgumentType.getInteger(ctx, "limit")))))))
                .then(Commands.literal("template")
                        .then(Commands.argument("prefix", ResourceLocationArgument.id())
                                .suggests(TEMPLATE_PREFIX_SUGGESTIONS)
                                .executes(ctx -> executeTemplatePrefix(ctx, DEFAULT_SPACING, DEFAULT_LIMIT))
                                .then(Commands.argument("spacing", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> executeTemplatePrefix(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "spacing"),
                                                DEFAULT_LIMIT))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 500))
                                                .executes(ctx -> executeTemplatePrefix(
                                                        ctx,
                                                        IntegerArgumentType.getInteger(ctx, "spacing"),
                                                        IntegerArgumentType.getInteger(ctx, "limit")))))))
                .then(Commands.literal("clearlabels")
                        .executes(ctx -> executeClearLabels(ctx, DEFAULT_CLEAR_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 2048))
                                .executes(ctx -> executeClearLabels(ctx, IntegerArgumentType.getInteger(ctx, "radius")))));
    }

    private static int executeStructure(CommandContext<CommandSourceStack> ctx, int spacing, int limit)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        Holder.Reference<Structure> structureHolder = ResourceKeyArgument.getStructure(ctx, "structure");
        ResourceLocation structureId = structureHolder.key().location();
        Structure structure = structureHolder.value();

        // 1. Try Jigsaw / template-pool crawl first (works for vanilla Trial Chambers, Villages,
        //    Bastions, Ancient Cities, and custom Jigsaw subclasses like SafeUndergroundJigsawStructure).
        List<ResourceLocation> jigsawTemplates = collectJigsawTemplates(level, structure);
        if (!jigsawTemplates.isEmpty()) {
            return spawnTemplateCluster(source, level, structureId.toString(), jigsawTemplates, spacing, limit);
        }

        // 2. Fallback for non-Jigsaw procedural Java structures (e.g. Mineshafts, Strongholds, Fortresses).
        return spawnProceduralPieceCluster(source, level, structureId, structure, spacing, limit);
    }

    private static int executeTemplatePrefix(CommandContext<CommandSourceStack> ctx, int spacing, int limit) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        ResourceLocation prefix = ResourceLocationArgument.getId(ctx, "prefix");

        StructureTemplateManager manager = level.getServer().getStructureManager();
        String namespace = prefix.getNamespace();
        String pathPrefix = prefix.getPath();

        List<ResourceLocation> matching = manager.listTemplates()
                .filter(id -> id.getNamespace().equals(namespace))
                .filter(id -> pathPrefix.isEmpty()
                        || id.getPath().equals(pathPrefix)
                        || id.getPath().startsWith(pathPrefix + "/")
                        || id.getPath().startsWith(pathPrefix))
                .distinct()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        if (matching.isEmpty()) {
            source.sendFailure(Component.literal("No NBT structure templates found matching prefix: " + prefix));
            return 0;
        }

        return spawnTemplateCluster(source, level, prefix.toString(), matching, spacing, limit);
    }

    private static int executeClearLabels(CommandContext<CommandSourceStack> ctx, int radius) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        AABB area = new AABB(origin).inflate(radius);

        List<Display.TextDisplay> labels = level.getEntitiesOfClass(
                Display.TextDisplay.class,
                area,
                entity -> entity.getTags().contains(LABEL_TAG)
        );
        for (Display.TextDisplay label : labels) {
            label.discard();
        }

        int removed = labels.size();
        source.sendSuccess(() -> Component.literal("Removed " + removed + " debug piece label(s) within " + radius + " blocks."), true);
        return removed;
    }

    // -------------------------------------------------------------------------
    // Jigsaw Template Pool Graph Crawler (No Reflection — Uses 1.21.1 Codecs)
    // -------------------------------------------------------------------------

    private static List<ResourceLocation> collectJigsawTemplates(ServerLevel level, Structure structure) {
        RegistryAccess registryAccess = level.registryAccess();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

        Optional<JsonElement> encoded = Structure.DIRECT_CODEC.encodeStart(ops, structure).result();
        if (encoded.isEmpty()) {
            return List.of();
        }

        Set<ResourceLocation> seedPools = new LinkedHashSet<>();
        Map<ResourceLocation, Set<ResourceLocation>> aliasMap = new HashMap<>();
        extractPoolReferencesFromStructureJson(encoded.get(), seedPools, aliasMap);

        if (seedPools.isEmpty()) {
            return List.of();
        }

        Registry<StructureTemplatePool> poolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);
        StructureTemplateManager templateManager = level.getServer().getStructureManager();

        Set<ResourceLocation> visitedPools = new HashSet<>();
        Queue<ResourceLocation> poolQueue = new ArrayDeque<>();
        for (ResourceLocation poolId : seedPools) {
            enqueuePool(poolId, visitedPools, poolQueue, aliasMap);
        }

        Set<ResourceLocation> discoveredTemplates = new LinkedHashSet<>();
        RandomSource deterministicRandom = RandomSource.create(0L);
        StructurePlaceSettings emptySettings = new StructurePlaceSettings();

        while (!poolQueue.isEmpty()) {
            ResourceLocation poolId = poolQueue.poll();
            StructureTemplatePool pool = poolRegistry.get(poolId);
            if (pool == null) {
                continue;
            }

            // Enqueue fallback pool (often contains dead-end caps!)
            pool.getFallback().unwrapKey().map(ResourceKey::location)
                    .ifPresent(fb -> enqueuePool(fb, visitedPools, poolQueue, aliasMap));

            // Inspect every unique pool element
            Set<StructurePoolElement> uniqueElements = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            uniqueElements.addAll(pool.getShuffledTemplates(deterministicRandom));

            for (StructurePoolElement element : uniqueElements) {
                Optional<JsonElement> elemJson = StructurePoolElement.CODEC.encodeStart(ops, element).result();
                if (elemJson.isEmpty()) {
                    continue;
                }
                List<ResourceLocation> elementTemplates = new ArrayList<>();
                extractTemplateLocations(elemJson.get(), elementTemplates);

                for (ResourceLocation templateId : elementTemplates) {
                    if (!discoveredTemplates.add(templateId)) {
                        continue;
                    }
                    // Scan Jigsaw blocks inside this template to discover deeper pools
                    templateManager.get(templateId).ifPresent(template -> {
                        List<StructureTemplate.StructureBlockInfo> jigsaws =
                                template.filterBlocks(BlockPos.ZERO, emptySettings, Blocks.JIGSAW);
                        for (StructureTemplate.StructureBlockInfo info : jigsaws) {
                            CompoundTag nbt = info.nbt();
                            if (nbt != null && nbt.contains("pool")) {
                                ResourceLocation nextPool = ResourceLocation.tryParse(nbt.getString("pool"));
                                if (nextPool != null) {
                                    enqueuePool(nextPool, visitedPools, poolQueue, aliasMap);
                                }
                            }
                        }
                    });
                }
            }
        }

        List<ResourceLocation> sorted = new ArrayList<>(discoveredTemplates);
        sorted.sort(Comparator.comparing(ResourceLocation::toString));
        return sorted;
    }

    private static void enqueuePool(
            ResourceLocation poolId,
            Set<ResourceLocation> visited,
            Queue<ResourceLocation> queue,
            Map<ResourceLocation, Set<ResourceLocation>> aliasMap) {
        if (poolId == null || (poolId.getNamespace().equals("minecraft") && poolId.getPath().equals("empty"))) {
            return;
        }
        if (visited.add(poolId)) {
            queue.add(poolId);
        }
        Set<ResourceLocation> aliasedTargets = aliasMap.get(poolId);
        if (aliasedTargets != null) {
            for (ResourceLocation target : aliasedTargets) {
                if (visited.add(target)) {
                    queue.add(target);
                }
            }
        }
    }

    private static void extractPoolReferencesFromStructureJson(
            JsonElement element,
            Set<ResourceLocation> seedPools,
            Map<ResourceLocation, Set<ResourceLocation>> aliasMap) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("start_pool") && obj.get("start_pool").isJsonPrimitive()) {
                ResourceLocation startPool = ResourceLocation.tryParse(obj.get("start_pool").getAsString());
                if (startPool != null) {
                    seedPools.add(startPool);
                }
            }

            // Direct alias: { "alias": "...", "target": "..." }
            if (obj.has("alias") && obj.get("alias").isJsonPrimitive()) {
                ResourceLocation aliasKey = ResourceLocation.tryParse(obj.get("alias").getAsString());
                if (aliasKey != null) {
                    if (obj.has("target") && obj.get("target").isJsonPrimitive()) {
                        ResourceLocation target = ResourceLocation.tryParse(obj.get("target").getAsString());
                        if (target != null) {
                            aliasMap.computeIfAbsent(aliasKey, k -> new LinkedHashSet<>()).add(target);
                            seedPools.add(target);
                        }
                    }
                    // Random alias: { "alias": "...", "targets": [ { "data": "...", "weight": 1 } ] }
                    if (obj.has("targets") && obj.get("targets").isJsonArray()) {
                        for (JsonElement entry : obj.getAsJsonArray("targets")) {
                            if (entry.isJsonObject() && entry.getAsJsonObject().has("data")
                                    && entry.getAsJsonObject().get("data").isJsonPrimitive()) {
                                ResourceLocation target = ResourceLocation.tryParse(entry.getAsJsonObject().get("data").getAsString());
                                if (target != null) {
                                    aliasMap.computeIfAbsent(aliasKey, k -> new LinkedHashSet<>()).add(target);
                                    seedPools.add(target);
                                }
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                extractPoolReferencesFromStructureJson(entry.getValue(), seedPools, aliasMap);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                extractPoolReferencesFromStructureJson(child, seedPools, aliasMap);
            }
        }
    }

    private static void extractTemplateLocations(JsonElement element, List<ResourceLocation> out) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("location") && obj.get("location").isJsonPrimitive()) {
                ResourceLocation loc = ResourceLocation.tryParse(obj.get("location").getAsString());
                if (loc != null) {
                    out.add(loc);
                }
            }
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                extractTemplateLocations(entry.getValue(), out);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                extractTemplateLocations(child, out);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Placement: NBT Template Cluster (Jigsaw Blocks Kept Intact)
    // -------------------------------------------------------------------------

    private record LoadedTemplateEntry(
            ResourceLocation id,
            StructureTemplate template,
            Vec3i size,
            int jigsawCount
    ) {}

    private static int spawnTemplateCluster(
            CommandSourceStack source,
            ServerLevel level,
            String displayName,
            List<ResourceLocation> templateIds,
            int spacing,
            int limit) {
        StructureTemplateManager manager = level.getServer().getStructureManager();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKnownShape(true);

        int totalFound = templateIds.size();
        int toProcess = Math.min(totalFound, limit);

        List<LoadedTemplateEntry> entries = new ArrayList<>(toProcess);
        long totalArea = 0;

        for (int i = 0; i < toProcess; i++) {
            ResourceLocation id = templateIds.get(i);
            Optional<StructureTemplate> opt = manager.get(id);
            if (opt.isEmpty()) {
                continue;
            }
            StructureTemplate template = opt.get();
            Vec3i size = template.getSize();
            if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
                continue;
            }
            int jigsaws = template.filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW).size();
            entries.add(new LoadedTemplateEntry(id, template, size, jigsaws));
            totalArea += (long) (size.getX() + spacing) * (size.getZ() + spacing);
        }

        if (entries.isEmpty()) {
            source.sendFailure(Component.literal("Found template references for " + displayName + ", but none could be loaded."));
            return 0;
        }

        int maxRowWidth = Math.max(48, (int) Math.ceil(Math.sqrt(totalArea) * 1.25));
        BlockPos origin = BlockPos.containing(source.getPosition()).offset(4, 0, 4);
        int cursorX = origin.getX();
        int cursorZ = origin.getZ();
        int rowMaxZ = 0;

        RandomSource random = level.getRandom();
        int placed = 0;

        for (LoadedTemplateEntry entry : entries) {
            int sx = entry.size().getX();
            int sy = entry.size().getY();
            int sz = entry.size().getZ();

            if (cursorX > origin.getX() && (cursorX - origin.getX()) + sx > maxRowWidth) {
                cursorX = origin.getX();
                cursorZ += rowMaxZ + spacing;
                rowMaxZ = 0;
            }

            BlockPos placePos = new BlockPos(cursorX, origin.getY(), cursorZ);
            ensureChunksLoaded(level, placePos.getX(), placePos.getZ(), placePos.getX() + sx, placePos.getZ() + sz);

            entry.template().placeInWorld(level, placePos, placePos, settings, random, PLACE_FLAGS);

            MutableComponent label = Component.literal(entry.id().getPath())
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("\n" + sx + "x" + sy + "x" + sz)
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" | " + entry.jigsawCount() + " jigsaw" + (entry.jigsawCount() == 1 ? "" : "s"))
                            .withStyle(entry.jigsawCount() > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));

            spawnLabel(level, placePos.getX() + sx / 2.0, placePos.getY() + sy + 1.5, placePos.getZ() + sz / 2.0, label);

            cursorX += sx + spacing;
            rowMaxZ = Math.max(rowMaxZ, sz);
            placed++;
        }

        final int placedCount = placed;
        MutableComponent msg = Component.literal("Spawned " + placedCount + " NBT piece(s) for " + displayName
                + " (Jigsaw blocks intact) starting at " + origin.toShortString() + ".");
        if (totalFound > placedCount) {
            msg.append(Component.literal(" Showing " + placedCount + " of " + totalFound
                    + " total — pass a higher limit (e.g. " + totalFound + ") or a narrower template prefix to see all.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        source.sendSuccess(() -> msg, true);
        return placedCount;
    }

    // -------------------------------------------------------------------------
    // Placement: Hard-Coded / Procedural Java Structure Pieces (e.g. Mineshafts)
    // -------------------------------------------------------------------------

    private record CollectedJavaPiece(
            String displayTitle,
            String subtitle,
            StructurePiece piece
    ) {}

    private static int spawnProceduralPieceCluster(
            CommandSourceStack source,
            ServerLevel level,
            ResourceLocation structureId,
            Structure structure,
            int spacing,
            int limit) {
        BlockPos origin = BlockPos.containing(source.getPosition()).offset(4, 0, 4);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        StructureTemplateManager templateManager = level.getServer().getStructureManager();
        StructurePieceSerializationContext serCtx = StructurePieceSerializationContext.fromLevel(level);

        // Sample a few starts with biomePredicate = true so even in a Void Superflat world
        // we capture all distinct piece classes (e.g. MineShaftRoom, Corridor, Crossing, Stairs).
        Map<String, CollectedJavaPiece> deduplicated = new LinkedHashMap<>();
        int validStarts = 0;

        for (int attempt = 0; attempt < 24 && validStarts < 4; attempt++) {
            long seed = level.getSeed() + attempt * 341873128712L + 132897987541L;
            ChunkPos sampleChunk = new ChunkPos((origin.getX() >> 4) + attempt * 7, (origin.getZ() >> 4) + attempt * 5);
            StructureStart start = structure.generate(
                    level.registryAccess(),
                    generator,
                    generator.getBiomeSource(),
                    level.getChunkSource().randomState(),
                    templateManager,
                    seed,
                    sampleChunk,
                    0,
                    level,
                    biome -> true
            );

            if (!start.isValid() || start.getPieces().isEmpty()) {
                continue;
            }
            validStarts++;

            for (StructurePiece piece : start.getPieces()) {
                BoundingBox box = piece.getBoundingBox();
                ResourceLocation pieceTypeId = BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType());
                String pieceTypeStr = pieceTypeId != null ? pieceTypeId.toString() : "unknown";
                String className = piece.getClass().getSimpleName();
                if (className.isEmpty()) {
                    className = pieceTypeStr;
                }

                String dedupeKey;
                String title;
                String subtitle;
                if (piece instanceof TemplateStructurePiece) {
                    CompoundTag tag = piece.createTag(serCtx);
                    String templateName = tag.contains("Template") ? tag.getString("Template") : className;
                    dedupeKey = "tpl|" + templateName;
                    title = templateName;
                    subtitle = pieceTypeStr;
                } else {
                    // Distinguish tall vs standard variants (e.g. 2-story vs 1-story Mineshaft crossings)
                    String heightBucket = box.getYSpan() > 7 ? "tall" : "std";
                    dedupeKey = pieceTypeStr + "|" + className + "|" + heightBucket;
                    title = className + (heightBucket.equals("tall") ? " (tall)" : "");
                    subtitle = pieceTypeStr;
                }

                deduplicated.putIfAbsent(dedupeKey, new CollectedJavaPiece(title, subtitle, piece));
            }
        }

        if (deduplicated.isEmpty()) {
            source.sendFailure(Component.literal(
                    "Could not generate any structure pieces for " + structureId
                            + " in this dimension/chunk generator."));
            return 0;
        }

        List<CollectedJavaPiece> piecesToPlace = new ArrayList<>(deduplicated.values());
        int totalUnique = piecesToPlace.size();
        if (piecesToPlace.size() > limit) {
            piecesToPlace = piecesToPlace.subList(0, limit);
        }

        long totalArea = 0;
        for (CollectedJavaPiece entry : piecesToPlace) {
            BoundingBox b = entry.piece().getBoundingBox();
            totalArea += (long) (b.getXSpan() + spacing) * (b.getZSpan() + spacing);
        }

        int maxRowWidth = Math.max(48, (int) Math.ceil(Math.sqrt(totalArea) * 1.25));
        int cursorX = origin.getX();
        int cursorZ = origin.getZ();
        int rowMaxZ = 0;
        RandomSource random = level.getRandom();
        boolean needsUndergroundMedium = structure instanceof MineshaftStructure
                || structure.step() == GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
        int placed = 0;

        for (CollectedJavaPiece entry : piecesToPlace) {
            StructurePiece piece = entry.piece();
            BoundingBox oldBox = piece.getBoundingBox();
            int sx = oldBox.getXSpan();
            int sy = oldBox.getYSpan();
            int sz = oldBox.getZSpan();

            if (cursorX > origin.getX() && (cursorX - origin.getX()) + sx > maxRowWidth) {
                cursorX = origin.getX();
                cursorZ += rowMaxZ + spacing;
                rowMaxZ = 0;
            }

            int dx = cursorX - oldBox.minX();
            int dy = origin.getY() - oldBox.minY();
            int dz = cursorZ - oldBox.minZ();
            piece.move(dx, dy, dz);

            BoundingBox movedBox = piece.getBoundingBox();
            // Inflate the clipping box slightly (especially vertically for minY-1 plank floors and maxY+1 ceiling checks)
            // because StructurePiece.getBlock returns AIR for any coordinate outside the clipping box!
            BoundingBox clipBox = new BoundingBox(
                    movedBox.minX() - 1,
                    Math.max(level.getMinBuildHeight(), movedBox.minY() - 2),
                    movedBox.minZ() - 1,
                    movedBox.maxX() + 1,
                    Math.min(level.getMaxBuildHeight() - 1, movedBox.maxY() + 2),
                    movedBox.maxZ() + 1
            );
            ensureChunksLoaded(level, clipBox.minX(), clipBox.minZ(), clipBox.maxX(), clipBox.maxZ());

            // Underground procedural structures (like Mineshafts & Strongholds) carve CAVE_AIR into solid rock,
            // require a non-air ceiling at maxY+1 (isSupportingBox), and check y < OCEAN_FLOOR_WG (isInterior).
            // Pre-filling with translucent stained glass + wrapping getHeight/getBiome makes them spawn cleanly in open air!
            if (needsUndergroundMedium) {
                prefillUndergroundDiorama(level, movedBox);
            }

            WorldGenLevel debugLevel = createDebugWorldGenLevel(level, movedBox.maxY() + 16);
            BlockPos center = movedBox.getCenter();
            piece.postProcess(
                    debugLevel,
                    level.structureManager(),
                    generator,
                    random,
                    clipBox,
                    new ChunkPos(center),
                    center
            );

            MutableComponent label = Component.literal(entry.displayTitle())
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("\n" + entry.subtitle() + " | " + sx + "x" + sy + "x" + sz)
                            .withStyle(ChatFormatting.GRAY));

            spawnLabel(level, movedBox.minX() + sx / 2.0, movedBox.maxY() + 2.5, movedBox.minZ() + sz / 2.0, label);

            cursorX += sx + spacing;
            rowMaxZ = Math.max(rowMaxZ, sz);
            placed++;
        }

        final int placedCount = placed;
        MutableComponent msg = Component.literal("Spawned " + placedCount + " unique Java piece(s) for "
                + structureId + " starting at " + origin.toShortString() + ".");
        if (totalUnique > placedCount) {
            msg.append(Component.literal(" Showing " + placedCount + " of " + totalUnique + " unique piece types.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        source.sendSuccess(() -> msg, true);
        return placedCount;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Fills the piece's bounding box (plus a 1-block ceiling cap at maxY+1) with translucent glass
     * so carving pieces (MineShaftRoom, MineShaftStairs, MineShaftCorridor, Strongholds) have a
     * visible medium to carve CAVE_AIR out of and a solid ceiling for isSupportingBox checks,
     * while leaving minY-1 open so plank bridge floors still generate underneath corridors.
     */
    private static void prefillUndergroundDiorama(ServerLevel level, BoundingBox box) {
        BlockState medium = Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int maxY = Math.min(level.getMaxBuildHeight() - 1, box.maxY() + 1);
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= maxY; y++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, medium, PLACE_FLAGS);
                    }
                }
            }
        }
    }

    /**
     * Wraps a {@link ServerLevel} in a {@link WorldGenLevel} proxy that reports a virtual surface
     * above {@code virtualSurfaceY} (so {@link StructurePiece#isInterior} succeeds in open air /
     * Void Superflat worlds) and replaces {@link BiomeTags#MINESHAFT_BLOCKING} biomes with Plains.
     */
    private static WorldGenLevel createDebugWorldGenLevel(ServerLevel delegate, int virtualSurfaceY) {
        Holder<Biome> safeBiome = delegate.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(Biomes.PLAINS)
                .map(h -> (Holder<Biome>) h)
                .orElse(null);

        return (WorldGenLevel) Proxy.newProxyInstance(
                WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getHeight") && args != null && args.length == 3
                            && args[0] instanceof Heightmap.Types) {
                        int actual = delegate.getHeight((Heightmap.Types) args[0], (Integer) args[1], (Integer) args[2]);
                        return Math.max(actual, virtualSurfaceY);
                    }
                    if (name.equals("getBiome") && args != null && args.length == 1
                            && args[0] instanceof BlockPos bp) {
                        Holder<Biome> realBiome = delegate.getBiome(bp);
                        if (realBiome.is(BiomeTags.MINESHAFT_BLOCKING) && safeBiome != null) {
                            return safeBiome;
                        }
                        return realBiome;
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );
    }

    private static void ensureChunksLoaded(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    private static void spawnLabel(ServerLevel level, double x, double y, double z, Component text) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.putString(Display.TextDisplay.TAG_TEXT, Component.Serializer.toJson(text, level.registryAccess()));
        tag.putString(Display.TAG_BILLBOARD, "center");
        tag.putInt("line_width", 320);
        tag.putInt("background", 0xB0000000);
        tag.putBoolean("shadow", true);
        tag.putBoolean("see_through", false);
        tag.putFloat(Display.TAG_VIEW_RANGE, 3.0F);
        tag.putString("alignment", "center");

        Entity entity = EntityType.loadEntityRecursive(tag, level, e -> {
            e.setPos(x, y, z);
            e.addTag(LABEL_TAG);
            return e;
        });
        if (entity != null) {
            level.addFreshEntity(entity);
        }
    }
}
