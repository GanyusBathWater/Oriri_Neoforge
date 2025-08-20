package net.ganyusbathwater.oririmod.enchantment.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record SniperEnchantment() implements EnchantmentEntityEffect {
    public static final MapCodec<SniperEnchantment> CODEC = MapCodec.unit(SniperEnchantment::new);


    @Override
    //called when the enchantment is used (in this case when the bow that shoots it is used)
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {

    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }

}
