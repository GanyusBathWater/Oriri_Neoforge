package net.ganyusbathwater.oririmod.compat.dh;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.factories.IDhApiWrapperFactory;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkProcessingEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Optional Distant Horizons integration (only loaded when {@code distanthorizons} is present).
 * <p>
 * Repairs DH 3.1.x's {@code ChunkCompoundTagParser_neoforge} bug where pre-existing {@code .mca}
 * chunks read from disk look up {@code "biomeRegistry"} via {@code CompoundTag.getCompound(...)}
 * (which returns a non-null empty tag instead of {@code null}), skipping the {@code "biomes"}
 * palette and falling back to {@code minecraft:plains} for all sections.
 */
public final class OririDistantHorizonsCompat {

    public static final String ELDERWOODS_DIM_ID = OririMod.MOD_ID + ":elderwoods";

    enum TintChannel {
        NONE,
        GRASS,
        FOLIAGE,
        WATER
    }

    private static final Map<ResourceLocation, IDhApiBiomeWrapper> BIOME_WRAPPER_CACHE = new ConcurrentHashMap<>();
    private static volatile ElderwoodsBiomeSource cachedSourceIdentity;
    private static volatile Supplier<ElderwoodsBiomeSource> clientFallbackSupplier;
    private static volatile boolean loggedWrapperFailure = false;
    private static volatile boolean commonRegistered = false;

    private OririDistantHorizonsCompat() {
    }

    /**
     * Registers server/common Distant Horizons event hooks during mod setup.
     */
    public static synchronized void registerCommon() {
        if (commonRegistered) {
            return;
        }
        commonRegistered = true;

        DhApi.events.bind(DhApiChunkProcessingEvent.class, new DhApiChunkProcessingEvent() {
            @Override
            public void blockOrBiomeChangedDuringChunkProcessing(DhApiEventParam<EventParam> param) {
                handleChunkProcessing(param);
            }
        });
    }

    static void setClientFallbackSupplier(Supplier<ElderwoodsBiomeSource> supplier) {
        clientFallbackSupplier = supplier;
    }

    static TintChannel classifyTintChannel(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.POTTED_FERN)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.PINK_PETALS)) {
            return TintChannel.GRASS;
        }
        if (state.is(Blocks.OAK_LEAVES)
                || state.is(Blocks.JUNGLE_LEAVES)
                || state.is(Blocks.ACACIA_LEAVES)
                || state.is(Blocks.DARK_OAK_LEAVES)
                || state.is(Blocks.MANGROVE_LEAVES)
                || state.is(Blocks.VINE)) {
            return TintChannel.FOLIAGE;
        }
        if (state.is(Blocks.WATER)) {
            return TintChannel.WATER;
        }
        return TintChannel.NONE;
    }

    static int retintChannel(int shown, int oldTint, int newTint) {
        if (oldTint <= 0) {
            return shown;
        }
        int scaled = Math.round(shown * ((float) newTint / (float) oldTint));
        return Math.max(0, Math.min(255, scaled));
    }

    private static void handleChunkProcessing(DhApiEventParam<DhApiChunkProcessingEvent.EventParam> param) {
        if (param == null || param.value == null) {
            return;
        }
        DhApiChunkProcessingEvent.EventParam ev = param.value;
        if (ev.levelWrapper == null || !ELDERWOODS_DIM_ID.equals(ev.levelWrapper.getDimensionName())) {
            return;
        }
        if (ev.currentBiome == null || !(ev.currentBiome.getWrappedMcObject() instanceof Holder<?> rawHolder)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Holder<Biome> currentBiome = (Holder<Biome>) rawHolder;
        // Only intercept the Plains fallback produced by DH's NBT chunk parser; leave real biomes intact.
        if (!currentBiome.is(Biomes.PLAINS)) {
            return;
        }
        if (ev.currentBlock == null || !(ev.currentBlock.getWrappedMcObject() instanceof BlockState state)) {
            return;
        }
        // Only override biome-tinted surface, foliage, and water blocks so deep underground strata
        // (stone/dirt/air) stay coalesced instead of fragmenting LOD vertical slices.
        if (classifyTintChannel(state) == TintChannel.NONE) {
            return;
        }

        IDhApiWrapperFactory factory = DhApi.Delayed.wrapperFactory;
        if (factory == null) {
            return;
        }
        ElderwoodsBiomeSource biomeSource = resolveBiomeSource(ev.levelWrapper);
        if (biomeSource == null) {
            return;
        }

        int worldX = (ev.chunkX << 4) + ev.relativeBlockPosX;
        int worldZ = (ev.chunkZ << 4) + ev.relativeBlockPosZ;
        Holder<Biome> realBiome = biomeSource.getBlockBiome(worldX, ev.blockPosY, worldZ);
        if (realBiome == null || realBiome.is(Biomes.PLAINS)) {
            return;
        }

        IDhApiBiomeWrapper replacement = getOrWrapBiome(factory, ev.levelWrapper, biomeSource, realBiome);
        if (replacement != null) {
            ev.setBiomeOverride(replacement);
        }
    }

    private static ElderwoodsBiomeSource resolveBiomeSource(IDhApiLevelWrapper levelWrapper) {
        Object mcLevel = levelWrapper.getWrappedMcObject();
        if (mcLevel instanceof ServerLevel serverLevel
                && serverLevel.getChunkSource().getGenerator().getBiomeSource() instanceof ElderwoodsBiomeSource ebs) {
            return ebs;
        }
        Supplier<ElderwoodsBiomeSource> fallback = clientFallbackSupplier;
        return fallback != null ? fallback.get() : null;
    }

    private static IDhApiBiomeWrapper getOrWrapBiome(IDhApiWrapperFactory factory, IDhApiLevelWrapper levelWrapper,
                                                      ElderwoodsBiomeSource source, Holder<Biome> biomeHolder) {
        ResourceLocation biomeId = biomeHolder.unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeId == null) {
            return null;
        }
        if (cachedSourceIdentity != source) {
            synchronized (BIOME_WRAPPER_CACHE) {
                if (cachedSourceIdentity != source) {
                    BIOME_WRAPPER_CACHE.clear();
                    cachedSourceIdentity = source;
                }
            }
        }
        IDhApiBiomeWrapper cached = BIOME_WRAPPER_CACHE.get(biomeId);
        if (cached != null) {
            return cached;
        }
        try {
            IDhApiBiomeWrapper created = factory.getBiomeWrapper(biomeId.toString(), levelWrapper);
            if (created != null) {
                BIOME_WRAPPER_CACHE.putIfAbsent(biomeId, created);
            }
            return created;
        } catch (Exception e) {
            if (!loggedWrapperFailure) {
                loggedWrapperFailure = true;
                OririMod.LOGGER.warn("Failed to resolve DH biome wrapper for {}: {}", biomeId, e.getMessage());
            }
            return null;
        }
    }
}
