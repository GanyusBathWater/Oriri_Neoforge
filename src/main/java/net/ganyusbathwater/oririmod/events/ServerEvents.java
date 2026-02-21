package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.config.OririConfig;
import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.ganyusbathwater.oririmod.effect.ModEffects;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class ServerEvents {

    private static final int NIGHT_START = 13000;
    private static final int DAY_START = 23500; // Kurz vor Tag 0

    // Erlaubte Monster, die während einer Sonnenfinsternis spawnen dürfen.
    private static final List<EntityType<? extends Monster>> ECLIPSE_MOBS = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.ENDERMAN);

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide())
            return;

        int intervalSeconds = ModManaUtil.getRegenIntervalSeconds(player);
        int regenAmount = OririConfig.COMMON.mana.regenAmount.get();
        int playerMax = ModManaUtil.getMaxMana(player);

        int intervalTicks = Math.max(1, intervalSeconds * 20);

        if (ModManaUtil.getMana(player) > playerMax) {
            ModManaUtil.setMana(player, playerMax);
        }

        ModManaUtil.incTickCounter(player);
        if (ModManaUtil.getTickCounter(player) >= intervalTicks) {
            ModManaUtil.setTickCounter(player, 0);
            if (ModManaUtil.getMana(player) < playerMax && regenAmount > 0) {
                ModManaUtil.addMana(player, regenAmount);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModManaUtil.syncToClient(sp);
            // Send current world event state to the player who just logged in
            NetworkHandler.sendWorldEventToPlayer(sp, WorldEventManager.getActiveEvent(sp.level()),
                    WorldEventManager.getTicksRemaining(sp.level()), WorldEventManager.getEventDuration(sp.level()));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModManaUtil.syncToClient(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModManaUtil.syncToClient(sp);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level == null)
            return;

        WorldEventManager manager = WorldEventManager.get(level);
        WorldEventManager.tick(level);

        long timeOfDay = level.getDayTime() % 24000;

        // Trigger für Nacht-Events (Blood Moon, Green Moon)
        if (timeOfDay == NIGHT_START) {
            if (level.random.nextFloat() < OririConfig.COMMON.worldEvents.nightEventChance.get()) {
                int duration = OririConfig.COMMON.worldEvents.nightEventDuration.get();
                if (level.random.nextBoolean()) {
                    manager.startEvent(WorldEventType.BLOOD_MOON, duration, level);
                } else {
                    manager.startEvent(WorldEventType.GREEN_MOON, duration, level);
                }
            }
        }

        // Trigger für Tag-Events (Eclipse)
        if (timeOfDay == DAY_START) {
            if (level.random.nextFloat() < OririConfig.COMMON.worldEvents.eclipseChance.get()) {
                int duration = OririConfig.COMMON.worldEvents.eclipseDuration.get();
                manager.startEvent(WorldEventType.ECLIPSE, duration, level);
            }
        }
        if (WorldEventManager.isEventActive(level, WorldEventType.ECLIPSE)) {
            // alle 20 Ticks prüfen, geringe Chance pro Spieler
            if (level.getGameTime() % 20L == 0L) {
                for (ServerPlayer sp : level.players()) {
                    // Chance pro Spieler, anpassen nach Bedarf / Config
                    if (level.random.nextFloat() < 0.02F) {
                        EntityType<? extends Monster> chosen = ECLIPSE_MOBS
                                .get(level.random.nextInt(ECLIPSE_MOBS.size()));
                        Monster mob = (Monster) chosen.create(level);
                        if (mob != null) {
                            double px = sp.getX() + (level.random.nextDouble() - 0.5) * 16.0;
                            double pz = sp.getZ() + (level.random.nextDouble() - 0.5) * 16.0;
                            int py = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(px),
                                    (int) Math.floor(pz));
                            mob.moveTo(px, py, pz, level.random.nextFloat() * 360.0F, 0.0F);
                            level.addFreshEntity(mob);
                        }
                    }
                }
            }
        }
    }

    // Effekt: Erhöhte Spawnrate für Blutmond
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Level level = event.getLevel().getLevel();
        if (level instanceof ServerLevel serverLevel && event.getEntity() instanceof Monster monster) {
            if (WorldEventManager.isEventActive(level, WorldEventType.BLOOD_MOON)) {
                // Verdoppelt die Chance, indem ein zweites Monster mit 50% Wahrscheinlichkeit
                // gespawnt wird
                if (serverLevel.random.nextFloat() < 0.5F) {
                    Monster extraMonster = (Monster) monster.getType().create(serverLevel);
                    if (extraMonster != null) {
                        extraMonster.copyPosition(monster);
                        serverLevel.addFreshEntity(extraMonster);
                    }
                }
            }
        }
    }

    // Effekt: Erhöhter Schaden bei Sonnenfinsternis
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof Monster && sourceEntity.level() instanceof ServerLevel serverLevel) {
            if (WorldEventManager.isEventActive(serverLevel, WorldEventType.ECLIPSE)) {
                event.setNewDamage(event.getOriginalDamage() * 1.25F);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();

            if (hitEntity instanceof LivingEntity blocker && blocker.isBlocking()) {
                ItemStack useItem = blocker.getUseItem();
                if (useItem.is(net.ganyusbathwater.oririmod.item.ModItems.JADE_SHIELD.get())) {
                    OririMod.LOGGER.debug("Jade Shield blocked projectile: {}", event.getProjectile());

                    if (blocker.level().isClientSide())
                        return;

                    Projectile projectile = event.getProjectile();
                    Vec3 currentVelocity = projectile.getDeltaMovement();
                    double speed = Math.max(currentVelocity.length(), 1.0) * 1.5;

                    Entity shooter = projectile.getOwner();
                    Vec3 dir;

                    if (shooter != null) {
                        dir = shooter.position().add(0, shooter.getEyeHeight() / 2.0, 0)
                                .subtract(projectile.position()).normalize();
                        OririMod.LOGGER.debug("Shield reflecting back to shooter: {}", shooter);
                    } else {
                        dir = blocker.getLookAngle();
                        OririMod.LOGGER.debug("Shield reflecting forward (no shooter found)");
                    }

                    projectile.shoot(dir.x, dir.y, dir.z, (float) speed, 0.1f);
                    projectile.setDeltaMovement(dir.scale(speed));
                    projectile.hasImpulse = true;

                    if (projectile instanceof AbstractHurtingProjectile hurting) {
                        try {
                            java.lang.reflect.Method m = AbstractHurtingProjectile.class
                                    .getDeclaredMethod("assignDirectionalMovement", Vec3.class, double.class);
                            m.setAccessible(true);
                            m.invoke(hurting, dir.scale(0.1), Math.min(speed, 5.0));
                            OririMod.LOGGER.debug("Updated fireball directional movement via reflection");
                        } catch (Exception e) {
                            OririMod.LOGGER.error("Failed to update fireball directional movement", e);
                        }
                    }

                    projectile.setOwner(blocker);

                    blocker.level().playSound(null, blocker.blockPosition(), SoundEvents.ANVIL_PLACE,
                            SoundSource.PLAYERS, 0.5f, 2.0f);

                    event.setCanceled(true);
                    OririMod.LOGGER.debug("ProjectileImpactEvent canceled for reflection");
                }
            }
        }
    }

    @SubscribeEvent

    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Monster monster))
            return;
        if (!(monster.level() instanceof ServerLevel))
            return;
        if (!WorldEventManager.isEventActive(monster.level(), WorldEventType.ECLIPSE))
            return;

        // Sonnenbrand verhindern
        if (monster.isOnFire()) {
            monster.clearFire();
        }
    }

    /**
     * Blocks all healing when the entity has the Anti-Heal effect.
     * Works for both players and mobs.
     */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();

        // Check if the entity has the Anti-Heal effect
        if (entity.hasEffect(ModEffects.ANTI_HEAL_EFFECT)) {
            // Cancel the healing event completely
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        ServerLevel level = (ServerLevel) entity.level();

        if (entity == null)
            return;
        if (entity.level().isClientSide())
            return;
        if (!(entity.level() instanceof ServerLevel))
            return;
        if (!WorldEventManager.isEventActive(entity.level(), WorldEventType.GREEN_MOON))
            return;

        // 33 % Chance, Loot zu verdoppeln
        if (level.random.nextFloat() < 0.5f) {

            List<ItemEntity> originalDrops = new ArrayList<>(event.getDrops());

            for (ItemEntity drop : originalDrops) {
                ItemStack stack = drop.getItem().copy();
                stack.setCount(stack.getCount());
                ItemEntity extra = new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), stack);
                event.getDrops().add(extra);
            }
        }
    }

    /**
     * Process newly generated chunks in Elderwoods dimension:
     * - Swap mineshaft wood blocks to elder wood
     * - Remove water blocks from caves
     */
    @SubscribeEvent
    public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel))
            return;

        // Only process Elderwoods dimension
        if (!serverLevel.dimension().location().toString().equals("oririmod:elderwoods"))
            return;

        // Only process newly generated chunks (not loaded from disk)
        if (!event.isNewChunk())
            return;

        net.minecraft.world.level.chunk.ChunkAccess chunk = event.getChunk();
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();

        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;

                // Process from min to max height
                for (int y = serverLevel.getMinBuildHeight(); y < serverLevel.getMaxBuildHeight(); y++) {
                    pos.set(worldX, y, worldZ);
                    net.minecraft.world.level.block.state.BlockState currentBlock = chunk.getBlockState(pos);

                    // Swap mineshaft wood blocks to elder wood
                    if (currentBlock.is(net.minecraft.world.level.block.Blocks.OAK_PLANKS) ||
                            currentBlock.is(net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS) ||
                            currentBlock.is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS)) {
                        chunk.setBlockState(pos,
                                net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_PLANKS.get().defaultBlockState(),
                                false);
                    } else if (currentBlock.is(net.minecraft.world.level.block.Blocks.OAK_FENCE) ||
                            currentBlock.is(net.minecraft.world.level.block.Blocks.DARK_OAK_FENCE) ||
                            currentBlock.is(net.minecraft.world.level.block.Blocks.SPRUCE_FENCE)) {
                        chunk.setBlockState(pos,
                                net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_FENCE.get().defaultBlockState(),
                                false);
                    } else if (currentBlock.is(net.minecraft.world.level.block.Blocks.OAK_LOG) ||
                            currentBlock.is(net.minecraft.world.level.block.Blocks.DARK_OAK_LOG) ||
                            currentBlock.is(net.minecraft.world.level.block.Blocks.SPRUCE_LOG)) {
                        chunk.setBlockState(pos,
                                net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState(),
                                false);
                    }
                    // Remove water blocks
                    else if (currentBlock.is(net.minecraft.world.level.block.Blocks.WATER)) {
                        chunk.setBlockState(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}