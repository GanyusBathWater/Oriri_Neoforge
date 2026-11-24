package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.custom.ArcanaItem;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends AdvancementProvider {

    private static final TagKey<Item> ARCANA_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "arcana"));

    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                  ExistingFileHelper existingFileHelper) {
        super(output, registries, existingFileHelper, List.of(new ArcanaAdvancementGenerator()));
    }

    private static final class ArcanaAdvancementGenerator implements AdvancementGenerator {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
                             ExistingFileHelper existingFileHelper) {
            AdvancementHolder root = Advancement.Builder.advancement()
                    .display(
                            ModItems.THE_FOOL.get(),
                            Component.translatable("advancement.oririmod.arcana.root.title"),
                            Component.translatable("advancement.oririmod.arcana.root.desc"),
                            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/block/abyssal_basalt_bricks.png"),
                            AdvancementType.TASK,
                            false,
                            false,
                            false
                    )
                    .addCriterion("has_arcana", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(ARCANA_TAG)))
                    .save(saver, ArcanaItem.ROOT_ADVANCEMENT_ID, existingFileHelper);

            for (ArcanaItem.ArcanaType arcana : ArcanaItem.ArcanaType.values()) {
                Advancement.Builder.advancement()
                        .parent(root)
                        .display(
                                arcana.getItem(),
                                Component.translatable(arcana.getTitleKey()),
                                Component.translatable(arcana.getDescriptionKey()),
                                null,
                                AdvancementType.TASK,
                                true,
                                true,
                                false
                        )
                        .addCriterion(ArcanaItem.CRITERION_CONSUMED,
                                CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                        .save(saver, arcana.getAdvancementId(), existingFileHelper);
            }

            Advancement.Builder.advancement()
                    .parent(root)
                    .display(
                            ModItems.THE_WORLD.get(),
                            Component.translatable("advancement.oririmod.arcana.all_arcana.title"),
                            Component.translatable("advancement.oririmod.arcana.all_arcana.desc"),
                            null,
                            AdvancementType.CHALLENGE,
                            true,
                            true,
                            false
                    )
                    .addCriterion(ArcanaItem.CRITERION_ALL_CONSUMED,
                            CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(saver, ArcanaItem.MASTERY_ADVANCEMENT_ID, existingFileHelper);
        }
    }
}
