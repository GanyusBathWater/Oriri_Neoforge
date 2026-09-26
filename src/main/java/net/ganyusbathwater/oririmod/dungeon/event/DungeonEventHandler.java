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
import net.neoforged.neoforge.event.level.ExplosionEvent;

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
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!isInDungeon(player)) return;
        if (player.isCreative()) return;

        net.minecraft.world.level.block.state.BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.is(net.minecraft.tags.BlockTags.REPLACEABLE) || state.is(net.minecraft.world.level.block.Blocks.COBWEB) || state.is(net.minecraft.tags.BlockTags.FIRE)) {
            return; // Allow breaking
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!isInDungeon(player)) return;
        if (player.isCreative()) return;

        net.minecraft.world.level.block.state.BlockState state = event.getState();
        if (state.is(net.minecraft.tags.BlockTags.REPLACEABLE) || state.is(net.minecraft.world.level.block.Blocks.COBWEB) || state.is(net.minecraft.tags.BlockTags.FIRE)) {
            return; // Allow breaking
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isInDungeon(sp)) return;
        if (sp.isCreative()) return;

        event.setCanceled(true);
        sp.displayClientMessage(
                Component.translatable("message.oririmod.dungeon.no_place")
                        .withStyle(ChatFormatting.RED),
                true);
    }

    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension().location().getPath().startsWith("dungeon_")) {
            // Prevent explosions from destroying blocks in the dungeon
            event.getAffectedBlocks().clear();
        }
    }

    // -------------------------------------------------------------------------
    //  Spawn Point Prevention
    // -------------------------------------------------------------------------
    
    @SubscribeEvent
    public static void onPlayerSetSpawn(net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (isInDungeon(sp)) {
                event.setCanceled(true);
                sp.displayClientMessage(
                        Component.translatable("message.oririmod.dungeon.no_sleep")
                                .withStyle(ChatFormatting.RED),
                        true);
            }
        }
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
        if (stack.is(Items.FLINT_AND_STEEL)) return true;
        if (stack.is(Items.FIRE_CHARGE)) return true;

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

        DungeonManager manager = DungeonManager.get(sp.serverLevel());
        var instance = manager.getInstanceForPlayer(sp.getUUID());
        if (instance != null) {
            int lives = instance.getPlayerLives(sp.getUUID());
            if (lives > 1) {
                // Lose a life, respawn in dungeon
                lives--;
                instance.setPlayerLives(sp.getUUID(), lives);
                manager.setDirty();
                
                sp.setHealth(sp.getMaxHealth());
                sp.removeAllEffects();
                
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload(lives));
                sp.displayClientMessage(Component.translatable("message.oririmod.dungeon.life_lost").withStyle(ChatFormatting.GOLD), false);
                
                // Teleport to stage start
                net.minecraft.core.BlockPos respawnPos = instance.getPlayerSpawnPos();
                if (instance.getActiveStage() != null && instance.getActiveStage().getDefinition().getPlayerSpawnPos() != null) {
                    respawnPos = instance.getActiveStage().getDefinition().getPlayerSpawnPos();
                }
                if (respawnPos != null) {
                    sp.teleportTo(respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5);
                }

                // Apply Stasis Effects to freeze the player while the Death Screen is open
                sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 255, false, false, false));
                sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP, net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 250, false, false, false));
                sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 255, false, false, false));
                sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, 0, false, false, false));

                // Send the payload to open the Custom Death Screen
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new net.ganyusbathwater.oririmod.network.packet.OpenDungeonDeathScreenPayload(lives));
            } else {
                // No lives left
                lives = 0;
                instance.setPlayerLives(sp.getUUID(), lives);
                
                if (instance.getAlivePlayers().size() <= 1) { // Current player is the last alive player (or solo)
                    // Everyone fails
                    sp.setHealth(sp.getMaxHealth());
                    sp.removeAllEffects();
                    sp.displayClientMessage(Component.translatable("message.oririmod.dungeon.died").withStyle(ChatFormatting.RED), false);
                    net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(sp);
                    instance.removePlayer(sp.getUUID());
                    
                    // Eject all spectators too
                    for (java.util.UUID specId : new java.util.HashSet<>(instance.getSpectators())) {
                        ServerPlayer spec = (ServerPlayer) sp.serverLevel().getPlayerByUUID(specId);
                        if (spec != null) {
                            spec.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                            spec.displayClientMessage(Component.translatable("message.oririmod.dungeon.party_failed").withStyle(ChatFormatting.RED), false);
                            net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(spec);
                            instance.removePlayer(specId);
                        }
                    }
                    
                    if (instance.getPlayers().isEmpty()) {
                        manager.removeInstance(sp.getServer(), instance.getInstanceId(), instance.getDungeonId()); 
                    }
                    manager.setDirty();
                } else {
                    // Multiplayer, and there are other alive players. Go to spectator.
                    sp.setHealth(sp.getMaxHealth());
                    sp.removeAllEffects();
                    sp.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                    instance.addSpectator(sp.getUUID());
                    manager.setDirty();
                    
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload(0));
                    sp.displayClientMessage(Component.translatable("message.oririmod.dungeon.spectating").withStyle(ChatFormatting.GOLD), false);
                    
                    // Force spectate first alive player
                    for (java.util.UUID aliveId : instance.getAlivePlayers()) {
                        ServerPlayer alive = (ServerPlayer) sp.serverLevel().getPlayerByUUID(aliveId);
                        if (alive != null) {
                            sp.setCamera(alive);
                            break;
                        }
                    }
                }
            }
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
        
        boolean inDungeon = isInDungeon(player);
        boolean shouldFly = player.isCreative() || player.isSpectator();

        if (inDungeon) {
            // Strip flight from non-creative/spectator players
            if (!shouldFly && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            } 
            // Ensure creative/spectator players CAN fly inside
            else if (shouldFly && !player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            
            // Phase 8: Spectator Lock (No free-roaming)
            if (player instanceof ServerPlayer sp && player.isSpectator()) {
                DungeonManager manager = DungeonManager.get(sp.serverLevel());
                var instance = manager.getInstanceForPlayer(sp.getUUID());
                if (instance != null && instance.isSpectator(sp.getUUID())) {
                    net.minecraft.world.entity.Entity camera = sp.getCamera();
                    boolean validCamera = (camera instanceof ServerPlayer cp) 
                            && instance.hasPlayer(cp.getUUID()) 
                            && !instance.isSpectator(cp.getUUID());
                            
                    if (!validCamera) {
                        // Find a new alive player to spectate
                        java.util.List<java.util.UUID> alivePlayers = instance.getAlivePlayers();
                        if (!alivePlayers.isEmpty()) {
                            ServerPlayer target = (ServerPlayer) sp.serverLevel().getPlayerByUUID(alivePlayers.get(0));
                            if (target != null) {
                                sp.setCamera(target);
                            }
                        }
                    }
                }
            }
        } else {
            // Outside dungeon: ensure creative/spectator players have flight
            if (shouldFly && !player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
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
            if (sp.isSpectator()) {
                sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            }
            DungeonManager manager = DungeonManager.get(sp.serverLevel());
            var instance = manager.getInstanceForPlayer(sp.getUUID());
            if (instance != null) {
                instance.removePlayer(sp.getUUID());
                if (instance.getPlayers().isEmpty()) {
                    manager.removeInstance(sp.getServer(), instance.getInstanceId(), instance.getDungeonId());
                }
                manager.setDirty();
            }

            // Wipe dungeon items
            if (!event.getTo().location().getPath().startsWith("dungeon_")) {
                wipeDungeonItems(sp);
            }
        } else if (event.getTo().location().getPath().startsWith("dungeon_")) {
            // Player entered a dungeon dimension, sync their lives!
            DungeonManager manager = DungeonManager.get(sp.serverLevel());
            var instance = manager.getInstanceForPlayer(sp.getUUID());
            if (instance != null) {
                int lives = instance.getPlayerLives(sp.getUUID());
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload(lives));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        if (isInDungeon(sp)) {
            // Option 1 Hardcore: if you log out in a dungeon, you are kicked and sent home.
            sp.displayClientMessage(Component.translatable("message.oririmod.dungeon.disconnect_kick").withStyle(ChatFormatting.RED), false);
            if (sp.isSpectator()) {
                sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            }
            net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(sp);
        } else {
            // Logged in outside a dungeon (e.g. game crashed or vanilla forced them to overworld). Wipe items to prevent smuggling.
            wipeDungeonItems(sp);
        }
    }

    private static void wipeDungeonItems(ServerPlayer sp) {
        boolean wipedAny = false;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack stack = sp.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(net.ganyusbathwater.oririmod.util.ModTags.Items.DUNGEON_ITEMS)) {
                sp.getInventory().setItem(i, ItemStack.EMPTY);
                wipedAny = true;
            }
        }
        if (wipedAny) {
            sp.displayClientMessage(Component.translatable("message.oririmod.dungeon.items_wiped").withStyle(ChatFormatting.YELLOW), false);
        }
    }

    public static void handleDeathAction(ServerPlayer sp, String action) {
        if (!isInDungeon(sp)) return;
        
        // Remove the stasis effects
        sp.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.JUMP);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        
        DungeonManager manager = DungeonManager.get(sp.serverLevel());
        var instance = manager.getInstanceForPlayer(sp.getUUID());
        if (instance == null) return;
        
        if ("GIVE_UP".equals(action)) {
            // Treat as failure for this player
            sp.setHealth(sp.getMaxHealth());
            sp.displayClientMessage(Component.translatable("message.oririmod.dungeon.died").withStyle(ChatFormatting.RED), false);
            
            if (instance.getAlivePlayers().isEmpty()) {
                // Everyone fails
                net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(sp);
                instance.removePlayer(sp.getUUID());
                
                // Eject all spectators too
                for (java.util.UUID specId : new java.util.HashSet<>(instance.getSpectators())) {
                    ServerPlayer spec = (ServerPlayer) sp.serverLevel().getPlayerByUUID(specId);
                    if (spec != null) {
                        spec.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                        spec.displayClientMessage(Component.translatable("message.oririmod.dungeon.party_failed").withStyle(ChatFormatting.RED), false);
                        net.ganyusbathwater.oririmod.item.custom.HomewardItem.teleportHome(spec);
                        instance.removePlayer(specId);
                    }
                }
                
                if (instance.getPlayers().isEmpty()) {
                    manager.removeInstance(sp.getServer(), instance.getInstanceId(), instance.getDungeonId()); 
                }
                manager.setDirty();
            } else {
                // Switch to spectator mode since party is still alive
                sp.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                instance.addSpectator(sp.getUUID());
                manager.setDirty();
                
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new net.ganyusbathwater.oririmod.network.packet.SyncDungeonLivesPayload(0));
                sp.displayClientMessage(Component.translatable("message.oririmod.dungeon.spectating").withStyle(ChatFormatting.GOLD), false);
                
                // Force spectate first alive player
                for (java.util.UUID aliveId : instance.getAlivePlayers()) {
                    ServerPlayer alive = (ServerPlayer) sp.serverLevel().getPlayerByUUID(aliveId);
                    if (alive != null) {
                        sp.setCamera(alive);
                        break;
                    }
                }
            }
        } else if ("RESPAWN".equals(action)) {
            // Already healed and teleported in onPlayerDeath, just removed effects. We can play a sound.
            sp.serverLevel().playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_BREATH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
