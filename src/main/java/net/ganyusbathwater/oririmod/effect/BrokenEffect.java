package net.ganyusbathwater.oririmod.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class BrokenEffect extends MobEffect {

    // Eindeutige ID für den Attributmodifier (beliebiger, aber stabiler Pfad)
    public static final ResourceLocation BROKEN_ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("oririmod", "broken_armor");

    public BrokenEffect(MobEffectCategory category, int color) {
        super(category, color); // Reduziert die *gesamte* Rüstung (inkl. Rüstungsteile) um:
                // Stufe 1: -25%, Stufe 2: -50%, Stufe 3+: -75%
        this.addAttributeModifier(
                Attributes.ARMOR,                           // Holder<Attribute>
                BROKEN_ARMOR_MODIFIER_ID,                   // ResourceLocation
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amp -> amp >= 2 ? -0.75D : (amp >= 1 ? -0.50D : -0.25D)
        );
    }

    @Override
    public void addAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, amplifier);
    }

    @Override
    public void removeAttributeModifiers(net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);
    }

}
