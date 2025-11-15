// java
package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.KeepInventoryOnDeathEffect;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class KeepInventoryEvents {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone e) {
        Player original = e.getOriginal();
        Player clone = e.getEntity();

        if (!(original instanceof net.minecraft.server.level.ServerPlayer from) ||
                !(clone instanceof net.minecraft.server.level.ServerPlayer to)) {
            return;
        }

        boolean keepRule = from.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

        KeepInventoryOnDeathEffect cfgEffect = findKeepInventoryEffect(from);

        boolean shouldKeepAnything = keepRule || cfgEffect != null;
        if (!shouldKeepAnything) {
            return;
        }

        // Extra-Inventar kopieren, sobald mindestens eine Bedingung erfüllt ist
        if (from.getPersistentData().contains(ExtraInventoryMenu.NBT_KEY)) {
            to.getPersistentData().put(
                    ExtraInventoryMenu.NBT_KEY,
                    from.getPersistentData()
                            .getCompound(ExtraInventoryMenu.NBT_KEY)
                            .copy()
            );
        }

        // Normales Inventar nur bei ausgeschalteter Gamerule und aktivem Vestige anpassen
        if (!keepRule && cfgEffect != null) {
            cfgEffect.copyKeptItems(from, to);
        }
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        boolean keepRule = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        KeepInventoryOnDeathEffect cfgEffect = findKeepInventoryEffect(player);

        // Wenn weder Gamerule noch Vestige aktiv -> Extra-Inventar komplett droppen
        if (!keepRule && cfgEffect == null) {
            CompoundTag root = player.getPersistentData();
            if (!root.contains(ExtraInventoryMenu.NBT_KEY)) return;

            CompoundTag invTag = root.getCompound(ExtraInventoryMenu.NBT_KEY);
            HolderLookup.Provider lookup = player.level().registryAccess();
            NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(invTag, list, lookup);

            for (ItemStack stack : list) {
                if (stack.isEmpty()) continue;
                ItemEntity drop = new ItemEntity(
                        player.level(),
                        player.getX(),
                        player.getY() + 0.5,
                        player.getZ(),
                        stack.copy()
                );
                e.getDrops().add(drop);
            }

            // NBT löschen, damit das Extra-Inventar wirklich weg ist
            root.remove(ExtraInventoryMenu.NBT_KEY);
        }
    }

    private static KeepInventoryOnDeathEffect findKeepInventoryEffect(Player player) {
        // TODO:
        // - ExtraInventoryMenu.NBT_KEY laden
        // - Items daraus prüfen
        // - falls ein Vestige mit KeepInventoryOnDeathEffect gefunden wird, dessen Effekt zurückgeben
        return null;
    }
}