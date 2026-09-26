package net.ganyusbathwater.oririmod.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
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

        int playerMax = ModManaUtil.getMaxMana(player);
        if (ModManaUtil.getMana(player) > playerMax) {
            ModManaUtil.setMana(player, playerMax);
        }

        ModManaUtil.tick(player);

        // ── Cold Aura Defrost System ──
        net.minecraft.world.effect.MobEffectInstance coldAura = player.getEffect(ModEffects.COLD_AURA_EFFECT);
        if (coldAura != null) {
            long lastBlizz = player.getPersistentData().getLong("LastBlizzardTick");
            long gameTime = player.level().getGameTime();
            
            // Only defrost if they are safely outside the active blizzard control loop
            if (gameTime - lastBlizz > 5) {
                // Is the player near an active light source (ambient light > 11, like lava, torches, lanterns)?
                boolean fastThaw = player.level().getLightEngine().getRawBrightness(player.blockPosition(), 0) > 11;
                int currentAmp = coldAura.getAmplifier();
                int defrostProgress = player.getPersistentData().getInt("ColdAuraDefrostProg");
                
                // Advanced Math: Base = 200 ticks (10 seconds)
                // Fast = Roughly 30 ticks (1.5 seconds)
                defrostProgress += fastThaw ? 6 : 1; 
                
                if (defrostProgress >= 200) {
                    player.getPersistentData().putInt("ColdAuraDefrostProg", 0);
                    player.removeEffect(ModEffects.COLD_AURA_EFFECT); // Safely strip current layer
                    // Re-apply if drops below 0? No, let it organically drop to 0 and disappear.
                    if (currentAmp > 0) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                ModEffects.COLD_AURA_EFFECT, 200, currentAmp - 1, false, true, true));
                    }
                } else {
                    player.getPersistentData().putInt("ColdAuraDefrostProg", defrostProgress);
                }
            } else {
                // Trapped in blizzard: Re-freeze/reset any active thaw
                player.getPersistentData().putInt("ColdAuraDefrostProg", 0);
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
            // Sync world event state to clear/update client state for the new dimension
            NetworkHandler.sendWorldEventToPlayer(sp, WorldEventManager.getActiveEvent(sp.level()),
                    WorldEventManager.getTicksRemaining(sp.level()), WorldEventManager.getEventDuration(sp.level()));
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

    // Effekt: Erhöhte Spawnrate für Blutmond & Infused Enemies
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Level level = event.getLevel().getLevel();
        if (level instanceof ServerLevel serverLevel && event.getEntity() instanceof Monster monster) {
            if (WorldEventManager.isEventActive(level, WorldEventType.BLOOD_MOON)) {
                // 25% Chance für einen "Infused" Gegner mit erhöhten Werten
                if (serverLevel.random.nextFloat() < 0.25F) {
                    monster.setCustomName(net.minecraft.network.chat.Component.literal("Infused " + monster.getName().getString()));
                    monster.setCustomNameVisible(true);
                    
                    var maxHealth = monster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                    if (maxHealth != null) maxHealth.setBaseValue(maxHealth.getBaseValue() * 2.0);
                    
                    var attackDamage = monster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    if (attackDamage != null) attackDamage.setBaseValue(attackDamage.getBaseValue() * 1.5);
                    
                    var speed = monster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.2);
                    
                    monster.setHealth(monster.getMaxHealth());
                } else if (serverLevel.random.nextFloat() < 0.5F) {
                    // Verdoppelt die Chance, indem ein zweites Monster mit 50% Wahrscheinlichkeit gespawnt wird
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

        // Issue #13: Boost EvokerFangs damage by +2 hearts (4.0f) if owned by Blizza
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs) {
            LivingEntity owner = fangs.getOwner();
            if (owner != null) {
                if (owner instanceof net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity) {
                    event.setNewDamage(event.getNewDamage() + 4.0f);
                }
            }
        }

        // Reduce Meteor damage to prevent it from one-shotting bosses while keeping the huge explosion crater
        if (event.getSource().getDirectEntity() instanceof net.ganyusbathwater.oririmod.entity.MeteorEntity) {
            // Cap damage to 20 (10 hearts) and heavily reduce the base multiplier.
            event.setNewDamage(Math.min(event.getNewDamage() * 0.15f, 20.0f));
        }

        // Molten Armor Counter-attack
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player target && sourceEntity instanceof net.minecraft.world.entity.LivingEntity attacker) {
            if (!target.level().isClientSide() && target.getInventory().getArmor(0).getItem() instanceof net.ganyusbathwater.oririmod.item.custom.ModArmorItem armorItem) {
                if (armorItem.hasPlayerCorrectArmorOn(net.ganyusbathwater.oririmod.item.custom.ModArmorMaterials.MOLTEN_ARMOR_MATERIAL, target)) {
                    attacker.igniteForSeconds(5.0f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof net.minecraft.world.entity.player.Player player && !player.level().isClientSide() && event.getSource().getDirectEntity() == player) {
            net.minecraft.world.item.ItemStack weapon = player.getMainHandItem();
            if (weapon.getItem() instanceof net.ganyusbathwater.oririmod.item.custom.SolsEmbraceItem) {
                net.minecraft.world.entity.LivingEntity target = event.getEntity();
                net.minecraft.world.level.Level level = target.level();
                
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // Visual explosion
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 
                        target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 
                        1, 0, 0, 0, 0);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), 
                        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                
                // AOE Damage
                java.util.List<net.minecraft.world.entity.LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class, 
                    target.getBoundingBox().inflate(4.0D)
                );
                
                for (net.minecraft.world.entity.LivingEntity e : nearbyEntities) {
                    if (e == target || e == player) continue;
                    
                    // Damage if aggressive or neutral in combat
                    boolean isHostile = e instanceof net.minecraft.world.entity.monster.Enemy;
                    boolean inCombat = e.getLastHurtByMob() != null;
                    if (e instanceof net.minecraft.world.entity.Mob mob) {
                        if (mob.getTarget() != null) inCombat = true;
                    }
                    
                    if (isHostile || inCombat) {
                        e.hurt(level.damageSources().explosion(target, player), 10.0F);
                        e.setRemainingFireTicks(100);
                    }
                }
            }
        }

        // Soul Tithe: If a player dies, heal nearby bosses
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player deadPlayer && !deadPlayer.level().isClientSide()) {
            net.minecraft.world.level.Level level = deadPlayer.level();
            java.util.List<net.minecraft.world.entity.LivingEntity> entities = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, 
                deadPlayer.getBoundingBox().inflate(64.0D)
            );
            for (net.minecraft.world.entity.LivingEntity e : entities) {
                if (e instanceof net.ganyusbathwater.oririmod.entity.custom.IOririBoss boss) {
                    boss.healFromSoulTithe();
                }
            }
        }
    }


    @SubscribeEvent
    public static void onLivingIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs) {
            LivingEntity owner = fangs.getOwner();
            if (owner != null) {
                boolean isOwnerNoxus = owner.getType().is(net.ganyusbathwater.oririmod.entity.custom.NoxusKnightEntity.NOXUS_MOBS) || owner.getPersistentData().getBoolean("IsNoxusMob");
                Entity target = event.getEntity();
                if (isOwnerNoxus && (target.getType().is(net.ganyusbathwater.oririmod.entity.custom.NoxusKnightEntity.NOXUS_MOBS) || target.getPersistentData().getBoolean("IsNoxusMob") || target == owner)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();

        // Oraphim Bow Whirlwind Ability (Only for fully charged shots)
        if (!projectile.level().isClientSide() && projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            net.minecraft.world.item.ItemStack weapon = null;
            try {
                weapon = arrow.getWeaponItem();
            } catch (Throwable t) {
                // Ignore, weapon item might not be accessible
            }
            if (weapon != null && weapon.is(net.ganyusbathwater.oririmod.item.ModItems.ORAPHIM_BOW.get()) && arrow.isCritArrow()) {
                net.minecraft.world.level.Level level = arrow.level();
                net.minecraft.world.phys.Vec3 epicenter = arrow.position();

                // Spawn visually stunning server-sided math-based vortex particles
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 40; i++) {
                        double theta = serverLevel.random.nextDouble() * 2 * Math.PI;
                        double radius = serverLevel.random.nextDouble() * 5.0; // 5 block radius visual
                        double pX = epicenter.x + radius * Math.cos(theta);
                        double pY = epicenter.y + serverLevel.random.nextDouble() * 2.0;
                        double pZ = epicenter.z + radius * Math.sin(theta);
                        
                        // Calculate swirl velocity
                        double vX = -Math.sin(theta) * 0.5 + (epicenter.x - pX) * 0.1;
                        double vY = 0.1;
                        double vZ = Math.cos(theta) * 0.5 + (epicenter.z - pZ) * 0.1;
                        
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, pX, pY, pZ, 0, vX, vY, vZ, 1.0);
                        if (i % 4 == 0) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, pX, pY, pZ, 1, 0, 0, 0, 0);
                        }
                    }
                    serverLevel.playSound(null, epicenter.x, epicenter.y, epicenter.z, net.minecraft.sounds.SoundEvents.WITHER_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
                }

                // Entity Suction
                java.util.List<Entity> entities = level.getEntities(arrow, arrow.getBoundingBox().inflate(5.0));
                for (Entity entity : entities) {
                    if (entity instanceof LivingEntity living && entity != arrow.getOwner()) {
                        double distSq = epicenter.distanceToSqr(living.position());
                        if (distSq <= 25.0) { // 5 blocks radius squared
                            double dist = Math.sqrt(distSq);
                            double pullStrength = 1.0 - (dist / 5.0);
                            pullStrength = Math.pow(pullStrength, 1.5);
                            double kbResist = living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
                            double actualPull = pullStrength * 6 * (1.0 - kbResist);

                            if (actualPull > 0) {
                                net.minecraft.world.phys.Vec3 dir = epicenter.subtract(living.position()).normalize();
                                // Add a slight upward lift (0.3) so ground friction doesn't immediately cancel the pull
                                net.minecraft.world.phys.Vec3 pullVelocity = dir.scale(actualPull).add(0, 0.3, 0);
                                living.setDeltaMovement(living.getDeltaMovement().add(pullVelocity));
                                living.hasImpulse = true;
                                if (living instanceof Player player) {
                                    living.hurtMarked = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (event.getRayTraceResult() instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();

            if (hitEntity instanceof LivingEntity blocker && blocker.isBlocking()) {
                ItemStack useItem = blocker.getUseItem();
                if (useItem.is(net.ganyusbathwater.oririmod.item.ModItems.JADE_SHIELD.get())) {
                    OririMod.LOGGER.debug("Jade Shield blocked projectile: {}", event.getProjectile());

                    if (blocker.level().isClientSide())
                        return;

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
    public static void onShieldBlock(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event) {
        if (event.getDamageSource().getEntity() instanceof LivingEntity attacker) {
            if (attacker.getMainHandItem().getItem() instanceof net.ganyusbathwater.oririmod.item.custom.CustomScytheItem) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onArrowJoinLevel(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
            Entity shooter = arrow.getOwner();
            if (shooter instanceof LivingEntity livingShooter) {
                net.minecraft.world.item.ItemStack weapon = null;
                try {
                    weapon = arrow.getWeaponItem(); 
                } catch (Throwable t) {
                    weapon = livingShooter.getMainHandItem();
                }
                
                boolean isArcus = (weapon != null && weapon.is(net.ganyusbathwater.oririmod.item.ModItems.ARCUS_LUCIS.get())) ||
                                  livingShooter.getOffhandItem().is(net.ganyusbathwater.oririmod.item.ModItems.ARCUS_LUCIS.get());

                if (isArcus) {
                    if (arrow.getClass() == net.minecraft.world.entity.projectile.Arrow.class) {
                        event.setCanceled(true);
                        net.minecraft.world.entity.projectile.SpectralArrow spectral = new net.minecraft.world.entity.projectile.SpectralArrow(
                            arrow.level(), 
                            livingShooter, 
                            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPECTRAL_ARROW),
                            weapon
                        );
                        spectral.copyPosition(arrow);
                        spectral.setDeltaMovement(arrow.getDeltaMovement());
                        spectral.setYRot(arrow.getYRot());
                        spectral.setXRot(arrow.getXRot());
                        spectral.getPersistentData().putBoolean("ArcusLucisHoming", true);
                        arrow.level().addFreshEntity(spectral);
                        return;
                    }
                    arrow.getPersistentData().putBoolean("ArcusLucisHoming", true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow && !arrow.level().isClientSide) {
            if (arrow.getPersistentData().getBoolean("ArcusLucisHoming")) {
                if (arrow.getDeltaMovement().lengthSqr() < 0.05) return; // Stop homing if it has stopped moving (e.g., stuck in ground)
                
                // Homing Logic
                net.minecraft.world.phys.AABB scanBox = arrow.getBoundingBox().inflate(15.0D);
                java.util.List<LivingEntity> entities = arrow.level().getEntitiesOfClass(LivingEntity.class, scanBox, 
                        e -> e != arrow.getOwner() && e.isAlive() && !(e instanceof net.minecraft.world.entity.animal.Animal));
                
                LivingEntity bestTarget = null;
                double bestScore = Double.MAX_VALUE;
                net.minecraft.world.phys.Vec3 arrowPos = arrow.position();
                net.minecraft.world.phys.Vec3 currentDir = arrow.getDeltaMovement().normalize();

                for (LivingEntity e : entities) {
                    net.minecraft.world.phys.Vec3 targetCenter = e.getBoundingBox().getCenter();
                    net.minecraft.world.phys.Vec3 dirToTarget = targetCenter.subtract(arrowPos).normalize();
                    double dotProduct = currentDir.dot(dirToTarget);
                    
                    // Only target entities roughly in front of the arrow (dot > 0.4 means within ~66 degrees)
                    if (dotProduct > 0.4) {
                        // Check Line of Sight
                        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
                                arrowPos, targetCenter, net.minecraft.world.level.ClipContext.Block.COLLIDER, 
                                net.minecraft.world.level.ClipContext.Fluid.NONE, arrow);
                        net.minecraft.world.phys.HitResult hitResult = arrow.level().clip(context);
                        
                        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                            double dist = arrow.distanceToSqr(e);
                            // Score based on distance and how directly we are aiming at it
                            double score = dist / dotProduct; 
                            if (score < bestScore) {
                                bestScore = score;
                                bestTarget = e;
                            }
                        }
                    }
                }

                if (bestTarget != null) {
                    net.minecraft.world.phys.Vec3 targetPos = bestTarget.getBoundingBox().getCenter();
                    net.minecraft.world.phys.Vec3 desiredDir = targetPos.subtract(arrowPos).normalize();
                    
                    // Interpolate towards target (0.2 is turning speed)
                    net.minecraft.world.phys.Vec3 newDir = currentDir.scale(0.8).add(desiredDir.scale(0.2)).normalize();
                    double speed = arrow.getDeltaMovement().length();
                    if (speed < 0.1) speed = 1.0; 
                    
                    arrow.setDeltaMovement(newDir.scale(speed));
                    
                    // Update rotation for the client
                    double horizDist = newDir.horizontalDistance();
                    arrow.setYRot((float)(net.minecraft.util.Mth.atan2(newDir.x, newDir.z) * (double)(180F / (float)Math.PI)));
                    arrow.setXRot((float)(net.minecraft.util.Mth.atan2(newDir.y, horizDist) * (double)(180F / (float)Math.PI)));
                    arrow.yRotO = arrow.getYRot();
                    arrow.xRotO = arrow.getXRot();
                    
                    // Force velocity sync to client to prevent visual rubberbanding!
                    arrow.hasImpulse = true;
                    
                    // Spawn particles trail
                    if (arrow.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, 
                            arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.0, 0.0, 0.0, 0.01);
                    }
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
            
        // ----- Dog Tag Mechanism -----
        if (entity instanceof net.minecraft.world.entity.animal.Wolf wolf) {
            if (wolf.isTame() && wolf.hasCustomName()) {
                ItemStack dogTag = new ItemStack(net.ganyusbathwater.oririmod.item.ModItems.DOG_TAG.get());
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                wolf.saveWithoutId(tag);
                tag.remove("active_effects"); // Clear mob effects so they don't persist on revival
                dogTag.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                dogTag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, wolf.getCustomName());
                
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        wolf.level(), wolf.getX(), wolf.getY(), wolf.getZ(), dogTag);
                event.getDrops().add(drop);
            }
        }
        // -----------------------------

        // ----- Arbiter Crossbow Mechanism -----
        if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            net.minecraft.world.item.ItemStack weapon = null;
            LivingEntity attacker = null;
            
            if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
                attacker = arrow.getOwner() instanceof LivingEntity ? (LivingEntity) arrow.getOwner() : null;
                try {
                    weapon = arrow.getWeaponItem();
                } catch (Throwable t) {}
            } else if (event.getSource().getDirectEntity() instanceof LivingEntity directAttacker) {
                attacker = directAttacker;
            }
            
            if (attacker != null && (weapon == null || weapon.isEmpty())) {
                weapon = attacker.getMainHandItem();
                if (weapon == null || weapon.isEmpty() || !weapon.is(net.ganyusbathwater.oririmod.item.ModItems.ARBITER_CROSSBOW.get())) {
                    weapon = attacker.getOffhandItem();
                }
            }
                
            if (weapon != null && weapon.is(net.ganyusbathwater.oririmod.item.ModItems.ARBITER_CROSSBOW.get())) {
                    net.ganyusbathwater.oririmod.OririMod.LOGGER.debug("Arbiter Crossbow loot roll triggered on {}", entity.getName().getString());
                    double luck = attacker != null ? attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK) : 0;
                    
                    // 50% Double Loot
                    if (level.random.nextFloat() < 0.5f) {
                        net.ganyusbathwater.oririmod.OririMod.LOGGER.debug("Arbiter Crossbow rolled DOUBLE LOOT!");
                        List<ItemEntity> originalDrops = new ArrayList<>(event.getDrops());
                        for (ItemEntity drop : originalDrops) {
                            ItemStack stack = drop.getItem().copy();
                            ItemEntity extra = new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), stack);
                            event.getDrops().add(extra);
                        }
                    }
                    
                    // Independent Rare Metal drops scaling with luck (+1% flat chance per luck point)
                    float ironChance = 0.02f + (float)(luck * 0.01f);
                    float goldChance = 0.01f + (float)(luck * 0.01f);
                    float diamondChance = 0.005f + (float)(luck * 0.01f);
                    
                    if (level.random.nextFloat() < ironChance) {
                        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)));
                    }
                    if (level.random.nextFloat() < goldChance) {
                        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT)));
                    }
                    if (level.random.nextFloat() < diamondChance) {
                        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(net.minecraft.world.item.Items.DIAMOND)));
                    }
                }
        }
        // --------------------------------------

        if (!WorldEventManager.isEventActive(entity.level(), WorldEventType.GREEN_MOON))
            return;

        // 33 % Chance, Loot zu verdoppeln
        if (level.random.nextFloat() < 0.5f) {

            List<ItemEntity> originalDrops = new ArrayList<>(event.getDrops());

            for (ItemEntity drop : originalDrops) {
                // If it's a dog tag we just injected, this might duplicate it, but let's prevent duplicating dog tags.
                if (drop.getItem().is(net.ganyusbathwater.oririmod.item.ModItems.DOG_TAG.get())) continue;
                
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

        net.minecraft.world.level.chunk.ChunkAccess chunk = event.getChunk();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        // Optimized Mineshaft block swap: Restricted to relevant wood types and Y-levels
        // Using false for the third argument to setBlockState prevents light updates and physics loops.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Focus on the full vertical range where Mineshafts can generate in this dimension
                for (int y = -128; y <= 128; y++) {
                    pos.set(minX + x, y, minZ + z);
                    BlockState state = chunk.getBlockState(pos);
                    
                    if (state.is(net.minecraft.world.level.block.Blocks.OAK_PLANKS) || state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS)
                            || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS) || state.is(net.minecraft.world.level.block.Blocks.BIRCH_PLANKS)
                            || state.is(net.minecraft.world.level.block.Blocks.ACACIA_PLANKS) || state.is(net.minecraft.world.level.block.Blocks.JUNGLE_PLANKS)
                            || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_PLANKS) || state.is(net.minecraft.world.level.block.Blocks.CHERRY_PLANKS)
                            || state.is(net.minecraft.world.level.block.Blocks.BAMBOO_PLANKS)) {
                        chunk.setBlockState(pos, net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_PLANKS.get().defaultBlockState(), false);
                    } else if (state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE) || state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_FENCE)
                            || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_FENCE) || state.is(net.minecraft.world.level.block.Blocks.BIRCH_FENCE)
                            || state.is(net.minecraft.world.level.block.Blocks.ACACIA_FENCE) || state.is(net.minecraft.world.level.block.Blocks.JUNGLE_FENCE)
                            || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_FENCE) || state.is(net.minecraft.world.level.block.Blocks.CHERRY_FENCE)
                            || state.is(net.minecraft.world.level.block.Blocks.BAMBOO_FENCE) || state.is(net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE)
                            || state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_FENCE_GATE) || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_FENCE_GATE)
                            || state.is(net.minecraft.world.level.block.Blocks.BIRCH_FENCE_GATE) || state.is(net.minecraft.world.level.block.Blocks.ACACIA_FENCE_GATE)
                            || state.is(net.minecraft.world.level.block.Blocks.JUNGLE_FENCE_GATE) || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_FENCE_GATE)
                            || state.is(net.minecraft.world.level.block.Blocks.CHERRY_FENCE_GATE) || state.is(net.minecraft.world.level.block.Blocks.BAMBOO_FENCE_GATE)) {
                        chunk.setBlockState(pos, net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_FENCE.get().defaultBlockState(), false);
                    } else if (state.is(net.minecraft.world.level.block.Blocks.OAK_LOG) || state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_LOG)
                            || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LOG) || state.is(net.minecraft.world.level.block.Blocks.BIRCH_LOG)
                            || state.is(net.minecraft.world.level.block.Blocks.ACACIA_LOG) || state.is(net.minecraft.world.level.block.Blocks.JUNGLE_LOG)
                            || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_LOG) || state.is(net.minecraft.world.level.block.Blocks.CHERRY_LOG)
                            || state.is(net.minecraft.world.level.block.Blocks.OAK_WOOD) || state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_WOOD)
                            || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_WOOD) || state.is(net.minecraft.world.level.block.Blocks.BIRCH_WOOD)
                            || state.is(net.minecraft.world.level.block.Blocks.ACACIA_WOOD) || state.is(net.minecraft.world.level.block.Blocks.JUNGLE_WOOD)
                            || state.is(net.minecraft.world.level.block.Blocks.MANGROVE_WOOD) || state.is(net.minecraft.world.level.block.Blocks.CHERRY_WOOD)) {
                        chunk.setBlockState(pos, net.ganyusbathwater.oririmod.block.ModBlocks.ELDER_LOG_BLOCK.get().defaultBlockState(), false);
                    }
                }
            }
        }
    }
}