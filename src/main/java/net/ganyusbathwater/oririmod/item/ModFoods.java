package net.ganyusbathwater.oririmod.item;


import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoods
{
    public static final FoodProperties ELDERBERRY = new FoodProperties.Builder().nutrition(1).alwaysEdible().effect(()-> new MobEffectInstance(MobEffects.GLOWING), 1).build();

}
