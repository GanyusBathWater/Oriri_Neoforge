package net.ganyusbathwater.oririmod.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class AddLootTableModifier extends LootModifier {
    public static final MapCodec<AddLootTableModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(m -> m.table)
            ).apply(inst, AddLootTableModifier::new)
    );

    private final ResourceKey<LootTable> table;

    public AddLootTableModifier(LootItemCondition[] conditionsIn, ResourceKey<LootTable> table) {
        super(conditionsIn);
        this.table = table;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        LootTable lootTable = context.getResolver().get(Registries.LOOT_TABLE, this.table).map(net.minecraft.core.Holder::value).orElse(null);
        if (lootTable != null) {
            lootTable.getRandomItemsRaw(context, generatedLoot::add);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.ADD_LOOT_TABLE.get();
    }
}
