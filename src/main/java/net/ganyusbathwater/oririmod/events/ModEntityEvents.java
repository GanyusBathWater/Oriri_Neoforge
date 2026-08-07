package net.ganyusbathwater.oririmod.events;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class ModEntityEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FIRE_ZOMBIE.get(), Zombie.createAttributes().build());
        event.put(ModEntities.SPORE_ZOMBIE.get(), Zombie.createAttributes().build());
        event.put(ModEntities.SPLINTER_SPIDER.get(), net.minecraft.world.entity.monster.Spider.createAttributes().build());
        event.put(ModEntities.MERMAID.get(), net.ganyusbathwater.oririmod.entity.custom.MermaidEntity.createAttributes().build());
        event.put(ModEntities.LOADED_BLAZE.get(), net.ganyusbathwater.oririmod.entity.custom.LoadedBlazeEntity.createAttributes().build());
        event.put(ModEntities.REX_ARANEA.get(), net.ganyusbathwater.oririmod.entity.custom.RexAraneaEntity.createAttributes().build());
        event.put(ModEntities.FAIRY.get(), net.ganyusbathwater.oririmod.entity.custom.FairyEntity.createAttributes().build());
        event.put(ModEntities.NOXUS_KNIGHT.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 25.0)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 2.5)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.2)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.5).build());
        event.put(ModEntities.NOXUS_GENERAL.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 30.0)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 5.0)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.175)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
        event.put(ModEntities.NOXUS_PALADIN.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 35.0)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 7.5)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.15)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1.5).build());
    }
}
