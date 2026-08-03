package net.ganyusbathwater.oririmod.dungeon.entity;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * An invisible, non-collidable marker entity that is baked into dungeon NBT structures
 * to define stage goals, spawn points, boss triggers, switch locations, etc.
 *
 * <p>It carries a set of plain NBT string tags that the DungeonStageManager reads when
 * it scans the freshly-placed structure. All logic is data-driven:
 *
 * <pre>
 *  Tag           | Example value          | Meaning
 *  ------------- | ---------------------- | --------------------------------
 *  stage_id      | "stage_0"              | Which stage this marker belongs to
 *  stage_type    | "KILL_ALL_ENEMIES"     | Type of stage goal (see StageType)
 *  role          | "SPAWN_POINT"          | What this specific marker is for
 *  enemy_type    | "oririmod:noxus_knight"| Entity resource location (for spawn roles)
 *  count         | "5"                    | Numeric parameter (enemy count, etc.)
 *  switch_id     | "switch_a"            | Identifies a pressure-plate/button switch
 *  loot_table    | "oririmod:dungeon/..."  | Loot table path for FETCH_ITEM goals
 *  boss_id       | "blizza"               | Boss entity id for BOSS_FIGHT goals
 * </pre>
 *
 * <p>The marker is completely server-side and invisible on the client. It is removed
 * when the dungeon dimension is cleaned up after all players leave.
 */
public class DungeonMarkerEntity extends Entity {

    // -------------------------------------------------------------------------
    //  Synced data (keeps the stage_id readable on client for debug overlays)
    // -------------------------------------------------------------------------

    private static final EntityDataAccessor<String> DATA_STAGE_ID =
            SynchedEntityData.defineId(DungeonMarkerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_STAGE_TYPE =
            SynchedEntityData.defineId(DungeonMarkerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_ROLE =
            SynchedEntityData.defineId(DungeonMarkerEntity.class, EntityDataSerializers.STRING);

    // -------------------------------------------------------------------------
    //  NBT tag keys (constants so callers don't hard-code strings)
    // -------------------------------------------------------------------------

    public static final String TAG_STAGE_ID    = "stage_id";
    public static final String TAG_STAGE_TYPE  = "stage_type";
    public static final String TAG_ROLE        = "role";
    public static final String TAG_ENEMY_TYPE  = "enemy_type";
    public static final String TAG_COUNT       = "count";
    public static final String TAG_SWITCH_ID   = "switch_id";
    public static final String TAG_LOOT_TABLE  = "loot_table";
    public static final String TAG_BOSS_ID     = "boss_id";

    // Extra arbitrary NBT payload (holds enemy_type, count, loot_table, etc.)
    private CompoundTag extraData = new CompoundTag();

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    public DungeonMarkerEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Don't apply physics (gravity, collision)
        this.setInvisible(true);
    }

    // -------------------------------------------------------------------------
    //  EntityDataAccessor setup
    // -------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STAGE_ID,   "");
        builder.define(DATA_STAGE_TYPE, "");
        builder.define(DATA_ROLE,       "");
    }

    // -------------------------------------------------------------------------
    //  Accessors for the core identifying fields
    // -------------------------------------------------------------------------

    public String getStageId()   { return this.entityData.get(DATA_STAGE_ID);   }
    public String getStageType() { return this.entityData.get(DATA_STAGE_TYPE); }
    public String getRole()      { return this.entityData.get(DATA_ROLE);        }

    public void setStageId(String id)     { this.entityData.set(DATA_STAGE_ID,   id);   }
    public void setStageType(String type) { this.entityData.set(DATA_STAGE_TYPE, type); }
    public void setRole(String role)      { this.entityData.set(DATA_ROLE,       role); }

    /**
     * Returns the full extra-data compound tag.
     * Callers can fetch any sub-tag from it, e.g. {@code getExtraData().getString("enemy_type")}.
     */
    public CompoundTag getExtraData() { return extraData; }

    // -------------------------------------------------------------------------
    //  NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString(TAG_STAGE_ID,   getStageId());
        tag.putString(TAG_STAGE_TYPE, getStageType());
        tag.putString(TAG_ROLE,       getRole());
        tag.put("ExtraData", extraData.copy());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setStageId  (tag.getString(TAG_STAGE_ID));
        setStageType(tag.getString(TAG_STAGE_TYPE));
        setRole     (tag.getString(TAG_ROLE));
        if (tag.contains("ExtraData")) {
            extraData = tag.getCompound("ExtraData");
        }
        // Also read the individual shorthand keys for structure-baked NBT convenience:
        // Structure builders can put all keys flat in the root compound.
        if (tag.contains(TAG_ENEMY_TYPE))  extraData.putString(TAG_ENEMY_TYPE,  tag.getString(TAG_ENEMY_TYPE));
        if (tag.contains(TAG_COUNT))       extraData.putInt   (TAG_COUNT,        tag.getInt   (TAG_COUNT));
        if (tag.contains(TAG_SWITCH_ID))   extraData.putString(TAG_SWITCH_ID,   tag.getString(TAG_SWITCH_ID));
        if (tag.contains(TAG_LOOT_TABLE))  extraData.putString(TAG_LOOT_TABLE,  tag.getString(TAG_LOOT_TABLE));
        if (tag.contains(TAG_BOSS_ID))     extraData.putString(TAG_BOSS_ID,     tag.getString(TAG_BOSS_ID));
    }

    // -------------------------------------------------------------------------
    //  Entity behavior overrides — the marker does nothing
    // -------------------------------------------------------------------------

    @Override
    public boolean isInvisible() { return true; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isInvulnerable() { return true; }

    @Override
    public void tick() {
        // Markers don't tick — they just sit there and hold data.
    }

    // -------------------------------------------------------------------------
    //  Factory helper — creates a fully configured marker at the given position.
    // -------------------------------------------------------------------------

    /**
     * Creates and places a DungeonMarkerEntity at the given block-center position.
     *
     * @param level     the server level
     * @param x, y, z  block position
     * @param stageId   e.g. "stage_0"
     * @param stageType e.g. "KILL_ALL_ENEMIES"
     * @param role      e.g. "SPAWN_POINT"
     * @param extras    any additional tag pairs to store (enemy_type, count, …)
     * @return the spawned entity, or {@code null} if spawning failed
     */
    public static DungeonMarkerEntity create(
            Level level,
            double x, double y, double z,
            String stageId, String stageType, String role,
            CompoundTag extras) {

        DungeonMarkerEntity marker = new DungeonMarkerEntity(ModEntities.DUNGEON_MARKER.get(), level);
        marker.setPos(x + 0.5, y, z + 0.5);
        marker.setStageId(stageId);
        marker.setStageType(stageType);
        marker.setRole(role);
        if (extras != null) {
            marker.extraData = extras.copy();
        }
        level.addFreshEntity(marker);
        return marker;
    }
}
