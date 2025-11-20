package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.custom.vestiges.Witherrose;
import net.ganyusbathwater.oririmod.menu.ExtraInventoryMenu;
import net.ganyusbathwater.oririmod.util.vestiges.VestigeManager;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class VestigeEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        VestigeManager.tick(sp);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if (VestigeManager.hasKeepInventory(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getOriginal() instanceof ServerPlayer oldSp)) return;
        if (!(event.getEntity() instanceof ServerPlayer newSp)) return;

        if (!VestigeManager.hasKeepInventory(oldSp)) return;

        var oldInv = oldSp.getInventory();
        var newInv = newSp.getInventory();

        for (int i = 0; i < oldInv.items.size(); i++) {
            ItemStack it = oldInv.items.get(i);
            newInv.items.set(i, it.isEmpty() ? ItemStack.EMPTY : it.copy());
        }
        for (int i = 0; i < oldInv.armor.size(); i++) {
            ItemStack it = oldInv.armor.get(i);
            newInv.armor.set(i, it.isEmpty() ? ItemStack.EMPTY : it.copy());
        }
        for (int i = 0; i < oldInv.offhand.size(); i++) {
            ItemStack it = oldInv.offhand.get(i);
            newInv.offhand.set(i, it.isEmpty() ? ItemStack.EMPTY : it.copy());
        }

        var oldRoot = oldSp.getPersistentData();
        var newRoot = newSp.getPersistentData();
        if (oldRoot.contains(ExtraInventoryMenu.NBT_KEY)) {
            newRoot.put(ExtraInventoryMenu.NBT_KEY, oldRoot.getCompound(ExtraInventoryMenu.NBT_KEY).copy());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        var srcEntity = event.getSource().getEntity();
        if (!(srcEntity instanceof WitherSkeleton || srcEntity instanceof WitherBoss)) return;

        // Nur aktiv, wenn Witherrose im Extra-Inventar liegt
        ItemStack witherrose = findWitherroseInExtra(sp);
        if (witherrose.isEmpty()) return;

        if (getUnlockedLevel(witherrose) < 2 || !isLevelEnabled(witherrose, 2)) return;

        float amount = event.getAmount();
        event.setAmount(amount * 0.5F); // 50 % weniger eingehender Schaden
    }

    @SubscribeEvent
    public static void onOutgoingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        LivingEntity target = event.getEntity();
        if (!(target instanceof WitherSkeleton || target instanceof WitherBoss)) return;

        // Nur aktiv, wenn Witherrose im Extra-Inventar liegt
        ItemStack witherrose = findWitherroseInExtra(sp);
        if (witherrose.isEmpty()) return;

        if (getUnlockedLevel(witherrose) < 3 || !isLevelEnabled(witherrose, 3)) return;

        float amount = event.getOriginalDamage();
        event.setNewDamage(amount * 1.5F); // 50 % mehr ausgehender Schaden
    }

    // --- Nur Extra-Inventar scannen ---
    private static ItemStack findWitherroseInExtra(ServerPlayer player) {
        var root = player.getPersistentData();
        if (!root.contains(ExtraInventoryMenu.NBT_KEY)) {
            return ItemStack.EMPTY;
        }

        var lookup = player.level().registryAccess();
        NonNullList<ItemStack> list = NonNullList.withSize(ExtraInventoryMenu.SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(root.getCompound(ExtraInventoryMenu.NBT_KEY), list, lookup);

        for (ItemStack stack : list) {
            if (!stack.isEmpty() && stack.getItem() instanceof Witherrose) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int getUnlockedLevel(ItemStack stack) {
        if (!(stack.getItem() instanceof Witherrose item)) return 1;
        return item.getUnlockedLevel(stack);
    }

    private static boolean isLevelEnabled(ItemStack stack, int level) {
        if (!(stack.getItem() instanceof Witherrose item)) return false;
        return item.isLevelEnabled(stack, level);
    }
}