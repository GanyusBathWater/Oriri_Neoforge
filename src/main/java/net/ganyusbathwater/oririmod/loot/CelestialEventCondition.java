package net.ganyusbathwater.oririmod.loot;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class CelestialEventCondition implements LootItemCondition {
    public static final CelestialEventCondition INSTANCE = new CelestialEventCondition();
    public static final MapCodec<CelestialEventCondition> CODEC = MapCodec.unit(INSTANCE);

    private CelestialEventCondition() {}

    @Override
    public LootItemConditionType getType() {
        return ModLootConditionTypes.CELESTIAL_EVENT_ACTIVE.get();
    }

    @Override
    public boolean test(LootContext context) {
        if (context.getLevel() != null) {
            return WorldEventManager.isEventActive(context.getLevel(), WorldEventType.GREEN_MOON) ||
                   WorldEventManager.isEventActive(context.getLevel(), WorldEventType.BLOOD_MOON) ||
                   WorldEventManager.isEventActive(context.getLevel(), WorldEventType.ECLIPSE);
        }
        return false;
    }

    public static LootItemCondition.Builder celestialEventActive() {
        return () -> INSTANCE;
    }
}
