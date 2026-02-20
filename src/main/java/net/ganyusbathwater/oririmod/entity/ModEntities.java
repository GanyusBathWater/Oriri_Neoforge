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

        public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR = ENTITIES.register("meteor",
                        () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(64)
                                        .updateInterval(10)
                                        .build(ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "meteor")
                                                        .toString()));

        public static void register(IEventBus modBus) {
                ENTITIES.register(modBus);
        }
}