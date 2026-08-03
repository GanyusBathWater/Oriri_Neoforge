package net.ganyusbathwater.oririmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Global saved data to track all active dungeon instances across the server.
 * Attached to the Overworld so it's always accessible.
 */
public class DungeonManager extends SavedData {
    private final Map<UUID, DungeonInstance> activeInstances = new HashMap<>();
    
    // Quick lookup mapping a player UUID to the instance UUID they are currently in
    private final Map<UUID, UUID> playerToInstance = new HashMap<>();

    // Tracks allocated 2048x2048 slots per dimension (key: dimension ResourceLocation string)
    private final Map<String, net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid> dimensionGrids = new HashMap<>();

    /**
     * Starts a new dungeon instance for a party of players.
     */
    public DungeonInstance startDungeon(ServerLevel overworld, DungeonDefinition definition, Set<ServerPlayer> party) {
        // 1. Get the target dimension
        ServerLevel dimensionLevel = overworld.getServer().getLevel(definition.dimension());
        if (dimensionLevel == null) {
            System.err.println("[OririMod] CRITICAL: Dungeon dimension not found: " + definition.dimension().location());
            return null;
        }
        
        // 2. Allocate grid slot
        String dimKey = definition.dimension().location().toString();
        net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid grid = getGrid(dimKey);
        BlockPos origin = grid.allocateSlot();
        
        // 3. Create Instance
        UUID instanceId = UUID.randomUUID();
        DungeonInstance instance = new DungeonInstance(instanceId, definition.id().toString(), origin);
        for (ServerPlayer p : party) {
            instance.addPlayer(p.getUUID());
        }
        
        // 4. Place Structure
        net.ganyusbathwater.oririmod.dungeon.dimension.DungeonDimensionManager.placeDungeonStructure(dimensionLevel, definition, instance);
        
        // 5. Register Instance in Manager
        this.addInstance(instance);
        
        // 6. Teleport Players
        for (ServerPlayer p : party) {
            net.ganyusbathwater.oririmod.dungeon.dimension.DungeonDimensionManager.teleportPlayerToDungeon(p, dimensionLevel, instance);
        }
        
        return instance;
    }

    public net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid getGrid(String dimensionKey) {
        return dimensionGrids.computeIfAbsent(dimensionKey, k -> new net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid());
    }

    public void addInstance(DungeonInstance instance) {
        activeInstances.put(instance.getInstanceId(), instance);
        for (UUID player : instance.getPlayers()) {
            playerToInstance.put(player, instance.getInstanceId());
        }
        setDirty();
    }
    
    public void removeInstance(net.minecraft.server.MinecraftServer server, UUID instanceId, String dimensionKey) {
        DungeonInstance instance = activeInstances.remove(instanceId);
        if (instance != null) {
            for (UUID player : instance.getPlayers()) {
                playerToInstance.remove(player);
            }
            if (dimensionKey != null) {
                getGrid(dimensionKey).freeSlot(instance.getOrigin());
            }
            
            // Phase 7: Chunk & Entity Cleanup
            var def = net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry.get(instance.getDungeonId());
            if (def != null) {
                ServerLevel dimLevel = server.getLevel(def.dimension());
                if (dimLevel != null && instance.getStructureBounds() != null) {
                    var bounds = instance.getStructureBounds();
                    
                    // Remove all entities
                    java.util.List<net.minecraft.world.entity.Entity> entities = dimLevel.getEntitiesOfClass(
                            net.minecraft.world.entity.Entity.class, 
                            new net.minecraft.world.phys.AABB(
                                    bounds.minX(), bounds.minY(), bounds.minZ(), 
                                    bounds.maxX(), bounds.maxY(), bounds.maxZ()
                            )
                    );
                    for (net.minecraft.world.entity.Entity e : entities) {
                        if (!(e instanceof net.minecraft.world.entity.player.Player)) {
                            e.discard();
                        }
                    }
                    
                    // Replace all blocks with AIR
                    BlockPos.betweenClosedStream(bounds.minX(), bounds.minY(), bounds.minZ(), 
                                                 bounds.maxX(), bounds.maxY(), bounds.maxZ())
                            .forEach(pos -> dimLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2));
                }
            }
            
            setDirty();
        }
    }

    public void updatePlayerMapping(DungeonInstance instance) {
        for (UUID player : instance.getPlayers()) {
            playerToInstance.put(player, instance.getInstanceId());
        }
        setDirty();
    }
    
    @Nullable
    public DungeonInstance getInstance(UUID instanceId) {
        return activeInstances.get(instanceId);
    }
    
    @Nullable
    public DungeonInstance getInstanceForPlayer(UUID playerId) {
        UUID instanceId = playerToInstance.get(playerId);
        return instanceId != null ? activeInstances.get(instanceId) : null;
    }
    
    public Map<UUID, DungeonInstance> getActiveInstances() {
        return activeInstances;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (DungeonInstance instance : activeInstances.values()) {
            list.add(instance.save(new CompoundTag()));
        }
        tag.put("Instances", list);

        CompoundTag gridsTag = new CompoundTag();
        for (Map.Entry<String, net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid> entry : dimensionGrids.entrySet()) {
            gridsTag.put(entry.getKey(), entry.getValue().save(new CompoundTag()));
        }
        tag.put("Grids", gridsTag);

        return tag;
    }

    public static DungeonManager load(CompoundTag tag, HolderLookup.Provider provider) {
        DungeonManager manager = new DungeonManager();
        if (tag.contains("Instances")) {
            ListTag list = tag.getList("Instances", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                DungeonInstance instance = DungeonInstance.load(list.getCompound(i));
                manager.addInstance(instance);
            }
        }
        if (tag.contains("Grids")) {
            CompoundTag gridsTag = tag.getCompound("Grids");
            for (String key : gridsTag.getAllKeys()) {
                net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid grid = new net.ganyusbathwater.oririmod.dungeon.dimension.DungeonInstanceGrid();
                grid.load(gridsTag.getCompound(key));
                manager.dimensionGrids.put(key, grid);
            }
        }
        return manager;
    }

    public static DungeonManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            return overworld.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(DungeonManager::new, DungeonManager::load),
                    "oririmod_dungeons"
            );
        }
        return new DungeonManager();
    }
}
