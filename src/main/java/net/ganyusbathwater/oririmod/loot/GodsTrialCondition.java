package net.ganyusbathwater.oririmod.loot;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.world.GodsTrialData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class GodsTrialCondition implements LootItemCondition {
    public static final GodsTrialCondition INSTANCE = new GodsTrialCondition();
    public static final MapCodec<GodsTrialCondition> CODEC = MapCodec.unit(INSTANCE);

    private GodsTrialCondition() {}

    @Override
    public LootItemConditionType getType() {
        return ModLootConditionTypes.GODS_TRIAL_ACTIVE.get();
    }

    @Override
    public boolean test(LootContext context) {
        if (context.getLevel() != null) {
            return GodsTrialData.get(context.getLevel()).isActive();
        }
        return false;
    }

    public static LootItemCondition.Builder godsTrialActive() {
        return () -> INSTANCE;
    }
}
