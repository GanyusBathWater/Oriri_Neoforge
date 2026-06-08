package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.util.CircuitryParticleUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.List;

/**
 * ParticleDebugWandItem — a creative-mode debug tool for testing and previewing
 * Lodestone-based particle effects created in this project.
 *
 * <h2>Controls</h2>
 * <ul>
 *   <li><b>Shift + Right Click</b>: Opens {@link ParticleSelectionScreen} to pick an effect.</li>
 *   <li><b>Right Click</b>: Fires the currently selected particle effect at the player's
 *       eye position (client-side only — no server round-trip needed for pure visuals).</li>
 * </ul>
 *
 * <h2>Adding a new effect</h2>
 * <ol>
 *   <li>Add an entry to {@link ParticleEffectType}.</li>
 *   <li>Add a {@code prettyName} switch case.</li>
 *   <li>Add a dispatch case inside {@link #fireEffect}.</li>
 * </ol>
 */
public class ParticleDebugWandItem extends Item {

    // ─────────────────────────────────────────────────────────────────────────
    // Effect registry enum — add future effects here
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * All available particle effects that can be selected and fired by this wand.
     * Extend this enum whenever a new effect utility is created.
     */
    public enum ParticleEffectType {
        /** Cyber-magic PCB-trace circuitry lines (default cyan, or custom colour). */
        CIRCUITRY_CYAN,
        CIRCUITRY_MAGENTA,
        CIRCUITRY_GREEN,
        CIRCUITRY_GOLD,
        CIRCUITRY_RED,
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NBT / CustomData key
    // ─────────────────────────────────────────────────────────────────────────

    private static final String NBT_SELECTED = "ActiveParticle";

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public ParticleDebugWandItem(Properties properties) {
        super(properties);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item behaviour
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift + Right Click → open selection screen (client only)
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new net.ganyusbathwater.oririmod.client.screen.ParticleSelectionScreen(stack, hand));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Right Click → fire the current effect (client-side visual only)
        if (level.isClientSide) {
            ParticleEffectType current = getSelected(stack);
            // Spawn at eye position, slightly in front so the player can see it
            Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.8));
            fireEffect(level, origin, current);

            // Show action-bar feedback
            player.displayClientMessage(
                    Component.literal("Spawned: " + prettyName(current))
                             .withStyle(ChatFormatting.AQUA), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tooltip
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ParticleEffectType current = getSelected(stack);
        tooltipComponents.add(Component.literal("Selected Effect: " + prettyName(current))
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Right Click: Spawn effect")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal("Shift + Right Click: Switch effect")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Effect dispatch — CLIENT SIDE ONLY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Routes the selected {@link ParticleEffectType} to its corresponding utility method.
     *
     * <p><strong>Must only be called on the client side.</strong></p>
     *
     * @param level  The client level.
     * @param origin The world position to spawn the effect at.
     * @param type   The selected effect type.
     */
    private static void fireEffect(Level level, Vec3 origin, ParticleEffectType type) {
        switch (type) {
            // ── Circuitry variants ──────────────────────────────────────────
            case CIRCUITRY_CYAN    -> CircuitryParticleUtil.spawn(level, origin, null);
            case CIRCUITRY_MAGENTA -> CircuitryParticleUtil.spawn(level, origin, new Color(255, 0, 200));
            case CIRCUITRY_GREEN   -> CircuitryParticleUtil.spawn(level, origin, new Color(0, 255, 80));
            case CIRCUITRY_GOLD    -> CircuitryParticleUtil.spawn(level, origin, new Color(255, 200, 0));
            case CIRCUITRY_RED     -> CircuitryParticleUtil.spawn(level, origin, new Color(255, 30, 30));
            // ── Add future effects here ─────────────────────────────────────
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Selection persistence (CustomData component — matches BossAttackDebugWand)
    // ─────────────────────────────────────────────────────────────────────────

    /** Reads the currently-selected effect from the item stack's CustomData. */
    private static ParticleEffectType getSelected(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) {
            String name = data.copyTag().getString(NBT_SELECTED);
            try {
                return name.isEmpty() ? ParticleEffectType.CIRCUITRY_CYAN
                                      : ParticleEffectType.valueOf(name);
            } catch (IllegalArgumentException e) {
                return ParticleEffectType.CIRCUITRY_CYAN;
            }
        }
        return ParticleEffectType.CIRCUITRY_CYAN;
    }

    /**
     * Writes the selected effect into the item stack's CustomData.
     * Called by the network handler when the client sends a selection packet.
     */
    public static void setSelected(ItemStack stack, ParticleEffectType type) {
        net.minecraft.world.item.component.CustomData data =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        net.minecraft.nbt.CompoundTag tag =
                data != null ? data.copyTag() : new net.minecraft.nbt.CompoundTag();
        tag.putString(NBT_SELECTED, type.name());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Display name helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a human-readable display name for the given effect type. */
    public static String prettyName(ParticleEffectType type) {
        return switch (type) {
            case CIRCUITRY_CYAN    -> "Circuitry (Cyan)";
            case CIRCUITRY_MAGENTA -> "Circuitry (Magenta)";
            case CIRCUITRY_GREEN   -> "Circuitry (Green)";
            case CIRCUITRY_GOLD    -> "Circuitry (Gold)";
            case CIRCUITRY_RED     -> "Circuitry (Red)";
        };
    }
}
