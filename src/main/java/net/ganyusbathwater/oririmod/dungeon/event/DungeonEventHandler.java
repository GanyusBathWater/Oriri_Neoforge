package net.ganyusbathwater.oririmod.dungeon.event;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.dungeon.DungeonManager;
import net.ganyusbathwater.oririmod.dungeon.dimension.DungeonDimensionManager;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Phase 3 — Dungeon Rules Enforcement.
 *
 * <ul>
 *   <li>Block all block-break and block-place actions inside dungeon dimensions.</li>
 *   <li>Ban specific items from being used inside dungeons (Elytra, Ender Pearl, Spring vestige,
 *       Trident, Leaping / Slow Falling potions).</li>
 *   <li>On player death: cancel the drop event for inventory, restore health, and eject the
 *       player back to their return position.</li>
 *   <li>On player disconnect: save progress and remove the player from the instance.</li>
 * </ul>
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public class DungeonEventHandler {

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    /** Returns true when the player is currently inside a dungeon dimension. */
    private static boolean isInDungeon(Player player) {
        return player.level().dimension().location().getPath().startsWith("dungeon_");
    }

    // -------------------------------------------------------------------------
    //  Block break / place prevention
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (!isInDungeon(sp)) return;
        if (sp.hasPermissions(4)) return; // Allow server operators for building/debugging

        event.setCanceled(true);
        sp.displayClientMessage(
                Component.translatable("message.oririmod.dungeon.no_break")
                        .withStyle(ChatFormatting.RED),
                true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isInDungeon(sp)) return;
        if (sp.hasPermissions(4)) return;

        event.setCanceled(true);
        sp.displayClientMessage(
                Component.translatable("message.oririmod.dungeon.no_place")
                        .withStyle(ChatFormatting.RED),
                true);
    }

    // -------------------------------------------------------------------------
    //  Banned item use
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer sp)) return;
        if (!isInDungeon(sp)) return;

        ItemStack stack = event.getItemStack();

        if (isBannedItem(stack)) {
            event.setCanceled(true);
            sp.displayClientMessage(
                    Component.translatable("message.oririmod.dungeon.item_banned")
                            .withStyle(ChatFormatting.RED),
                    true);
        }
    }

    /**
     * Returns true for every item that is banned from use inside dungeons:
     * <ul>
     *   <li>Elytra</li>
     *   <li>Ender Pearl</li>
     *   <li>Spring vestige</li>
     *   <li>Trident</li>
     *   <li>Leaping potions (all variants)</li>
     *   <li>Slow Falling potions (all variants)</li>
     * </ul>
     */
    private static boolean isBannedItem(ItemStack stack) {
        // Vanilla bans
        if (stack.is(Items.ELYTRA)) return true;
        if (stack.is(Items.ENDER_PEARL)) return true;
        if (stack.is(Items.TRIDENT)) return true;

        // Custom vestige bans
        if (stack.is(ModItems.SPRING.get())) return true;

        // Potion bans — ban any potion that grants Leaping or Slow Falling
        if (stack.getItem() instanceof PotionItem) {
            PotionContents contents = stack.getOrDefault(
                    net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                    PotionContents.EMPTY);
            for (net.minecraft.world.effect.MobEffectInstance effect : contents.getAllEffects()) {
                if (effect.getEffect().is(MobEffects.JUMP) || effect.getEffect().is(MobEffects.SLOW_FALLING)) {
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    //  Death handling — keep inventory, eject from dungeon
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isInDungeon(sp)) return;

        // Cancel the normal death so vanilla doesn't move the player to respawn or drop items.
        // We will handle everything manually.
        event.setCanceled(true);

        // Restore to 1 hp so the death state clears (we can't cancel after full death without side effects)
        sp.setHealth(1.0f);

        // Remove all harmful effects that caused/accompanied the death
        sp.removeAllEffects();

        // Eject from dungeon — teleport to home immediately
        net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(sp);

        // Inform the player
        sp.displayClientMessage(
                Component.translatable("message.oririmod.dungeon.died")
                        .withStyle(ChatFormatting.GOLD),
                false);

        // Remove player from the active instance (progress is saved, they can re-enter later)
        DungeonManager manager = DungeonManager.get(sp.serverLevel());
        var instance = manager.getInstanceForPlayer(sp.getUUID());
        if (instance != null) {
            instance.removePlayer(sp.getUUID());
            // If the dungeon is now empty, clean it up
            if (instance.getPlayers().isEmpty()) {
                manager.removeInstance(sp.getServer(), instance.getInstanceId(),
                        instance.getDungeonId()); 
            }
            manager.setDirty();
        }
    }

    // -------------------------------------------------------------------------
    //  Disconnect handling — save progress, remove from instance
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isInDungeon(sp)) return;

        // Fetch the server level BEFORE the player is fully removed from it
        ServerLevel level = sp.serverLevel();
        DungeonManager manager = DungeonManager.get(level);

        var instance = manager.getInstanceForPlayer(sp.getUUID());
        if (instance != null) {
            instance.removePlayer(sp.getUUID());
            if (instance.getPlayers().isEmpty()) {
                manager.removeInstance(sp.getServer(), instance.getInstanceId(), instance.getDungeonId());
            }
            manager.setDirty();
        }

        // Note: The player's position is saved by vanilla to their home dimension
        // when they disconnect. They will NOT respawn inside the dungeon dimension
        // when they log back in, but their dungeon progress is preserved via SavedData.
    }

    // -------------------------------------------------------------------------
    //  Disable creative flight inside dungeons
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!isInDungeon(player)) return;

        // Strip creative flight ability in dungeon — keeps the game mode, just removes the flight
        if (player.getAbilities().mayfly) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    // -------------------------------------------------------------------------
    //  Switch detection (Phase 6)
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isInDungeon(sp)) return;

        DungeonManager manager = DungeonManager.get(sp.serverLevel());
        var instance = manager.getInstanceForPlayer(sp.getUUID());
        if (instance != null && instance.getActiveStage() instanceof net.ganyusbathwater.oririmod.dungeon.stage.ActivateSwitchesStage switchStage) {
            var def = switchStage.getDefinition();
            for (var switchEntry : def.getSwitches()) {
                if (switchEntry.pos().equals(event.getPos())) {
                    switchStage.notifySwitchActivated(switchEntry.switchId());
                    sp.serverLevel().playSound(null, event.getPos(), net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Homeward cleanup (Phase 6) - removes players leaving via HomewardItem
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        // If the player left a dungeon dimension
        if (event.getFrom().location().getPath().startsWith("dungeon_")) {
            DungeonManager manager = DungeonManager.get(sp.serverLevel());
            var instance = manager.getInstanceForPlayer(sp.getUUID());
            if (instance != null) {
                instance.removePlayer(sp.getUUID());
                if (instance.getPlayers().isEmpty()) {
                    manager.removeInstance(sp.getServer(), instance.getInstanceId(), instance.getDungeonId());
                }
                manager.setDirty();
            }
        }
    }
}
