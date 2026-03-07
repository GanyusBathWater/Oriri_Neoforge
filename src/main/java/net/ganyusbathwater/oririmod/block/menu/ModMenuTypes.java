package net.ganyusbathwater.oririmod.block.menu;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, OririMod.MOD_ID);

    public static final Supplier<MenuType<EquinoxTableMenu>> EQUINOX_TABLE_MENU = MENUS.register("equinox_table",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                var pos = data.readBlockPos();
                var level = inv.player.level();
                var be = level.getBlockEntity(pos);
                if (be instanceof net.ganyusbathwater.oririmod.block.entity.EquinoxTableBlockEntity tableBE) {
                    return new EquinoxTableMenu(windowId, inv, tableBE);
                }
                return new EquinoxTableMenu(windowId, inv);
            }));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
