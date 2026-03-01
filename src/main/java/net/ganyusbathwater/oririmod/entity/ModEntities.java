package net.ganyusbathwater.oririmod.entity;

import net.ganyusbathwater.oririmod.OririMod;
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

        public static void register(IEventBus modBus) {
                ENTITIES.register(modBus);
        }
}