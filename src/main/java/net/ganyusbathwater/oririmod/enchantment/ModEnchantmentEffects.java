package net.ganyusbathwater.oririmod.enchantment;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.enchantment.custom.SniperEnchantment;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEnchantmentEffects {
    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> ENTITY_ENCHANTMENT_EFFECTS =
            DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, OririMod.MOD_ID);

    public static final Supplier<MapCodec<? extends EnchantmentEntityEffect>> SNIPER =
            ENTITY_ENCHANTMENT_EFFECTS.register("sniper", () -> SniperEnchantment.CODEC);

    public static void register(IEventBus eventBus) {
        ENTITY_ENCHANTMENT_EFFECTS.register(eventBus);
    }
}
