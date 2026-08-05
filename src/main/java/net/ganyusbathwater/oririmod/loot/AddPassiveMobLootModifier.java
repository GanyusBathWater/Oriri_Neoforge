package net.ganyusbathwater.oririmod.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class AddPassiveMobLootModifier extends LootModifier {
    public static final MapCodec<AddPassiveMobLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(m -> m.table)
            ).apply(inst, AddPassiveMobLootModifier::new)
    );

    private final ResourceKey<LootTable> table;

    public AddPassiveMobLootModifier(LootItemCondition[] conditionsIn, ResourceKey<LootTable> table) {
        super(conditionsIn);
        this.table = table;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        
        // Ensure that the drop is exclusively from a passive mob
        if (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal || entity instanceof Squid || entity instanceof AbstractVillager) {
            LootTable lootTable = context.getResolver().get(Registries.LOOT_TABLE, this.table).map(net.minecraft.core.Holder::value).orElse(null);
            if (lootTable != null) {
                lootTable.getRandomItemsRaw(context, generatedLoot::add);
            }
        }
        
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.ADD_PASSIVE_MOB_LOOT.get();
    }
}
