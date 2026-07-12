package net.ganyusbathwater.oririmod.loot;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public class ModLootConditionTypes {
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES = 
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, OririMod.MOD_ID);

    public static final Supplier<LootItemConditionType> GODS_TRIAL_ACTIVE = 
            LOOT_CONDITION_TYPES.register("gods_trial_active", () -> new LootItemConditionType(GodsTrialCondition.CODEC));

    public static final Supplier<LootItemConditionType> CELESTIAL_EVENT_ACTIVE =
            LOOT_CONDITION_TYPES.register("celestial_event_active", () -> new LootItemConditionType(CelestialEventCondition.CODEC));

    public static void register(IEventBus eventBus) {
        LOOT_CONDITION_TYPES.register(eventBus);
    }
}
