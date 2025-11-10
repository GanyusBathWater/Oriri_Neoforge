package net.ganyusbathwater.oririmod.menu;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, OririMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ExtraInventoryMenu>> EXTRA_INVENTORY =
            MENUS.register("extra_inventory",
                    () -> IMenuTypeExtension.create((windowId, inv, data) ->
                            new ExtraInventoryMenu(windowId, inv)));

    private ModMenus() {}
}