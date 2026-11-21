package net.ganyusbathwater.oririmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;
import static net.ganyusbathwater.oririmod.client.screen.OririInventoryGuiRenderer.*;

/**
 * Programmatic Oriri reliquary & chromatic-edge renderer for all 17 vanilla
 * Minecraft container screen families (Chests, Barrels, Ender Chests, Shulker Boxes,
 * Hoppers, Dispensers/Droppers, Crafting Tables, Furnaces/Blast Furnaces/Smokers,
 * Brewing Stands, Anvils, Smithing Tables, Grindstones, Horses/Mounts, Crafters,
 * Enchanting Tables, Stonecutters, Looms, Cartography Tables, Beacons, and Villagers).
 *
 * Strictly checks for vanilla container screen classes so modded machines/containers
 * are never overwritten.
 */
@OnlyIn(Dist.CLIENT)
public final class OririVanillaContainerRenderer {

    private OririVanillaContainerRenderer() {}

    /**
     * Returns true only for the vanilla container screens whose backgrounds we reskin.
     */
    public static boolean isSupportedVanillaContainer(AbstractContainerScreen<?> screen) {
        return screen instanceof ContainerScreen
                || screen instanceof ShulkerBoxScreen
                || screen instanceof DispenserScreen
                || screen instanceof HopperScreen
                || screen instanceof CraftingScreen
                || screen instanceof AbstractFurnaceScreen<?>
                || screen instanceof BrewingStandScreen
                || screen instanceof ItemCombinerScreen<?>
                || screen instanceof GrindstoneScreen
                || screen instanceof HorseInventoryScreen
                || screen instanceof CrafterScreen
                || screen instanceof EnchantmentScreen
                || screen instanceof StonecutterScreen
                || screen instanceof LoomScreen
                || screen instanceof CartographyTableScreen
                || screen instanceof BeaconScreen
                || screen instanceof MerchantScreen;
    }

    /**
     * Renders the Oriri dark-reliquary chassis, tailored workstation accents, and all
     * active container/inventory slots for any supported vanilla container screen.
     */
    public static void renderVanillaContainerBg(GuiGraphics gui, AbstractContainerScreen<?> screen,
                                                int x, int y, int w, int h) {
        drawReliquaryChassis(gui, x, y, w, h);

        // Draw tailored workstation chambers, conduits, and progress channels BEHIND slots & vanilla sprites
        if (screen instanceof CraftingScreen) {
            drawRecessedWell(gui, x + 27, y + 14, 58, 58, 0xFF3E2650);
            drawChromaticArrow(gui, x + 90, y + 43, 22);
        } else if (screen instanceof AbstractFurnaceScreen<?>) {
            // Flame hearth chamber (56, 36, 14x14)
            drawRecessedWell(gui, x + 55, y + 35, 16, 16, 0xFF4A2A24);
            drawDiamond(gui, x + 63, y + 43, 4, 0x44804028);
            // Smelting progress arrow channel (79, 34, 24x16)
            drawRecessedWell(gui, x + 78, y + 34, 26, 16, 0xFF3E2650);
            drawMutedArrow(gui, x + 81, y + 42, 20);
        } else if (screen instanceof BrewingStandScreen) {
            // Alchemical conduit pipes connecting ingredient (79,17) down to 3 bottles
            int pipe = 0xFF4A3163;
            gui.fill(x + 86, y + 34, x + 88, y + 57, pipe);
            gui.fill(x + 64, y + 46, x + 110, y + 48, pipe);
            gui.fill(x + 64, y + 46, x + 66, y + 51, pipe);
            gui.fill(x + 108, y + 46, x + 110, y + 51, pipe);
            // Blaze fuel bar channel (60, 44, 18x4)
            drawRecessedWell(gui, x + 59, y + 43, 20, 6, 0xFF4D331E);
            // Bubbles channel (63, 14, 12x29) & brew progress arrow channel (97, 16, 9x28)
            drawRecessedWell(gui, x + 62, y + 13, 14, 30, 0xFF35244A);
            drawRecessedWell(gui, x + 96, y + 15, 11, 30, 0xFF35244A);
        } else if (screen instanceof AnvilScreen) {
            // Plus sign between slot 0 (27,47) and slot 1 (76,47)
            drawPlus(gui, x + 56, y + 55, GOLD_DIM);
            drawChromaticArrow(gui, x + 99, y + 55, 22);
        } else if (screen instanceof SmithingScreen) {
            // Armor stand preview chamber (vanilla renders rotating stand around x+141, y+61)
            drawPortraitChamber(gui, x + 122, y + 10, x + 168, y + 76);
            drawChromaticArrow(gui, x + 68, y + 56, 20);
        } else if (screen instanceof GrindstoneScreen) {
            // Grindstone chamber connecting inputs (49,19 & 49,40) to output (129,34)
            drawRecessedWell(gui, x + 44, y + 14, 26, 48, 0xFF3E2650);
            drawChromaticArrow(gui, x + 92, y + 41, 22);
        } else if (screen instanceof CrafterScreen) {
            drawRecessedWell(gui, x + 23, y + 14, 60, 60, 0xFF3E2650);
            drawRecessedWell(gui, x + 98, y + 35, 18, 16, 0xFF3E2650);
        } else if (screen instanceof EnchantmentScreen) {
            // Book pedestal alcove & 3-row enchantment rune bay
            drawRecessedWell(gui, x + 12, y + 11, 42, 34, 0xFF3E2650);
            drawRecessedWell(gui, x + 58, y + 12, 112, 61, 0xFF3E2650);
        } else if (screen instanceof StonecutterScreen) {
            // 4×3 stonecutter pattern grid + scrollbar well
            drawRecessedWell(gui, x + 50, y + 12, 68, 58, 0xFF3E2650);
            drawRecessedWell(gui, x + 118, y + 13, 14, 56, 0xFF3E2650);
        } else if (screen instanceof LoomScreen) {
            // Pattern grid + scrollbar well + 3D banner preview chamber
            drawRecessedWell(gui, x + 58, y + 11, 60, 60, 0xFF3E2650);
            drawRecessedWell(gui, x + 118, y + 11, 14, 60, 0xFF3E2650);
            drawPortraitChamber(gui, x + 138, y + 7, x + 164, y + 49);
        } else if (screen instanceof CartographyTableScreen) {
            drawPlus(gui, x + 23, y + 41, GOLD_DIM);
            drawPortraitChamber(gui, x + 64, y + 11, x + 135, y + 79);
        } else if (screen instanceof BeaconScreen) {
            // Dual power sanctuaries (Primary left, Secondary right)
            drawRecessedWell(gui, x + 8, y + 18, 102, 88, 0xFF3E2650);
            drawRecessedWell(gui, x + 114, y + 18, 108, 88, 0xFF3E2650);
            drawGildedDivider(gui, x + 10, y + 132, w - 20, GOLD_DIM);
        } else if (screen instanceof MerchantScreen) {
            // Left scrollable trade ledger & right trade chamber
            drawRecessedWell(gui, x + 4, y + 17, 98, 142, 0xFF3E2650);
            drawRecessedWell(gui, x + 94, y + 18, 7, 140, 0xFF2D1C3D);
            drawRecessedWell(gui, x + 135, y + 15, 104, 7, 0xFF3E2650);
            drawChromaticArrow(gui, x + 186, y + 45, 24);
            drawGildedDivider(gui, x + 106, y + 67, w - 116, GOLD_DIM);
        } else if (screen instanceof ContainerScreen
                || screen instanceof ShulkerBoxScreen
                || screen instanceof DispenserScreen
                || screen instanceof HopperScreen) {
            drawContainerVaultAndDivider(gui, screen.getMenu().slots, x, y, w);
        }

        // Standard horizontal divider right above player inventory on standard 176-wide workstations
        if (w == 176 && !(screen instanceof ContainerScreen
                || screen instanceof ShulkerBoxScreen
                || screen instanceof DispenserScreen
                || screen instanceof HopperScreen
                || screen instanceof HorseInventoryScreen)) {
            int minPlayerY = findMinPlayerInventoryY(screen.getMenu().slots);
            if (minPlayerY >= 24) {
                drawGildedDivider(gui, x + 8, y + minPlayerY - 12, w - 16, GOLD_DIM);
            }
        }

        // Draw every active slot cleanly using actual menu slot coordinates
        drawAllMenuSlots(gui, screen, x, y);
    }

    /**
     * Dedicated Oriri renderer for {@link HorseInventoryScreen} (Horse, Donkey, Mule, Llama, Camel)
     * replacing vanilla's grey saddle/armor/chest sprite overlays seamlessly.
     */
    public static void renderHorseBg(GuiGraphics gui, HorseInventoryScreen screen,
                                     int x, int y, int w, int h,
                                     AbstractHorse horse, int inventoryColumns,
                                     float xMouse, float yMouse) {
        drawReliquaryChassis(gui, x, y, w, h);

        // Framed 3D mount preview chamber (26, 18) -> (78, 70)
        drawPortraitChamber(gui, x + 25, y + 17, x + 79, y + 71);

        // Optional recessed chest vault on the right (79, 17, cols*18 × 54)
        if (inventoryColumns > 0) {
            drawRecessedWell(gui, x + 78, y + 16, inventoryColumns * 18 + 2, 56, 0xFF3E2650);
        }

        // Divider above player inventory
        drawGildedDivider(gui, x + 8, y + 76, w - 16, GOLD_DIM);

        // Draw all active slots (saddle, armor, chest slots, player inventory, hotbar)
        drawAllMenuSlots(gui, screen, x, y);

        // Subtle gold/cyan accent markers on empty saddle & armor slots so players recognize them
        if (horse.isSaddleable()) {
            drawSlot(gui, x + 8, y + 18, GOLD_DIM, SLOT_FILL);
        }
        if (horse.canUseSlot(EquipmentSlot.BODY)) {
            drawSlot(gui, x + 8, y + 36, EQUIP_BORDER, SLOT_FILL);
        }

        // Live 3D mount preview (exact vanilla coords & scale)
        gui.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                gui, x + 26, y + 18, x + 78, y + 70, 17, 0.25F, xMouse, yMouse, horse);
    }

    public static void renderVanillaLabels(GuiGraphics gui, Font font,
                                           Component title, int titleX, int titleY,
                                           Component invTitle, int invX, int invY) {
        gui.drawString(font, title, titleX, titleY, TEXT_TITLE, true);
        gui.drawString(font, invTitle, invX, invY, TEXT_BODY, false);
    }

    public static void renderMerchantLabels(GuiGraphics gui, Font font, MerchantMenu menu,
                                            Component title, Component playerInventoryTitle,
                                            int imageWidth, int invX, int invY) {
        int level = menu.getTraderLevel();
        if (level > 0 && level <= 5 && menu.showProgressBar()) {
            Component fullTitle = Component.translatable("merchant.title", title,
                    Component.translatable("merchant.level." + level));
            int tw = font.width(fullTitle);
            int tx = 49 + imageWidth / 2 - tw / 2;
            gui.drawString(font, fullTitle, tx, 6, TEXT_TITLE, true);
        } else {
            int tw = font.width(title);
            gui.drawString(font, title, 49 + imageWidth / 2 - tw / 2, 6, TEXT_TITLE, true);
        }

        gui.drawString(font, playerInventoryTitle, invX, invY, TEXT_BODY, false);

        Component tradesLabel = Component.translatable("merchant.trades");
        int tradesW = font.width(tradesLabel);
        gui.drawString(font, tradesLabel, 5 - tradesW / 2 + 48, 6, GOLD_BRIGHT, true);
    }

    // ── Internal Layout Helpers ──

    private static void drawContainerVaultAndDivider(GuiGraphics gui, List<Slot> slots, int x, int y, int w) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minPlayerY = Integer.MAX_VALUE;

        for (Slot slot : slots) {
            if (slot == null || !slot.isActive() || slot.x < 0 || slot.y < 0) continue;
            if (slot.container instanceof Inventory) {
                if (slot.y < minPlayerY) minPlayerY = slot.y;
            } else {
                if (slot.x < minX) minX = slot.x;
                if (slot.x > maxX) maxX = slot.x;
                if (slot.y < minY) minY = slot.y;
                if (slot.y > maxY) maxY = slot.y;
            }
        }

        if (minX < Integer.MAX_VALUE) {
            drawRecessedWell(gui, x + minX - 2, y + minY - 2,
                    (maxX - minX) + 20, (maxY - minY) + 20, 0xFF3E2650);
        }
        if (minPlayerY < Integer.MAX_VALUE && minPlayerY >= 24) {
            drawGildedDivider(gui, x + 8, y + minPlayerY - 12, w - 16, GOLD_DIM);
        }
    }

    private static int findMinPlayerInventoryY(List<Slot> slots) {
        int minY = Integer.MAX_VALUE;
        for (Slot slot : slots) {
            if (slot != null && slot.isActive() && slot.container instanceof Inventory && slot.y >= 0) {
                if (slot.y < minY) minY = slot.y;
            }
        }
        return minY == Integer.MAX_VALUE ? -1 : minY;
    }

    private static void drawAllMenuSlots(GuiGraphics gui, AbstractContainerScreen<?> screen, int x, int y) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || !slot.isActive() || slot.x < 0 || slot.y < 0) continue;
            int sx = x + slot.x;
            int sy = y + slot.y;

            if (slot.container instanceof Inventory) {
                boolean isHotbar = slot.getSlotIndex() >= 0 && slot.getSlotIndex() < 9;
                drawSlot(gui, sx, sy, isHotbar ? HOTBAR_BORDER : SLOT_BORDER, SLOT_FILL);
            } else if (isOutputSlot(screen, slot)) {
                drawBorder(gui, sx - 2, sy - 2, 20, 20, withAlpha(GOLD, 0x77));
                drawSlot(gui, sx, sy, GOLD, 0xFF1A1128);
            } else {
                drawSlot(gui, sx, sy, EQUIP_BORDER, SLOT_FILL);
            }
        }
    }

    private static boolean isOutputSlot(AbstractContainerScreen<?> screen, Slot slot) {
        int idx = slot.index;
        if (screen instanceof CraftingScreen) return idx == 0;
        if (screen instanceof AbstractFurnaceScreen<?>) return idx == 2;
        if (screen instanceof AnvilScreen) return idx == 2;
        if (screen instanceof SmithingScreen) return idx == 3;
        if (screen instanceof GrindstoneScreen) return idx == 2;
        if (screen instanceof CrafterScreen) return idx == 9;
        if (screen instanceof StonecutterScreen) return idx == 1;
        if (screen instanceof LoomScreen) return idx == 3;
        if (screen instanceof CartographyTableScreen) return idx == 2;
        if (screen instanceof MerchantScreen) return idx == 2;
        return false;
    }

    private static void drawChromaticArrow(GuiGraphics gui, int x, int cy, int length) {
        int shaftEnd = x + Math.max(4, length - 4);
        int mid = (x + shaftEnd) / 2;
        fillHorizontalGradient(gui, x, cy - 1, mid, cy + 2, GOLD_BRIGHT, AMETHYST);
        fillHorizontalGradient(gui, mid, cy - 1, shaftEnd, cy + 2, AMETHYST, CYAN_LIGHT);
        gui.vLine(shaftEnd - 1, cy - 3, cy + 3, CYAN_LIGHT);
        gui.vLine(shaftEnd, cy - 2, cy + 2, CYAN_LIGHT);
        gui.vLine(shaftEnd + 1, cy - 1, cy + 1, GOLD_BRIGHT);
    }

    private static void drawMutedArrow(GuiGraphics gui, int x, int cy, int length) {
        int shaftEnd = x + Math.max(4, length - 4);
        int col = 0x554E3A66;
        gui.fill(x, cy - 1, shaftEnd, cy + 2, col);
        gui.vLine(shaftEnd - 1, cy - 3, cy + 3, col);
        gui.vLine(shaftEnd, cy - 2, cy + 2, col);
        gui.vLine(shaftEnd + 1, cy - 1, cy + 1, col);
    }

    private static void drawPlus(GuiGraphics gui, int cx, int cy, int color) {
        gui.fill(cx - 4, cy - 1, cx + 5, cy + 1, color);
        gui.fill(cx - 1, cy - 4, cx + 1, cy + 5, color);
    }
}
