package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.events.disaster.DisasterSafetyUtil;
import net.ganyusbathwater.oririmod.events.disaster.EarthquakeDisaster;
import net.ganyusbathwater.oririmod.events.disaster.LightningStormDisaster;
import net.ganyusbathwater.oririmod.events.disaster.MeteorShowerDisaster;
import net.ganyusbathwater.oririmod.events.disaster.SnowstormShelterEvaluator;
import net.ganyusbathwater.oririmod.events.disaster.TornadoDisaster;
import net.ganyusbathwater.oririmod.events.disaster.NaturalDisasterManager;
import net.ganyusbathwater.oririmod.events.disaster.NaturalDisasterType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * [DEV] Starts / stops natural disasters manually.
 * <ul>
 *   <li>Right-Click: start the selected disaster (or stop the running one)</li>
 *   <li>Shift + Right-Click (air): cycle the selected disaster</li>
 *   <li>Shift + Right-Click (block): toggle "Ignore Safety Check"</li>
 * </ul>
 * Every start prints the fresh-area scan of the user's position so the safety check can be verified.
 */
public class NaturalDisasterDebugItem extends Item {

    private static final String NBT_SELECTED = "SelectedDisaster";
    private static final String NBT_FORCE = "IgnoreSafety";

    public NaturalDisasterDebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS; // normal right-click on a block -> handled by use()
        }
        ItemStack stack = context.getItemInHand();
        if (!context.getLevel().isClientSide) {
            if (!checkPermission(player)) {
                return InteractionResult.FAIL;
            }
            boolean force = !isForced(stack);
            setForced(stack, force);
            player.displayClientMessage(Component.translatable(force
                            ? "message.oririmod.disaster_debug.force_on"
                            : "message.oririmod.disaster_debug.force_off")
                    .withStyle(force ? ChatFormatting.RED : ChatFormatting.GREEN), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!checkPermission(player) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown()) {
            NaturalDisasterType next = next(getSelected(stack));
            setSelected(stack, next);
            player.displayClientMessage(Component.translatable("message.oririmod.disaster_debug.selected", next.displayName())
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.success(stack);
        }

        ServerLevel currentLevel = serverPlayer.serverLevel();
        if (!DisasterSafetyUtil.isDisasterDimension(currentLevel.dimension())) {
            player.sendSystemMessage(Component.translatable("message.oririmod.disaster_debug.overworld_only").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel overworld = serverPlayer.server.overworld();
        NaturalDisasterManager manager = NaturalDisasterManager.get(overworld);
        if (manager.isActive()) {
            NaturalDisasterType running = manager.getActiveType();
            manager.stop(currentLevel);
            player.sendSystemMessage(Component.translatable("message.oririmod.disaster_debug.stopped", running.displayName())
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResultHolder.success(stack);
        }

        NaturalDisasterType selected = getSelected(stack);
        if (!selected.isImplemented()) {
            player.sendSystemMessage(Component.translatable("message.oririmod.disaster_debug.not_implemented", selected.displayName())
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        boolean force = isForced(stack);
        if (selected == NaturalDisasterType.METEOR_SHOWER || selected == NaturalDisasterType.EARTHQUAKE || selected == NaturalDisasterType.LIGHTNING_STORM || selected == NaturalDisasterType.TORNADO) {
            DisasterSafetyUtil.AreaScan scan = switch (selected) {
                case METEOR_SHOWER -> MeteorShowerDisaster.scanAround(currentLevel, serverPlayer);
                case EARTHQUAKE -> EarthquakeDisaster.scanAround(currentLevel, serverPlayer);
                case LIGHTNING_STORM -> LightningStormDisaster.scanAround(currentLevel, serverPlayer);
                case TORNADO -> TornadoDisaster.scanAround(currentLevel, serverPlayer);
                default -> null;
            };
            if (scan != null) {
                player.sendSystemMessage(Component.translatable("message.oririmod.disaster_debug.scan",
                                scan.maxInhabitedTicks(), scan.threshold(), scan.scannedChunks(), scan.skippedChunks())
                        .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.translatable(scan.isFresh()
                                ? "message.oririmod.disaster_debug.scan_fresh"
                                : (force ? "message.oririmod.disaster_debug.scan_inhabited_forced"
                                         : "message.oririmod.disaster_debug.scan_inhabited"))
                        .withStyle(scan.isFresh() ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            }
        }

        if (selected == NaturalDisasterType.SNOWSTORM) {
            boolean inCold = SnowstormShelterEvaluator.isColdBiome(currentLevel, serverPlayer.blockPosition());
            boolean sheltered = SnowstormShelterEvaluator.isSheltered(currentLevel, serverPlayer.blockPosition());
            player.sendSystemMessage(Component.literal("§7[Snowstorm Diagnostic] In Cold Biome: " + (inCold ? "§aYes" : "§cNo") + "§7 | Sheltered: " + (sheltered ? "§aYes" : "§cNo")));
        }

        int duration = selected.settings().duration.get();
        if (manager.start(currentLevel, selected, duration, force)) {
            player.sendSystemMessage(Component.translatable("message.oririmod.disaster_debug.started",
                    selected.displayName(), duration / 20).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("message.oririmod.disaster_debug.start_failed", selected.displayName())
                    .withStyle(ChatFormatting.RED));
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.oririmod.natural_disaster_debug_item.selected", getSelected(stack).displayName())
                .withStyle(ChatFormatting.GRAY));
        boolean force = isForced(stack);
        tooltip.add(Component.translatable(force
                        ? "tooltip.oririmod.natural_disaster_debug_item.force_on"
                        : "tooltip.oririmod.natural_disaster_debug_item.force_off")
                .withStyle(force ? ChatFormatting.RED : ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.oririmod.natural_disaster_debug_item.use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.oririmod.natural_disaster_debug_item.cycle").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.oririmod.natural_disaster_debug_item.toggle_force").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean checkPermission(Player player) {
        if (player.hasPermissions(2)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.oririmod.disaster_debug.no_permission")
                .withStyle(ChatFormatting.RED), true);
        return false;
    }

    private static NaturalDisasterType next(NaturalDisasterType current) {
        NaturalDisasterType[] values = NaturalDisasterType.values();
        int i = current.ordinal();
        do {
            i = (i + 1) % values.length;
        } while (values[i] == NaturalDisasterType.NONE);
        return values[i];
    }

    private static CompoundTag readTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    private static NaturalDisasterType getSelected(ItemStack stack) {
        NaturalDisasterType type = NaturalDisasterType.byName(readTag(stack).getString(NBT_SELECTED));
        return type == NaturalDisasterType.NONE ? NaturalDisasterType.METEOR_SHOWER : type;
    }

    private static void setSelected(ItemStack stack, NaturalDisasterType type) {
        CompoundTag tag = readTag(stack);
        tag.putString(NBT_SELECTED, type.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean isForced(ItemStack stack) {
        return readTag(stack).getBoolean(NBT_FORCE);
    }

    private static void setForced(ItemStack stack, boolean force) {
        CompoundTag tag = readTag(stack);
        tag.putBoolean(NBT_FORCE, force);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}

