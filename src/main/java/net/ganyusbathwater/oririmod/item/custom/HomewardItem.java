package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Homeward — a charged-use item with two behaviors:
 *
 * <ul>
 *   <li><b>In a dungeon dimension</b>: after charging (hold right-click for 2 s),
 *       a simple confirmation screen is sent to the client. The client either
 *       confirms or cancels. On confirm the server teleports the player out.</li>
 *   <li><b>Outside a dungeon</b>: teleports the player to their bed / world spawn.</li>
 * </ul>
 *
 * Mana cost: 40. No durability. Stacks to 1.
 */
public class HomewardItem extends Item {

    /** Charge time in ticks (2 seconds). */
    public static final int CHARGE_TICKS = 40;
    public static final int MANA_COST = 40;

    public HomewardItem(Properties properties) {
        super(properties);
    }

    // -------------------------------------------------------------------------
    //  Use animation
    // -------------------------------------------------------------------------

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Shows a charging animation
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS;
    }

    // -------------------------------------------------------------------------
    //  Right-click → begin charging
    // -------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Check mana before allowing the charge (server-side only)
        if (!level.isClientSide() && !player.isCreative()) {
            if (ModManaUtil.getMana(player) < MANA_COST) {
                player.displayClientMessage(
                        Component.translatable("tooltip.oririmod.homeward.no_mana")
                                .withStyle(ChatFormatting.RED),
                        true);
                return InteractionResultHolder.fail(stack);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // -------------------------------------------------------------------------
    //  Charge ticking — play sound feedback
    // -------------------------------------------------------------------------

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        // Play a subtle sound every 10 ticks while charging
        if (!level.isClientSide() && remainingUseDuration % 10 == 0) {
            float pitch = 1.2f + (1f - (float) remainingUseDuration / CHARGE_TICKS) * 0.5f;
            level.playSound(null, entity.blockPosition(),
                    SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.3f, pitch);
        }
    }

    // -------------------------------------------------------------------------
    //  Charge complete → perform action
    // -------------------------------------------------------------------------

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer serverPlayer)) {
            return stack;
        }

        // Consume mana (creative bypasses this)
        if (!serverPlayer.isCreative()) {
            if (!ModManaUtil.tryConsumeMana(serverPlayer, MANA_COST)) {
                serverPlayer.displayClientMessage(
                        Component.translatable("tooltip.oririmod.homeward.no_mana")
                                .withStyle(ChatFormatting.RED),
                        true);
                return stack;
            }
        }

        // Check if the player is inside any dungeon dimension
        if (isDungeonDimension(serverPlayer)) {
            // Ask the client to show the "Leave the Dungeon?" confirmation screen.
            // The client replies with HomewardConfirmPayload (confirmed = true/false).
            NetworkHandler.sendHomewardConfirmRequest(serverPlayer);
        } else {
            // Outside a dungeon — teleport home immediately
            teleportHome(serverPlayer);
        }

        return stack;
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    /**
     * Checks whether the player is currently inside a dungeon dimension.
     * All dungeon dimensions follow the convention "oririmod:dungeon_<name>",
     * so we just check if the path starts with "dungeon_".
     */
    public static boolean isDungeonDimension(Player player) {
        String dimensionPath = player.level().dimension().location().getPath();
        return dimensionPath.startsWith("dungeon_");
    }

    /**
     * Teleports the player to their respawn point (bed / anchor) or the world spawn
     * if no respawn point is set.
     *
     * <p>Uses {@link ServerPlayer#findRespawnPositionAndUseSpawnBlock} which is the
     * authoritative 1.21.1 API — it returns a {@link DimensionTransition} that already
     * encodes the correct level, position and angle (including missing-block fallbacks).
     */
    public static void teleportHome(ServerPlayer serverPlayer) {
        // findRespawnPositionAndUseSpawnBlock is an instance method on ServerPlayer in 1.21.1.
        // Pass keepInventory=true so respawn anchors are NOT consumed on departure.
        DimensionTransition transition =
                serverPlayer.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);

        ServerLevel targetLevel = transition.newLevel();
        Vec3 pos = transition.pos();

        // Teleport — handles same-dimension and cross-dimension cases correctly.
        serverPlayer.teleportTo(
                targetLevel,
                pos.x, pos.y, pos.z,
                transition.yRot(), transition.xRot()
        );

        // Play a sound at the destination (we are now in the target level)
        serverPlayer.serverLevel().playSound(
                null, serverPlayer.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);

        serverPlayer.displayClientMessage(
                Component.translatable("tooltip.oririmod.homeward.teleported")
                        .withStyle(ChatFormatting.AQUA),
                true);
    }

    // -------------------------------------------------------------------------
    //  Tooltip
    // -------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.oririmod.homeward.lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.oririmod.homeward.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.oririmod.mana_cost", MANA_COST)
                .withStyle(ChatFormatting.DARK_BLUE));
    }
}
