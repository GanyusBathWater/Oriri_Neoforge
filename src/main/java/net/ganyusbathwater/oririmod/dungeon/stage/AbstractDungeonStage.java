package net.ganyusbathwater.oririmod.dungeon.stage;

import net.ganyusbathwater.oririmod.dungeon.DungeonInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Shared base for all stage implementations.
 * Handles the common door-opening and area-modifier logic so each stage
 * impl only needs to worry about its own win condition.
 */
public abstract class AbstractDungeonStage implements DungeonStage {

    protected final StageDefinition definition;
    protected StageState state = StageState.PENDING;
    
    public static class InfiniteSpawnTracker {
        public int timer = 0;
        public final java.util.Set<java.util.UUID> activeMobs = new java.util.HashSet<>();
    }
    protected final java.util.Map<StageDefinition.InfiniteSpawnEntry, InfiniteSpawnTracker> infiniteSpawners = new java.util.HashMap<>();

    protected AbstractDungeonStage(StageDefinition definition) {
        this.definition = definition;
    }

    @Override
    public StageDefinition getDefinition() {
        return definition;
    }

    @Override
    public StageState getState() {
        return state;
    }
    
    public boolean shouldClearMobsOnComplete() {
        return true;
    }
    
    public void forceComplete() {
        this.state = StageState.COMPLETE;
    }

    @Override
    public void onStart(ServerLevel level, DungeonInstance instance) {
        if (definition.getTriggers().isEmpty()) {
            this.state = StageState.ACTIVE;
            applyStartEffects(level, instance);
            doStart(level, instance);
        } else {
            this.state = StageState.PENDING;
        }
    }

    /** Subclasses must implement this to do their specific start logic (spawning mobs, etc) */
    protected abstract void doStart(ServerLevel level, DungeonInstance instance);

    @Override
    public void tick(ServerLevel level, DungeonInstance instance) {
        if (state == StageState.PENDING) {
            // Check every 10 ticks for performance
            if (level.getGameTime() % 10 != 0) return;

            for (StageDefinition.TriggerEntry trigger : definition.getTriggers()) {
                BlockPos pos = trigger.pos();
                double r = trigger.radius();
                net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                    pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                    pos.getX() + r, pos.getY() + r, pos.getZ() + r
                );
                
                boolean playerInside = level.players().stream().anyMatch(p -> {
                    return instance.hasPlayer(p.getUUID()) && box.contains(p.position());
                });

                if (playerInside) {
                    this.state = StageState.ACTIVE;
                    
                    // Teleport any party members who are outside the trigger into the trigger zone
                    for (java.util.UUID pId : instance.getPlayers()) {
                        net.minecraft.server.level.ServerPlayer partyMember = level.getServer().getPlayerList().getPlayer(pId);
                        if (partyMember != null && !box.contains(partyMember.position())) {
                            partyMember.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, partyMember.getYRot(), partyMember.getXRot());
                        }
                    }
                    
                    applyStartEffects(level, instance);
                    doStart(level, instance);
                    break;
                }
            }
        } else if (state == StageState.ACTIVE) {
            tickInfiniteSpawners(level);
            doTick(level, instance);
        }
    }

    /** Subclasses must implement this to do their specific tick logic */
    protected abstract void doTick(ServerLevel level, DungeonInstance instance);
    
    protected void tickInfiniteSpawners(ServerLevel level) {
        for (StageDefinition.InfiniteSpawnEntry entry : definition.getInfiniteSpawns()) {
            InfiniteSpawnTracker tracker = infiniteSpawners.computeIfAbsent(entry, e -> new InfiniteSpawnTracker());
            
            // Clean up dead mobs
            tracker.activeMobs.removeIf(uuid -> {
                net.minecraft.world.entity.Entity ent = level.getEntity(uuid);
                return ent == null || !ent.isAlive();
            });
            
            // Cap at 5 active mobs per spawner
            if (tracker.activeMobs.size() >= 5) continue;
            
            tracker.timer++;
            if (tracker.timer >= entry.cooldownTicks()) {
                tracker.timer = 0;
                
                var type = resolveEntityType(level, entry.entityType(), entry.isTag());
                if (type == null) continue;
                
                if (level.getRandom().nextFloat() <= entry.chance()) {
                    var entity = type.create(level);
                    if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                        living.moveTo(entry.pos().getX() + 0.5, entry.pos().getY(), entry.pos().getZ() + 0.5,
                                level.getRandom().nextFloat() * 360f, 0f);
                        if (living instanceof net.minecraft.world.entity.Mob mob) {
                            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWNER, null);
                        }
                        level.addFreshEntity(living);
                        tracker.activeMobs.add(living.getUUID());
                    }
                }
            }
        }
    }

    @org.jetbrains.annotations.Nullable
    protected net.minecraft.world.entity.EntityType<?> resolveEntityType(ServerLevel level, ResourceLocation rl, boolean isTag) {
        if (isTag) {
            net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> tagKey = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, rl);
            var optionalList = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getTag(tagKey);
            if (optionalList.isPresent() && optionalList.get().size() > 0) {
                int index = level.getRandom().nextInt(optionalList.get().size());
                return optionalList.get().get(index).value();
            }
            return null;
        } else {
            return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
        }
    }

    /**
     * Executes all door lock effects by placing MAGIC_BARRIER_BLOCK
     */
    protected void applyStartEffects(ServerLevel level, DungeonInstance instance) {
        net.minecraft.world.level.block.Block magicBarrier = net.ganyusbathwater.oririmod.block.ModBlocks.MAGIC_BARRIER_BLOCK.get();
        if (magicBarrier == net.minecraft.world.level.block.Blocks.AIR) return; // Fallback if block not found
        
        for (StageDefinition.DoorEntry door : definition.getDoors()) {
            BlockPos pos = door.pos();
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) {
                level.setBlock(pos, magicBarrier.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Executes all door and area-modifier entries in the definition.
     * Called by subclasses once they determine their win condition is met.
     */
    protected void applyCompletionEffects(ServerLevel level, DungeonInstance instance) {
        // Open doors / remove barriers
        for (StageDefinition.DoorEntry door : definition.getDoors()) {
            BlockPos pos = door.pos();
            BlockState state = level.getBlockState(pos);
            // If it's any non-air block acting as a barrier, set it to air.
            // In Phase 5+ we will check for MagicBarrier specifically.
            if (!state.isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // Apply area modifiers (e.g. destroy floor)
        for (StageDefinition.AreaModifierEntry modifier : definition.getAreaModifiers()) {
            applyAreaModifier(level, modifier);
        }

        // Play a triumphant sound at the instance origin
        level.playSound(null, instance.getOrigin(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.AMBIENT, 1.0f, 1.0f);
    }

    private void applyAreaModifier(ServerLevel level, StageDefinition.AreaModifierEntry modifier) {
        BlockPos center = modifier.pos();
        int radius = modifier.radius();
        ResourceLocation filter = modifier.blockFilter();

        switch (modifier.action().toLowerCase()) {
            case "destroy" -> {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos target = center.offset(x, y, z);
                            BlockState state = level.getBlockState(target);
                            if (filter == null || matchesFilter(state, filter)) {
                                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
            case "fill" -> {
                if (filter != null) {
                    net.minecraft.world.level.block.Block fillBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(filter);
                    if (fillBlock != Blocks.AIR) {
                        for (int x = -radius; x <= radius; x++) {
                            for (int y = -radius; y <= radius; y++) {
                                for (int z = -radius; z <= radius; z++) {
                                    BlockPos target = center.offset(x, y, z);
                                    level.setBlock(target, fillBlock.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
            case "place_teleporter" -> {
                int rotation = net.minecraft.util.Mth.floor((double)((modifier.yRot() * 8.0F / 360.0F) + 0.5F)) & 7;
                BlockState teleporterState = net.ganyusbathwater.oririmod.block.ModBlocks.TELEPORTER_BLOCK.get().defaultBlockState()
                        .setValue(net.ganyusbathwater.oririmod.block.custom.TeleporterBlock.ROTATION, rotation);
                level.setBlock(center, teleporterState, 3);
                if (level.getBlockEntity(center) instanceof net.ganyusbathwater.oririmod.block.entity.TeleporterBlockEntity teleporter) {
                    teleporter.setTeleporterId(modifier.extraString(), null);
                }
            }
        }
    }

    private boolean matchesFilter(BlockState state, ResourceLocation filter) {
        return state.getBlock().builtInRegistryHolder().key().location().equals(filter);
    }
}
