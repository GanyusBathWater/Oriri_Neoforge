package net.ganyusbathwater.oririmod.compat.dh;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-only Distant Horizons color override compatibility.
 * <p>
 * Repaints cached {@code minecraft:plains} LOD voxels already stored in a world's
 * {@code DistantHorizons.sqlite} file inside {@code oririmod:elderwoods} using the
 * integrated server's deterministic {@link ElderwoodsBiomeSource}.
 */
public final class OririDhClientCompat {

    private static volatile boolean clientRegistered = false;

    private OririDhClientCompat() {
    }

    /**
     * Registers client-side DH block wrapper &amp; color override handlers.
     */
    public static synchronized void registerClient() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;

        OririDistantHorizonsCompat.setClientFallbackSupplier(OririDhClientCompat::resolveIntegratedBiomeSource);

        DhApi.events.bind(DhApiBlockStateWrapperCreatedEvent.class, new DhApiBlockStateWrapperCreatedEvent() {
            @Override
            public void blockStateWrapperCreated(DhApiEventParam<EventParam> param) {
                if (param == null || param.value == null || param.value.getBlockStateWrapper() == null) {
                    return;
                }
                if (param.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState state
                        && OririDistantHorizonsCompat.classifyTintChannel(state) != OririDistantHorizonsCompat.TintChannel.NONE) {
                    param.value.setAllowApiColorOverride(true);
                }
            }
        });

        DhApi.events.bind(DhApiBlockColorOverrideEvent.class, new DhApiBlockColorOverrideEvent() {
            @Override
            public void onBlockColorOverridden(DhApiEventParam<EventParam> param) {
                handleBlockColorOverride(param);
            }
        });
    }

    private static void handleBlockColorOverride(DhApiEventParam<DhApiBlockColorOverrideEvent.EventParam> param) {
        if (param == null || param.value == null) {
            return;
        }
        DhApiBlockColorOverrideEvent.EventParam ev = param.value;
        if (ev.getLevelWrapper() == null
                || !OririDistantHorizonsCompat.ELDERWOODS_DIM_ID.equals(ev.getLevelWrapper().getDimensionName())) {
            return;
        }
        if (ev.getBiomeWrapper() == null || !(ev.getBiomeWrapper().getWrappedMcObject() instanceof Holder<?> rawHolder)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Holder<Biome> dhBiomeHolder = (Holder<Biome>) rawHolder;
        // If DH already stored the real biome in SQLite, native tinting is already exact.
        if (!dhBiomeHolder.is(Biomes.PLAINS)) {
            return;
        }
        if (ev.getBlockStateWrapper() == null
                || !(ev.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState state)) {
            return;
        }

        OririDistantHorizonsCompat.TintChannel channel = OririDistantHorizonsCompat.classifyTintChannel(state);
        if (channel == OririDistantHorizonsCompat.TintChannel.NONE) {
            return;
        }

        ElderwoodsBiomeSource biomeSource = resolveIntegratedBiomeSource();
        if (biomeSource == null) {
            return;
        }

        int x = ev.getBlockPosX();
        int y = ev.getBlockPosY();
        int z = ev.getBlockPosZ();
        Holder<Biome> realBiome = biomeSource.getBlockBiome(x, y, z);
        if (realBiome == null || realBiome.is(Biomes.PLAINS)) {
            return;
        }

        Biome plains = dhBiomeHolder.value();
        Biome target = realBiome.value();
        int oldTint;
        int newTint;
        switch (channel) {
            case GRASS -> {
                oldTint = plains.getGrassColor(x, z);
                newTint = target.getGrassColor(x, z);
            }
            case FOLIAGE -> {
                oldTint = plains.getFoliageColor();
                newTint = target.getFoliageColor();
            }
            case WATER -> {
                oldTint = plains.getWaterColor();
                newTint = target.getWaterColor();
            }
            default -> {
                return;
            }
        }
        if (oldTint == newTint) {
            return;
        }

        int r = OririDistantHorizonsCompat.retintChannel(ev.getRed(), (oldTint >>> 16) & 0xFF, (newTint >>> 16) & 0xFF);
        int g = OririDistantHorizonsCompat.retintChannel(ev.getGreen(), (oldTint >>> 8) & 0xFF, (newTint >>> 8) & 0xFF);
        int b = OririDistantHorizonsCompat.retintChannel(ev.getBlue(), oldTint & 0xFF, newTint & 0xFF);
        ev.setColor(r, g, b);
    }

    private static ElderwoodsBiomeSource resolveIntegratedBiomeSource() {
        IntegratedServer spServer = Minecraft.getInstance().getSingleplayerServer();
        if (spServer == null) {
            return null;
        }
        return ElderwoodsBiomeSource.getActiveInstance();
    }
}
