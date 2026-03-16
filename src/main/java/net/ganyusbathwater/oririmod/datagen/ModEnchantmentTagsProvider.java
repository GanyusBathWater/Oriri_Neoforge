package net.ganyusbathwater.oririmod.datagen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.enchantment.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentTagsProvider extends EnchantmentTagsProvider {
    public ModEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, OririMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(net.ganyusbathwater.oririmod.util.ModTags.Enchantments.ELEMENTAL)
                .add(ModEnchantments.ELEMENT_FIRE)
                .add(ModEnchantments.ELEMENT_WATER)
                .add(ModEnchantments.ELEMENT_NATURE)
                .add(ModEnchantments.ELEMENT_EARTH)
                .add(ModEnchantments.ELEMENT_LIGHT)
                .add(ModEnchantments.ELEMENT_DARKNESS);
    }
}
