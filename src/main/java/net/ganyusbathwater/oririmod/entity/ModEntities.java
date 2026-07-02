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

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.GiantSwordEntity>> GIANT_SWORD = ENTITIES
                        .register("giant_sword",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.GiantSwordEntity>of(net.ganyusbathwater.oririmod.entity.custom.GiantSwordEntity::new, MobCategory.MISC)
                                                        .sized(1.0F, 1.0F)
                                                        .clientTrackingRange(128)
                                                        .updateInterval(1)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "giant_sword").toString()));


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

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.SporeBlossomEntity>> SPORE_BLOSSOM = ENTITIES
                        .register("spore_blossom",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.SporeBlossomEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.SporeBlossomEntity::new,
                                                                        MobCategory.MONSTER)
                                                        .sized(1.0F, 1.0F)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(3)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "spore_blossom")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity>> EYE_OF_DESOLATION = ENTITIES
                        .register("eye_of_desolation",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.EyeOfDesolationEntity::new,
                                                                        MobCategory.MONSTER)
                                                        .sized(1.0F, 1.5F)
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

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.VenomousPlantEntity>> VENOMOUS_PLANT = ENTITIES
                        .register("venomous_plant",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.VenomousPlantEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.VenomousPlantEntity::new,
                                                                        MobCategory.MONSTER)
                                                        .sized(1.0F, 1.5F)
                                                        .clientTrackingRange(32)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "venomous_plant")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.ThornProjectileEntity>> THORN_PROJECTILE = ENTITIES
                        .register("thorn_projectile",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.ThornProjectileEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.ThornProjectileEntity::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.3F, 0.3F)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "thorn_projectile")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.arrow.TntArrowEntity>> TNT_ARROW = ENTITIES
                        .register("tnt_arrow",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.custom.arrow.TntArrowEntity>of(net.ganyusbathwater.oririmod.entity.custom.arrow.TntArrowEntity::new, MobCategory.MISC)
                                                        .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "tnt_arrow").toString()));
        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.arrow.EventHorizonArrowEntity>> EVENT_HORIZON_ARROW = ENTITIES
                        .register("event_horizon_arrow",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.custom.arrow.EventHorizonArrowEntity>of(net.ganyusbathwater.oririmod.entity.custom.arrow.EventHorizonArrowEntity::new, MobCategory.MISC)
                                                        .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "event_horizon_arrow").toString()));
        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.arrow.DragonIronArrowEntity>> DRAGON_IRON_ARROW = ENTITIES
                        .register("dragon_iron_arrow",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.custom.arrow.DragonIronArrowEntity>of(net.ganyusbathwater.oririmod.entity.custom.arrow.DragonIronArrowEntity::new, MobCategory.MISC)
                                                        .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "dragon_iron_arrow").toString()));
        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.arrow.FrostArrowEntity>> FROST_ARROW = ENTITIES
                        .register("frost_arrow",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.custom.arrow.FrostArrowEntity>of(net.ganyusbathwater.oririmod.entity.custom.arrow.FrostArrowEntity::new, MobCategory.MISC)
                                                        .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "frost_arrow").toString()));
        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.arrow.CopperArrowEntity>> COPPER_ARROW = ENTITIES
                        .register("copper_arrow",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.custom.arrow.CopperArrowEntity>of(net.ganyusbathwater.oririmod.entity.custom.arrow.CopperArrowEntity::new, MobCategory.MISC)
                                                        .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "copper_arrow").toString()));
        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.arrow.SonicArrowEntity>> SONIC_ARROW = ENTITIES
                        .register("sonic_arrow",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.custom.arrow.SonicArrowEntity>of(net.ganyusbathwater.oririmod.entity.custom.arrow.SonicArrowEntity::new, MobCategory.MISC)
                                                        .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "sonic_arrow").toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity>> BLACK_HOLE = ENTITIES
                        .register("black_hole",
                                        () -> EntityType.Builder.<net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity>of(net.ganyusbathwater.oririmod.entity.custom.BlackHoleEntity::new, MobCategory.MISC)
                                                        .sized(1.0F, 1.0F).clientTrackingRange(64).updateInterval(1)
                                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "black_hole").toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity>> DEVIARTRAS = ENTITIES
                        .register("deviartras",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity::new,
                                                                        MobCategory.MONSTER)
                                                        .sized(0.8F, 2.5F)
                                                        .clientTrackingRange(64)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "deviartras")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.MermaidEntity>> MERMAID = ENTITIES
                        .register("mermaid",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.MermaidEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.MermaidEntity::new,
                                                                        net.minecraft.world.entity.MobCategory.WATER_CREATURE)
                                                        .sized(0.6F, 1.95F)
                                                        .clientTrackingRange(32)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "mermaid")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.LoadedBlazeEntity>> LOADED_BLAZE = ENTITIES
                        .register("loaded_blaze",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.LoadedBlazeEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.LoadedBlazeEntity::new,
                                                                        net.minecraft.world.entity.MobCategory.MONSTER)
                                                        .sized(0.6F, 1.8F)
                                                        .clientTrackingRange(8)
                                                        .fireImmune()
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "loaded_blaze")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.AetherChargeEntity>> AETHER_CHARGE_ENTITY = ENTITIES
                        .register("aether_charge_entity",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.AetherChargeEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.AetherChargeEntity::new,
                                                                        net.minecraft.world.entity.MobCategory.MISC)
                                                        .sized(0.3125F, 0.3125F)
                                                        .clientTrackingRange(4)
                                                        .updateInterval(10)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "aether_charge_entity")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.projectile.RexAraneaWebEntity>> REX_ARANEA_WEB = ENTITIES
                        .register("rex_aranea_web",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.projectile.RexAraneaWebEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.projectile.RexAraneaWebEntity::new,
                                                                        net.minecraft.world.entity.MobCategory.MISC)
                                                        .sized(0.25F, 0.25F)
                                                        .clientTrackingRange(64)
                                                        .updateInterval(1)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "rex_aranea_web")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.RexAraneaEntity>> REX_ARANEA = ENTITIES
                        .register("rex_aranea",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.RexAraneaEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.RexAraneaEntity::new,
                                                                        net.minecraft.world.entity.MobCategory.MONSTER)
                                                        .sized(1.4F, 2.25F) // adjusted for wall distance and human body height
                                                        .clientTrackingRange(64)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "rex_aranea")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.ModBoatEntity>> MOD_BOAT = ENTITIES
                        .register("mod_boat",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.ModBoatEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.ModBoatEntity::new,
                                                                        net.minecraft.world.entity.MobCategory.MISC)
                                                        .sized(1.375F, 0.5625F)
                                                        .clientTrackingRange(10)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "mod_boat")
                                                                        .toString()));

        public static final DeferredHolder<EntityType<?>, EntityType<net.ganyusbathwater.oririmod.entity.custom.ModChestBoatEntity>> MOD_CHEST_BOAT = ENTITIES
                        .register("mod_chest_boat",
                                        () -> EntityType.Builder
                                                        .<net.ganyusbathwater.oririmod.entity.custom.ModChestBoatEntity>of(
                                                                        net.ganyusbathwater.oririmod.entity.custom.ModChestBoatEntity::new,
                                                                        net.minecraft.world.entity.MobCategory.MISC)
                                                        .sized(1.375F, 0.5625F)
                                                        .clientTrackingRange(10)
                                                        .build(ResourceLocation
                                                                        .fromNamespaceAndPath(OririMod.MOD_ID,
                                                                                        "mod_chest_boat")
                                                                        .toString()));

        public static void register(IEventBus modBus) {

                ENTITIES.register(modBus);
        }
}
