package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.FireZombieEntity;
import net.ganyusbathwater.oririmod.entity.custom.SporeZombieEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

        public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE,
                        OririMod.MOD_ID);

        public static final DeferredHolder<EntityType<?>, EntityType<MagicBoltEntity>> MAGIC_BOLT = ENTITIES.register(
                        "magic_bolt",
                        () -> EntityType.Builder.<MagicBoltEntity>of(MagicBoltEntity::new, MobCategory.MISC)
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(64)
                                        .updateInterval(1)
                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "magic_bolt")
                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<FireballProjectileEntity>> FIREBALL_PROJECTILE = ENTITIES
                        .register(
                                        "fireball_projectile",
                                        () -> EntityType.Builder
                                                        .<FireballProjectileEntity>of(FireballProjectileEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.25F, 0.25F)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "fireball_projectile")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR = ENTITIES.register("meteor",
                        () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(64)
                                        .updateInterval(10)
                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "meteor")
                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<MagicProjectileEntity>> MAGIC_PROJECTILE = ENTITIES.register(
                        "magic_projectile",
                        () -> EntityType.Builder.<MagicProjectileEntity>of(MagicProjectileEntity::new, MobCategory.MISC)
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(64)
                                        .updateInterval(1)
                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "magic_projectile")
                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.RootVisualEntity>> ROOT_VISUAL = ENTITIES
                        .register("root_visual",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.RootVisualEntity>of(
                                                        net.ganyusbathwater.oririmod.entity.RootVisualEntity::new,
                                                        MobCategory.MISC)
                                                        .sized(1f, 1f)
                                                        .clientTrackingRange(10)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "root_visual")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.EyeOfTheStormEntity>> EYE_OF_THE_STORM = ENTITIES
                        .register("eye_of_the_storm",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.EyeOfTheStormEntity>of(
                                                        net.ganyusbathwater.oririmod.entity.EyeOfTheStormEntity::new,
                                                        MobCategory.MISC)
                                                        .sized(1f, 1f)
                                                        .clientTrackingRange(64)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "eye_of_the_storm")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<IcicleEntity>> ICICLE = ENTITIES.register("icicle",
                        () -> EntityType.Builder.<IcicleEntity>of(IcicleEntity::new, MobCategory.MISC)
                                        .sized(0.5F, 1.0F)
                                        .clientTrackingRange(64)
                                        .updateInterval(1)
                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "icicle")
                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<SwordProjectileEntity>> SWORD_PROJECTILE = ENTITIES
                        .register("sword_projectile",
                                        () -> EntityType.Builder
                                                        .<SwordProjectileEntity>of(SwordProjectileEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.5F, 0.5F)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "sword_projectile")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<DoomClockEntity>> DOOM_CLOCK = ENTITIES
                        .register("doom_clock",
                                        () -> EntityType.Builder
                                                        .<DoomClockEntity>of(DoomClockEntity::new, MobCategory.MISC)
                                                        .sized(2.0f, 2.0f)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "doom_clock")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<LaserBeamEntity>> LASER_BEAM = ENTITIES
                        .register("laser_beam",
                                        () -> EntityType.Builder
                                                        .<LaserBeamEntity>of(LaserBeamEntity::new, MobCategory.MISC)
                                                        // Bounding box is at the beam midpoint; tracking range covers long beams
                                                        .sized(0.5f, 0.5f)
                                                        .clientTrackingRange(128)
                                                        .updateInterval(1)  // sync every tick for smooth client rendering
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "laser_beam")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.MagicWaveEntity>> MAGIC_WAVE = ENTITIES
                        .register("magic_wave",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.MagicWaveEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.MagicWaveEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(1.5f, 0.5f)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "magic_wave")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<FireZombieEntity>> FIRE_ZOMBIE = ENTITIES
                        .register("fire_zombie",
                                        () -> EntityType.Builder.<FireZombieEntity>of(FireZombieEntity::new,
                                                        MobCategory.MONSTER)
                                                        .sized(0.6F, 1.95F)
                                                        .clientTrackingRange(8)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "fire_zombie")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<SporeZombieEntity>> SPORE_ZOMBIE = ENTITIES
                        .register("spore_zombie",
                                        () -> EntityType.Builder.<SporeZombieEntity>of(SporeZombieEntity::new,
                                                        MobCategory.MONSTER)
                                                        .sized(0.6F, 1.95F)
                                                        .clientTrackingRange(8)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "spore_zombie")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity>> EYE_OF_DESOLATION = ENTITIES
                        .register("eye_of_desolation",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity::new,
                                                                        MobCategory.MONSTER)
                                                        .sized(1.5F, 1.8F)
                                                        .eyeHeight(1.5F)
                                                        .clientTrackingRange(32)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "eye_of_desolation")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity>> BLIZZA = ENTITIES
                        .register("blizza",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity::new,
                                                                        MobCategory.MONSTER)
                                                        .sized(0.8F, 2.5F)
                                                        .clientTrackingRange(64)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "blizza")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.SplinterSpiderEntity>> SPLINTER_SPIDER = ENTITIES
                        .register("splinter_spider",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.SplinterSpiderEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.SplinterSpiderEntity::new,
                                                                        MobCategory.MONSTER)
                                                        .sized(1.4F, 0.9F)
                                                        .clientTrackingRange(8)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "splinter_spider")
                                                                        .toString()));

        public static void register(IEventBus modBus) {
                ENTITIES.register(modBus);
        }
}
