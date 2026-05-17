package net.ganyusbathwater.oririmod.item.component;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/**
 * Registers all custom DataComponentTypes for the Oriri mod.
 *
 * Registration must be called from the mod constructor by passing the
 * mod event bus (call ModDataComponents.register(modEventBus)).
 */
public final class ModDataComponents {

    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, OririMod.MOD_ID);

    /**
     * Presence of this component on an ItemStack opts it into the Calamity-style
     * animated tooltip pipeline. The value carries style/animation metadata.
     *
     * Key: "oririmod:cosmic_tooltip"
     * Persisted: yes (CODEC is defined, so the component survives save/load)
     * Networked: yes (STREAM_CODEC defined for client sync)
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CosmicTooltipData>> COSMIC_TOOLTIP =
            COMPONENTS.register("cosmic_tooltip", () ->
                    DataComponentType.<CosmicTooltipData>builder()
                            .persistent(CosmicTooltipData.CODEC)
                            .networkSynchronized(CosmicTooltipData.STREAM_CODEC)
                            .build()
            );

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
