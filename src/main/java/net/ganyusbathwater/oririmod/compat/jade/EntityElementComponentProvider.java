package net.ganyusbathwater.oririmod.compat.jade;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.combat.Element;
import net.ganyusbathwater.oririmod.combat.EntityElementRegistry;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum EntityElementComponentProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation FIRE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "fire_element_symbole");
    private static final ResourceLocation WATER = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "water_element_symbole");
    private static final ResourceLocation NATURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "nature_element_symbole");
    private static final ResourceLocation EARTH = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "earth_element_symbole");
    private static final ResourceLocation LIGHT = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "light_element_symbole");
    private static final ResourceLocation DARKNESS = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "darkness_element_symbole");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Element element = EntityElementRegistry.getElement(accessor.getEntity());
        
        ResourceLocation icon = null;
        switch (element) {
            case FIRE -> icon = FIRE;
            case WATER -> icon = WATER;
            case NATURE -> icon = NATURE;
            case EARTH -> icon = EARTH;
            case LIGHT -> icon = LIGHT;
            case DARKNESS -> icon = DARKNESS;
            default -> {} // Physical and True Damage do not have icons
        }

        if (icon != null) {
            // Append the image at the top or wherever the component defaults to.
            tooltip.add(snownee.jade.api.ui.IElementHelper.get().sprite(icon, 16, 16)); 
            
            net.minecraft.ChatFormatting color = net.minecraft.ChatFormatting.WHITE;
            switch (element) {
                case FIRE -> color = net.minecraft.ChatFormatting.RED;
                case WATER -> color = net.minecraft.ChatFormatting.BLUE;
                case NATURE -> color = net.minecraft.ChatFormatting.GREEN;
                case EARTH -> color = net.minecraft.ChatFormatting.GOLD;
                case LIGHT -> color = net.minecraft.ChatFormatting.YELLOW;
                case DARKNESS -> color = net.minecraft.ChatFormatting.DARK_PURPLE;
                default -> {}
            }

            // Capitalize the first letter, lowercase the rest
            String name = element.name().substring(0, 1).toUpperCase() + element.name().substring(1).toLowerCase();
            tooltip.append(snownee.jade.api.ui.IElementHelper.get().text(net.minecraft.network.chat.Component.literal(" " + name).withStyle(color)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "element_symbol");
    }
}
