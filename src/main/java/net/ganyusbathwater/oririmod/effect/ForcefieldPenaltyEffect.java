package net.ganyusbathwater.oririmod.effect;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ForcefieldPenaltyEffect extends MobEffect {
    public ForcefieldPenaltyEffect(Holder<Attribute> attribute, String id, double baseValue, AttributeModifier.Operation operation) {
        super(MobEffectCategory.HARMFUL, 0x800080);
        this.addAttributeModifier(
                attribute,
                ResourceLocation.fromNamespaceAndPath("oririmod", id),
                operation,
                (it.unimi.dsi.fastutil.ints.Int2DoubleFunction) amp -> baseValue + (amp * 0.05)
        );
    }
    
    public ForcefieldPenaltyEffect(Holder<Attribute> attribute, String id, it.unimi.dsi.fastutil.ints.Int2DoubleFunction valueSupplier, AttributeModifier.Operation operation) {
        super(MobEffectCategory.HARMFUL, 0x800080);
        this.addAttributeModifier(
                attribute,
                ResourceLocation.fromNamespaceAndPath("oririmod", id),
                operation,
                valueSupplier
        );
    }
}
