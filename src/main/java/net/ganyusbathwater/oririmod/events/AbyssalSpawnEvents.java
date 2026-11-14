package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.entity.ForcefieldEmitterBlockEntity;
import net.ganyusbathwater.oririmod.entity.ai.FleeSilentDepthsGoal;
import net.ganyusbathwater.oririmod.worldgen.ElderwoodsBiomeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.List;

/**
 * Throttles overall natural mob spawning inside and around the Silent Abyss
 * ({@code the_silent_depths}, {@code the_sunken_trenches}, and adjacent aphotic waters)
 * uniformly across all entities rather than singling out individual mob weights.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public class AbyssalSpawnEvents {

    private static final float INSIDE_ABYSS_REJECT_CHANCE = 0.88F;
    private static final float NEAR_ABYSS_REJECT_CHANCE = 0.78F;
    private static final int MAX_MONSTERS_NEARBY = 3;
    private static final int MAX_PASSIVE_NEARBY = 5;
    private static final double MONSTER_DENSITY_RADIUS = 56.0;
    private static final double PASSIVE_DENSITY_RADIUS = 44.0;
    private static final int PERIMETER_CHECK_DISTANCE = 20;
    private static final int APHOTIC_WATER_Y_THRESHOLD = 44;

    private enum AbyssZone {
        NONE,
        NEAR,
        INSIDE
    }

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        if (!event.getPlacementCheckResult()) {
            return;
        }

        ServerLevelAccessor levelAccessor = event.getLevel();
        BlockPos pos = event.getPos();
        if (levelAccessor instanceof Level level && ForcefieldEmitterBlockEntity.isAttractingNearby(level, pos)) {
            return;
        }

        AbyssZone zone = classifyAbyssZone(levelAccessor, pos);
        if (zone == AbyssZone.NONE) {
            return;
        }

        float rejectChance = zone == AbyssZone.INSIDE ? INSIDE_ABYSS_REJECT_CHANCE : NEAR_ABYSS_REJECT_CHANCE;
        if (event.getRandom().nextFloat() < rejectChance) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }

        MobCategory category = event.getEntityType().getCategory();
        if (exceedsCategoryDensityCap(levelAccessor, pos, category, zone)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    private static AbyssZone classifyAbyssZone(ServerLevelAccessor level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        if (FleeSilentDepthsGoal.isAbyssalBiome(biome)) {
            return AbyssZone.INSIDE;
        }

        boolean isSilentOcean = biome.is(ElderwoodsBiomeSource.THE_SILENT_OCEANS_KEY);
        if (!isSilentOcean && pos.getY() > 48) {
            return AbyssZone.NONE;
        }

        if (isSilentOcean && pos.getY() <= APHOTIC_WATER_Y_THRESHOLD) {
            return AbyssZone.NEAR;
        }

        int abyssDepthY = Math.min(pos.getY(), 34);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Check vertically beneath upper silent ocean waters (3D strata above the trenches)
        int lowerY = Math.max(level.getMinBuildHeight() + 4, pos.getY() - PERIMETER_CHECK_DISTANCE);
        cursor.set(pos.getX(), lowerY, pos.getZ());
        if (isAbyssalAtLoaded(level, cursor)) {
            return AbyssZone.NEAR;
        }

        // Check loaded horizontal perimeter so trenches also suppress swarms on their rim
        int[][] offsets = {
                { PERIMETER_CHECK_DISTANCE, 0 },
                { -PERIMETER_CHECK_DISTANCE, 0 },
                { 0, PERIMETER_CHECK_DISTANCE },
                { 0, -PERIMETER_CHECK_DISTANCE }
        };
        for (int[] offset : offsets) {
            cursor.set(pos.getX() + offset[0], abyssDepthY, pos.getZ() + offset[1]);
            if (isAbyssalAtLoaded(level, cursor)) {
                return AbyssZone.NEAR;
            }
        }

        return AbyssZone.NONE;
    }

    private static boolean isAbyssalAtLoaded(ServerLevelAccessor level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        return FleeSilentDepthsGoal.isAbyssalBiome(level.getBiome(pos));
    }

    private static boolean exceedsCategoryDensityCap(
            ServerLevelAccessor level,
            BlockPos pos,
            MobCategory category,
            AbyssZone zone) {
        boolean hostile = category == MobCategory.MONSTER;
        double radius = hostile ? MONSTER_DENSITY_RADIUS : PASSIVE_DENSITY_RADIUS;
        int baseCap = hostile ? MAX_MONSTERS_NEARBY : MAX_PASSIVE_NEARBY;
        int cap = zone == AbyssZone.INSIDE ? baseCap : baseCap + 1;

        AABB box = new AABB(pos).inflate(radius);
        List<Mob> nearby = level.getEntitiesOfClass(
                Mob.class, box, mob -> mob.isAlive() && mob.getType().getCategory() == category);
        return nearby.size() >= cap;
    }
}

