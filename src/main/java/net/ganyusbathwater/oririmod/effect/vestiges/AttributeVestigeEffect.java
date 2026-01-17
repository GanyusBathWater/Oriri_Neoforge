package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeVestigeEffect implements VestigeEffect {

    private final Holder<Attribute> attribute;
    private final double amount;
    private final ResourceLocation location;

    public AttributeVestigeEffect(Holder<Attribute> attribute, double amount) {
        this.attribute = attribute;
        this.amount = amount;
        // \*Stabile ID\* pro Attribut, damit das Entfernen zuverlässig ist.
        String attrPath = attribute.unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("unknown");
        this.location = ResourceLocation.fromNamespaceAndPath(
                OririMod.MOD_ID,
                "vestige_attribute_modifier_" + attrPath
        );
    }

    @Override
    public void tick(VestigeContext ctx) {
        // Hält den Wert korrekt, falls sich das Level während des Tragens ändert.
        applyOrUpdate(ctx);
    }

    @Override
    public void onEquip(VestigeContext ctx) {
        applyOrUpdate(ctx);
    }

    @Override
    public void onUnequip(VestigeContext ctx) {
        if (ctx == null || ctx.player() == null) return;

        AttributeInstance inst = ctx.player().getAttribute(attribute);
        if (inst == null) return;

        inst.removeModifier(location);
    }

    private void applyOrUpdate(VestigeContext ctx) {
        if (ctx == null || ctx.player() == null) return;

        AttributeInstance inst = ctx.player().getAttribute(attribute);
        if (inst == null) return;

        // Stacking verhindern: erst entfernen, dann einmal mit korrektem Wert hinzufügen.
        inst.removeModifier(location);
        inst.addTransientModifier(new AttributeModifier(location, amount, AttributeModifier.Operation.ADD_VALUE));
    }
}